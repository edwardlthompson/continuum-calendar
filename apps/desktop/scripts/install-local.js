#!/usr/bin/env node
/** Cross-platform install:local dispatcher (Windows PowerShell / Linux bash). */
import { spawnSync } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const isWin = process.platform === 'win32'
const script = isWin ? join(here, 'install-local.ps1') : join(here, 'install-local.sh')
const cmd = isWin
  ? {
      file: 'powershell',
      args: ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', script],
    }
  : { file: 'bash', args: [script] }

const result = spawnSync(cmd.file, cmd.args, { stdio: 'inherit', shell: false })
if (result.error) {
  console.error(result.error.message)
  process.exit(1)
}
process.exit(result.status ?? 1)
