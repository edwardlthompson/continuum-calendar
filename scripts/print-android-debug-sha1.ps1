# Print debug keystore SHA-1 for Google Cloud Android OAuth client.
# Usage (from repo root):
#   .\scripts\print-android-debug-sha1.ps1
#   pwsh -NoProfile -File .\scripts\print-android-debug-sha1.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$debugKeystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
if (-not (Test-Path $debugKeystore)) {
    Write-Host "ERROR: debug keystore not found at $debugKeystore"
    Write-Host "Build the app once so Android Studio / Gradle creates it, then re-run."
    exit 1
}

Write-Host "=== keytool (debug.keystore) ==="
keytool -list -v -keystore $debugKeystore -alias androiddebugkey -storepass android -keypass android |
    Select-String -Pattern "SHA1:|SHA256:|Owner:"

Write-Host ""
Write-Host "=== Gradle signingReport (optional; slower) ==="
Write-Host "Paste if you prefer Gradle's view:"
Write-Host "  cd apps\mobile; .\gradlew.bat :app:signingReport"
Write-Host ""
Write-Host "Use the SHA1 line (colons OK) in Google Cloud > Android OAuth client."
Write-Host "Package names to register:"
Write-Host "  org.continuumcalendar.app.debug"
Write-Host "  org.continuumcalendar.app   (release keystore SHA-1 when you ship)"
