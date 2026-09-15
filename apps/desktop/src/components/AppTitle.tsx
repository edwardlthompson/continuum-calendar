import { APP_TITLE } from './appTitle'

/** Header product name only. Installed version lives in Settings / About. */
export function AppTitle() {
  return <h1 className="text-2xl font-semibold tracking-tight">{APP_TITLE}</h1>
}
