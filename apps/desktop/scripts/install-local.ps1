# Install Continuum Calendar locally (release only).
# Usage (from apps/desktop):  npm run install:local
# Or:  powershell -File scripts/install-local.ps1

$ErrorActionPreference = "Stop"
$DesktopRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $DesktopRoot "package.json"))) {
  Write-Error "Run from apps/desktop (scripts/install-local.ps1). Found root: $DesktopRoot"
}

$cwdLeaf = Split-Path -Leaf (Get-Location).Path
if ($cwdLeaf -eq "debug" -and ((Get-Location).Path -match "[\\/]target[\\/]debug$")) {
  Write-Error "Refuse to run install-local from target\debug. Use apps/desktop."
}

$InstallDir = Join-Path $env:LOCALAPPDATA "Continuum Calendar"
$InstallExe = Join-Path $InstallDir "app.exe"
$ReleaseExe = Join-Path $DesktopRoot "src-tauri\target\release\app.exe"
$DistIndex = Join-Path $DesktopRoot "dist\index.html"

Write-Host "Stopping installed Continuum (if running)..."
Get-CimInstance Win32_Process -Filter "Name='app.exe'" -ErrorAction SilentlyContinue |
  Where-Object { $_.ExecutablePath -and ($_.ExecutablePath -ieq $InstallExe) } |
  ForEach-Object {
    Write-Host ("  stop PID " + $_.ProcessId)
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
  }
Start-Sleep -Seconds 1

Push-Location $DesktopRoot
try {
  Write-Host "Building release with embedded frontend (tauri build --no-bundle)..."
  npm run tauri:build -- --no-bundle
  if ($LASTEXITCODE -ne 0) {
    Write-Error ("tauri:build failed with exit " + $LASTEXITCODE)
  }
} finally {
  Pop-Location
}

if (-not (Test-Path -LiteralPath $DistIndex)) {
  Write-Error ("Missing " + $DistIndex + " after build - frontend was not embedded.")
}
if (-not (Test-Path -LiteralPath $ReleaseExe)) {
  Write-Error ("Missing " + $ReleaseExe + " after build.")
}
$norm = (Resolve-Path $ReleaseExe).Path
if ($norm -match "[\\/]target[\\/]debug[\\/]") {
  Write-Error ("Refuse to install a debug EXE: " + $norm)
}

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
$copied = $false
for ($i = 0; $i -lt 5; $i++) {
  try {
    Copy-Item -LiteralPath $ReleaseExe -Destination $InstallExe -Force
    $copied = $true
    break
  } catch {
    Start-Sleep -Seconds 1
  }
}
if (-not $copied) {
  Write-Error ("Could not copy to " + $InstallExe + " (file locked?). Quit Continuum and retry.")
}

$meta = Get-Item -LiteralPath $InstallExe
Write-Host ("Installed: " + $meta.FullName)
Write-Host ("Size: " + $meta.Length + "  LastWriteTime: " + $meta.LastWriteTime)
Write-Host "Done. Launch from Start Menu or the path above (not target\debug)."
