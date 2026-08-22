import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  hasDriveAppDataScope,
  humanizeOAuthFailure,
  isExpiredGoogleAuth,
  isTestingModeOAuthError,
} from './oauthErrors.ts'

test('access_denied points at Test users', () => {
  const msg = humanizeOAuthFailure(new Error('403:access_denied'))
  assert.match(msg, /Test user/i)
  assert.equal(isTestingModeOAuthError('access_denied'), true)
})

test('unknown error after unverified warning points at Calendar sign-in', () => {
  const msg = humanizeOAuthFailure('An unknown error has occurred')
  assert.match(msg, /Calendar/i)
  assert.doesNotMatch(msg, /0\.17\.2/)
})

test('something went wrong is treated as Testing-mode consent failure', () => {
  const msg = humanizeOAuthFailure('Sorry, something went wrong there. Try again.')
  assert.match(msg, /Testing mode|Calendar/i)
  assert.equal(isTestingModeOAuthError('Sorry, something went wrong there'), true)
})

test('network errors stay network-focused', () => {
  const msg = humanizeOAuthFailure(new Error('Failed to fetch'))
  assert.match(msg, /network/i)
})

test('invalid_grant asks to sign in again and keep local edits', () => {
  const msg = humanizeOAuthFailure(new Error('Token exchange failed (400): invalid_grant'))
  assert.match(msg, /expired/i)
  assert.match(msg, /saved on this PC/i)
  assert.equal(isExpiredGoogleAuth('invalid_grant — Token has been expired or revoked'), true)
  assert.equal(isExpiredGoogleAuth('Calendar list failed: 401'), false)
})

test('Drive 403 tells the user to use Google Calendar for the phone', () => {
  const msg = humanizeOAuthFailure(
    new Error('Drive local-events list failed: 403 Request had insufficient authentication scopes.'),
  )
  assert.match(msg, /Google Calendar/i)
  assert.match(msg, /phone/i)
})

test('hasDriveAppDataScope reads the Drive App Data URL', () => {
  assert.equal(hasDriveAppDataScope('https://www.googleapis.com/auth/calendar'), false)
  assert.equal(
    hasDriveAppDataScope(
      'https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/drive.appdata',
    ),
    true,
  )
})
