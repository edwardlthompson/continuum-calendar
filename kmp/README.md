# Continuum KMP (Compose Multiplatform)

Parallel MIT track for shared domain + Compose UI. Shipping apps remain `apps/desktop` (Tauri)
and `apps/mobile` (Fossify) until parity.

## Build

```bash
cd kmp
./gradlew :continuum-domain:jvmTest
./gradlew :desktopApp:run
./gradlew :androidApp:assembleDebug
```

iOS targets: `-Pcontinuum.enableIos=true` on macOS only.
