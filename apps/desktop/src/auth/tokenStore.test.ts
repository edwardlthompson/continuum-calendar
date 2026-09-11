import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import {
  getDeviceId,
  loadTokens,
  parseStoredTokens,
  peekStoredTokenScope,
  saveTokens,
  scopeFromTokens,
} from './tokenStore.ts'

const store = new Map<string, string>()

const memoryStorage: Storage = {
  get length() {
    return store.size
  },
  clear() {
    store.clear()
  },
  getItem(key: string) {
    return store.has(key) ? store.get(key)! : null
  },
  key(index: number) {
    return [...store.keys()][index] ?? null
  },
  removeItem(key: string) {
    store.delete(key)
  },
  setItem(key: string, value: string) {
    store.set(key, value)
  },
}

Object.defineProperty(globalThis, 'localStorage', { value: memoryStorage, configurable: true })

afterEach(async () => {
  store.clear()
  await saveTokens(null)
})

test('parseStoredTokens rejects empty and corrupt blobs', () => {
  assert.equal(parseStoredTokens(null), null)
  assert.equal(parseStoredTokens(''), null)
  assert.equal(parseStoredTokens('{'), null)
  assert.equal(parseStoredTokens('{"scope":"x"}'), null)
})

test('saveTokens and loadTokens round-trip in localStorage', async () => {
  const tokens = {
    accessToken: 'a',
    refreshToken: 'r',
    expiresAt: Date.now() + 3_600_000,
    tokenType: 'Bearer',
    scope: 'https://www.googleapis.com/auth/calendar',
  }
  await saveTokens(tokens)
  assert.deepEqual(await loadTokens(), tokens)
  assert.equal(peekStoredTokenScope(), tokens.scope)
  assert.equal(scopeFromTokens(tokens), tokens.scope)
  await saveTokens(null)
  assert.equal(await loadTokens(), null)
  assert.equal(peekStoredTokenScope(), '')
})

test('getDeviceId is stable after the first mint', () => {
  const first = getDeviceId()
  assert.match(first, /^[0-9a-f-]{36}$/i)
  assert.equal(getDeviceId(), first)
})
