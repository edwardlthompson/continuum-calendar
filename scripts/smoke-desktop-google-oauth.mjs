#!/usr/bin/env node
/**
 * Local smoke: validate Continuum desktop Google OAuth wiring (no interactive login).
 * Usage: node scripts/smoke-desktop-google-oauth.mjs
 */
import { createHash, randomBytes } from 'node:crypto'
import { readFileSync, existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import net from 'node:net'
import http from 'node:http'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const envPath = resolve(root, 'apps/desktop/.env')
const SCOPE = [
  'https://www.googleapis.com/auth/calendar',
  'https://www.googleapis.com/auth/contacts.readonly',
  'https://www.googleapis.com/auth/drive.appdata',
  'https://www.googleapis.com/auth/tasks',
].join(' ')

function fail(msg) {
  console.error(`FAIL: ${msg}`)
  process.exitCode = 1
}
function ok(msg) {
  console.log(`OK: ${msg}`)
}

function loadEnv(path) {
  if (!existsSync(path)) throw new Error(`Missing ${path}`)
  const out = {}
  for (const line of readFileSync(path, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^([A-Z0-9_]+)=(.*)$/)
    if (m) out[m[1]] = m[2].trim()
  }
  return out
}

function b64url(buf) {
  return Buffer.from(buf)
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

function pkce() {
  const verifier = b64url(randomBytes(32))
  const challenge = b64url(createHash('sha256').update(verifier).digest())
  return { verifier, challenge }
}

function listenLoopback() {
  return new Promise((resolveListen, reject) => {
    const server = http.createServer((req, res) => {
      res.writeHead(200, { 'Content-Type': 'text/plain' })
      res.end('Continuum OAuth smoke listener')
      const url = new URL(req.url || '/', 'http://127.0.0.1')
      server.close()
      resolveListen({ port: server.address().port, query: Object.fromEntries(url.searchParams) })
    })
    server.listen(0, '127.0.0.1', () => {
      const port = server.address().port
      resolveListen({ port, server, query: null })
    })
    server.on('error', reject)
  })
}

async function main() {
  console.log('=== Continuum desktop Google OAuth smoke ===')
  const env = loadEnv(envPath)
  const clientId = env.VITE_GOOGLE_CLIENT_ID || ''
  if (!clientId.includes('apps.googleusercontent.com')) {
    fail('VITE_GOOGLE_CLIENT_ID missing or not a Google client id')
    return
  }
  ok(`Client ID present (…${clientId.slice(-28)})`)

  const { challenge } = pkce()
  const probe = await new Promise((resolveListen, reject) => {
    const server = net.createServer()
    server.listen(0, '127.0.0.1', () => {
      const port = server.address().port
      server.close(() => resolveListen(port))
    })
    server.on('error', reject)
  })
  const redirectUri = `http://127.0.0.1:${probe}/`
  ok(`Loopback redirect probe port ${probe}`)

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: SCOPE,
    access_type: 'offline',
    prompt: 'consent',
    state: 'smoke',
    code_challenge: challenge,
    code_challenge_method: 'S256',
  })
  const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?${params}`
  ok(`Auth URL built (${authUrl.length} chars)`)

  const res = await fetch(authUrl, { redirect: 'manual' })
  const loc = res.headers.get('location') || ''
  console.log(`Google auth HTTP ${res.status}${loc ? ` → ${loc.slice(0, 80)}…` : ''}`)

  // Valid clients typically 302 to AccountChooser / signin / oauth
  if (res.status >= 300 && res.status < 400 && /google\.com/i.test(loc)) {
    ok('Google accepted client_id + redirect_uri shape (redirect into Google login)')
  } else if (res.status === 200) {
    const body = await res.text()
    if (/access_denied|invalid_request|Error 400|Error 401|Error 403/i.test(body)) {
      fail('Google returned an error page for this client/redirect/scopes')
      console.error(body.slice(0, 400))
    } else if (/Sign in|Account|identifier/i.test(body)) {
      ok('Google served a sign-in HTML page (client looks usable)')
    } else {
      fail('Unexpected 200 body from Google auth endpoint')
      console.error(body.slice(0, 400))
    }
  } else {
    fail(`Unexpected Google auth response status ${res.status}`)
    const body = await res.text().catch(() => '')
    if (body) console.error(body.slice(0, 400))
  }

  // Vite should be up for tauri:dev
  try {
    const vite = await fetch('http://localhost:5173/', { redirect: 'manual' })
    if (vite.ok || vite.status === 200) ok('Vite dev server responds on :5173')
    else fail(`Vite :5173 returned ${vite.status}`)
  } catch {
    fail('Vite not reachable on http://localhost:5173/ — start apps/desktop with npm run tauri:dev')
  }

  if (process.exitCode) {
    console.log('=== SMOKE FAILED ===')
  } else {
    console.log('=== SMOKE PASSED (interactive Sign in still required in the app UI) ===')
  }
}

main().catch((e) => {
  fail(e instanceof Error ? e.message : String(e))
  console.log('=== SMOKE FAILED ===')
})
