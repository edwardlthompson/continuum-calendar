import assert from 'node:assert/strict'
import test from 'node:test'
import { loadLastMainView, parseMainView, saveLastMainView, LAST_MAIN_VIEW_KEY } from './lastMainView'

test('parseMainView accepts known views only', () => {
  assert.equal(parseMainView('agenda'), 'agenda')
  assert.equal(parseMainView('rolling'), 'rolling')
  assert.equal(parseMainView('month'), 'month')
  assert.equal(parseMainView('year'), 'year')
  assert.equal(parseMainView('nope'), null)
  assert.equal(parseMainView(1), null)
})

test('save and load last main view', () => {
  const store = new Map<string, string>()
  const ls = {
    getItem: (k: string) => (store.has(k) ? store.get(k)! : null),
    setItem: (k: string, v: string) => {
      store.set(k, v)
    },
    removeItem: (k: string) => {
      store.delete(k)
    },
  }
  Object.defineProperty(globalThis, 'localStorage', { value: ls, configurable: true })

  saveLastMainView('month')
  assert.equal(store.get(LAST_MAIN_VIEW_KEY), 'month')
  assert.equal(loadLastMainView(), 'month')
  saveLastMainView('rolling')
  assert.equal(loadLastMainView('agenda'), 'rolling')
})
