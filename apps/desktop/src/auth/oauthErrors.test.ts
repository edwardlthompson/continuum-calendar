import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  hasDriveAppDataScope,
  humanizeOAuthFailure,
  isExpiredGoogleAuth,
  isTestingModeOAuthError,
  shouldSkipDrivePeerSync,
} from './oauthErrors.ts'
import { GOOGLE_DESKTOP_SIGNIN_SCOPE, GOOGLE_SCOPES } from '../../../../packages/shared/src/oauth.ts'

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

test('Drive 403 tells the user to Sign in again for Drive App Data', () => {
  const msg = humanizeOAuthFailure(
    new Error('Drive local-events list failed: 403 Request had insufficient authentication scopes.'),
  )
  assert.match(msg, /Drive App Data/i)
  assert.match(msg, /Sign in again/i)
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

test('shouldSkipDrivePeerSync when Calendar-only', () => {
  assert.equal(shouldSkipDrivePeerSync(GOOGLE_SCOPES.calendar), true)
  assert.equal(shouldSkipDrivePeerSync(GOOGLE_DESKTOP_SIGNIN_SCOPE), false)
  assert.equal(shouldSkipDrivePeerSync(undefined), true)
})

test('desktop sign-in scope is Calendar plus Drive App Data', () => {
  assert.match(GOOGLE_DESKTOP_SIGNIN_SCOPE, /calendar/)
  assert.ok(GOOGLE_DESKTOP_SIGNIN_SCOPE.includes(GOOGLE_SCOPES.driveAppData))
  assert.doesNotMatch(GOOGLE_DESKTOP_SIGNIN_SCOPE, /contacts/)
  assert.doesNotMatch(GOOGLE_DESKTOP_SIGNIN_SCOPE, /tasks/)
})
