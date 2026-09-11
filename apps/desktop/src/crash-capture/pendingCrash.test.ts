import assert from 'node:assert/strict'
import { beforeEach, test } from 'node:test'
import {
  onSaveCrashesChanged,
  persistPendingCrash,
  readPendingCrash,
  sanitizeCrashPayload,
} from './pendingCrash.ts'

function memoryStorage(): Storage {
  const map = new Map<string, string>()
  return {
    get length() {
      return map.size
    },
    clear() {
      map.clear()
    },
    getItem(key: string) {
      return map.has(key) ? map.get(key)! : null
    },
    key(index: number) {
      return [...map.keys()][index] ?? null
    },
    removeItem(key: string) {
      map.delete(key)
    },
    setItem(key: string, value: string) {
      map.set(key, value)
    },
  }
}

beforeEach(() => {
  const session = memoryStorage()
  const local = memoryStorage()
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: session })
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: local })
})

test('sanitizes before persist and queues one record', () => {
  persistPendingCrash({
    message: 'boom ghp_abcdefghijklmnopqrstuvwxyz012345',
    stack: 'at C:\\Users\\Ada\\x.ts',
  })
  const read = readPendingCrash()
  assert.ok(read)
  assert.equal(read.message.includes('ghp_'), false)
  assert.equal(read.stack.includes('Ada'), false)
})

test('drops email token prompt extras from the payload allowlist', () => {
  const got = sanitizeCrashPayload({
    message: 'e',
    stack: 's',
    email: 'keep-out',
    token: 'keep-out',
    prompt: 'keep-out',
  })
  assert.deepEqual(Object.keys(got).sort(), ['message', 'stack'])
  assert.equal(JSON.stringify(got).includes('keep-out'), false)
})

test('does not write localStorage unless saveLocal', () => {
  persistPendingCrash({ message: 'e', stack: 's' }, false)
  assert.equal(localStorage.getItem('gp.crash.pending.local'), null)
})

test('clears when save-crashes turns off', () => {
  persistPendingCrash({ message: 'e', stack: 's' }, true)
  onSaveCrashesChanged(false)
  assert.equal(readPendingCrash(), null)
})
