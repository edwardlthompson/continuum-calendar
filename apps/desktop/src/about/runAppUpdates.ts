import { GITHUB_RELEASES_PAGE } from './donate.ts'
import { fetchLatestGithubRelease } from './githubRelease.ts'
import {
  selectProductAsset,
  shouldCheckDaily,
  shouldNudgeDonate,
  shouldPromptUpdate,
} from './productUpdate.ts'
import { loadUpdatePrefs, markUpdateChecked, markVersionSeen } from './updatePrefs.ts'

export type LaunchPrompt =
  | { kind: 'donate' }
  | { kind: 'update'; version: string; url: string }

export async function decideLaunchPrompt(
  currentVersion: string,
  now = Date.now(),
  fetchLatest = fetchLatestGithubRelease,
): Promise<LaunchPrompt | null> {
  const prefs = loadUpdatePrefs()
  if (shouldNudgeDonate(prefs.lastSeenVersion, currentVersion)) {
    return { kind: 'donate' }
  }
  markVersionSeen(currentVersion)
  if (!shouldCheckDaily(prefs.lastCheckAt, now)) return null
  const release = await fetchLatest(currentVersion)
  markUpdateChecked(now)
  if (!release) return null
  const asset = selectProductAsset(release.assets, 'exe')
  const latest = asset?.version ?? null
  if (!shouldPromptUpdate(currentVersion, latest, prefs.dismissedVersion) || !latest) {
    return null
  }
  return { kind: 'update', version: latest, url: asset?.url || release.htmlUrl || GITHUB_RELEASES_PAGE }
}
