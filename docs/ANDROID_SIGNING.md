# Android signing and rollback

> Upload keys stay **outside git**. This template never ships a keystore.

## Continuum Calendar release key

Maintainer keystore lives **outside git** at `$HOME/keys/continuum-release.jks` (mode `600`). Local Gradle reads gitignored `apps/mobile/keystore.properties`. The same material is stored as **GitHub Environment** secrets on `release-signing` (`ANDROID_KEYSTORE_BASE64`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD`).

| Variable | Role |
|----------|------|
| `SIGNING_STORE_FILE` | Absolute path to the Continuum release keystore |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias (`continuum`) |
| `SIGNING_KEY_PASSWORD` | Key password |

`.github/workflows/ci.yml` `android-release` must **not** receive these secrets (unsigned hash-compare only). Optional signed FOSS APKs: `.github/workflows/android-signed-foss.yml` (`workflow_dispatch`, environment `release-signing`). Windows NSIS EXE: `.github/workflows/desktop-nsis.yml` (`workflow_dispatch`, informational).

**New key (2026-09-11, this Linux host):** package `org.continuumcalendar.app`, alias `continuum`.

- SHA-1: `4A:93:68:05:E1:8F:43:28:E0:31:33:B2:CD:8B:E4:AF:71:70:E2:82`
- SHA-256: `16:F3:2E:23:C6:53:43:79:49:50:67:80:3F:A4:20:78:32:9E:BF:83:B6:BE:2D:45:F4:1D:35:43:12:11:40:93`

Add that SHA-1 on the Google **Android** OAuth client. It **cannot** update sideloaded APKs signed with the lost v0.16.2 key (`72:40:1C:C3:82:8B:26:80:6A:D2:C1:F3:B9:97:52:76:13:C0:7F:4F`). Uninstall `org.continuumcalendar.app` first, or keep using the `.debug` package.

Local signed build:

```bash
cd apps/mobile && ./gradlew :app:assembleFossRelease
```

## Upload keystore

Create one upload keystore on a machine you control (`keytool -genkeypair`). Store the `.jks` / `.keystore` / `.p12` file outside the repo (password manager or encrypted disk). **Never commit** those files — `.gitignore` already lists `*.jks`, `*.keystore`, and `*.p12`.

Play App Signing (optional commercial store) keeps the *app signing* key on Google's side. You keep only the **upload** key. F-Droid rebuilds from source and signs with the F-Droid key; your upload key is not required there.

## Environment variables

Copy `.env.example` → `.env` (gitignored). For a local signed release:

| Variable | Role |
|----------|------|
| `GOLDENPATH_UPLOAD_STORE_FILE` | Absolute path to the upload keystore |
| `GOLDENPATH_UPLOAD_STORE_PASSWORD` | Keystore password |
| `GOLDENPATH_UPLOAD_KEY_ALIAS` | Key alias (default `upload` in Gradle) |
| `GOLDENPATH_UPLOAD_KEY_PASSWORD` | Key password |
`examples/android/app/build.gradle.kts` applies the `upload` signing config only when `GOLDENPATH_UPLOAD_STORE_FILE` is set. Empty or missing → release stays debug-signed (CI hash compare).

## Local signed release

```bash
export SOURCE_DATE_EPOCH=1700000000
export GOLDENPATH_UPLOAD_STORE_FILE="$HOME/keys/goldenpath-upload.jks"
# plus the three password/alias variables
cd examples/android && ./gradlew assembleRelease

```

Confirm `app/build/outputs/apk/release/` is signed (`apksigner verify`). Keep that version's `mapping.txt` with the GitHub Release.

## Continuous integration

`android-release` in `.github/workflows/ci.yml` must **not** receive store passwords. It assembles twice with `SOURCE_DATE_EPOCH=1700000000` and compares APK hashes. Production signing happens on a human-held keystore or a protected GitHub Environment — never on the default `GITHUB_TOKEN` job.

## Per-ABI release APK hash compare

When shipping ABI splits (`armeabi-v7a`, `arm64-v8a`, `x86_64`), compare hashes **per output APK**, not only the universal artifact:

```bash
export SOURCE_DATE_EPOCH=1700000000
cd examples/android
./gradlew assembleRelease
sha256sum app/build/outputs/apk/release/*.apk | tee /tmp/apk-hashes-1.txt
./gradlew clean assembleRelease
sha256sum app/build/outputs/apk/release/*.apk | tee /tmp/apk-hashes-2.txt
diff -u /tmp/apk-hashes-1.txt /tmp/apk-hashes-2.txt

```

`scripts/verify-reproducible-apk.sh` checks the primary release APK twice. Extend child CI to loop ABI outputs the same way when `splits.abi.isEnable = true`. One-ABI mismatches usually mean NDK/unstripped `.so` timestamps — pin NDK and keep `SOURCE_DATE_EPOCH`.

## CI artifact retention (reproducible APK)

| Artifact | Retention policy |
|----------|------------------|
| Unsigned release APK from `android-release` | **Do not upload** by default. The job proves reproducibility via hash compare only — retaining unsigned APKs adds little value and grows Actions storage. |
| Optional debug upload (human / fork) | If you add `actions/upload-artifact` for triage, set `retention-days: 7` (max 14 for this template). Never longer than needed to bisect a flake. |
| Signed APK / AAB + `mapping.txt` | Attach to the **GitHub Release** for that tag (not CI artifacts). Releases are the long-term store. |
| SBOM / OpenVEX | Release assets; wait with `wait-release-sbom -- --require`. |
Rationale: reproducibility is a **gate**, not a binary warehouse. Ship binaries through Releases; keep CI lean.

## F-Droid and Play

| Channel | Who signs | Rollback |
|---------|-----------|----------|
| GitHub Releases | Your upload (or debug for CI proofs) | Re-attach the previous tag's APK / AAB |
| F-Droid | F-Droid builders | Previous suggested version in the recipe |
| Play (optional) | Play App Signing after upload | Play Console → prior release; keep the same upload key |
## Rollback

1. Identify the last known-good tag (GitHub Release).
2. Re-publish that APK/AAB (do not rebuild if the key or epoch changed).
3. If users already installed a bad build, ship a **higher** `versionCode` that reverts the code, signed with the **same** upload key.
4. Retrace crashes with that version's `mapping.txt` (`app/build/outputs/mapping/release/mapping.txt`).
5. Log user-impacting incidents in `DECISION_LOG.md`.

Losing the upload key **cannot** be fixed by generating a new one for the same Play listing. Rotate only via `docs/RUNBOOK.md` Secret Rotation after a leak; then enroll a new upload key in Play Console.

## Mapping files

Archive `mapping.txt` next to each signed artifact. R8 obfuscation makes unmapped Play/F-Droid stacks useless. Do not commit mappings that embed secrets; they are build outputs (`*.apk` is already gitignored).

## Unsigned CI vs local signed

| Build | Where | Signing | Purpose |
|-------|-------|---------|---------|
| CI release assemble | GitHub Actions | Unsigned (or ephemeral) | Reproducible artifact + SBOM pairing; no upload key in CI secrets by default |
| Local upload build | Maintainer machine | `GOLDENPATH_UPLOAD_*` keystore env | Play / sideload upload only |
FOSS path: keep CI **unsigned** for bit-for-bit comparison; sign locally when distributing. Do not commit keystores. See package attestation honesty in `docs/PACKAGE_ATTESTATION.md`.
