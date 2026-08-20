import assert from 'node:assert/strict'
import { test } from 'node:test'
import { humanizeOAuthFailure, isTestingModeOAuthError } from './oauthErrors.ts'

test('access_denied points at Test users', () => {
  const msg = humanizeOAuthFailure(new Error('403:access_denied'))
  assert.match(msg, /Test user/i)
  assert.equal(isTestingModeOAuthError('access_denied'), true)
})

test('unknown error after unverified warning mentions Test users', () => {
  const msg = humanizeOAuthFailure('An unknown error has occurred')
  assert.match(msg, /Test user/i)
  assert.doesNotMatch(msg, /0\.17\.2/)
})

test('network errors stay network-focused', () => {
  const msg = humanizeOAuthFailure(new Error('Failed to fetch'))
  assert.match(msg, /network/i)
})
