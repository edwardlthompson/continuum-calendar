/** @deprecated Use googleCalendar.ts + googleAuth.ts — kept for import compatibility. */
export {
  listGoogleEvents as listEvents,
  createGoogleEvent as createEvent,
  updateGoogleEvent as updateEvent,
  deleteGoogleEvent as deleteEvent,
  searchGoogleContacts as searchContacts,
} from './googleCalendar'
export { beginGoogleSignIn as buildAuthUrl } from '../auth/googleAuth'
export { mockSeedEvents as mockWeekEvents } from '../data/localStore'
export { GOOGLE_SCOPES, GOOGLE_SCOPE_STRING } from '@continuum/shared'
