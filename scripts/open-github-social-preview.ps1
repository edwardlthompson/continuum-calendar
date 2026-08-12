# Open GitHub Social preview settings + reveal the Continuum neon upload image.
# GitHub has no public API to set the social preview — this is a one-click helper for the HUMAN step.
# Usage (repo root): .\scripts\open-github-social-preview.ps1
param(
    [string]$Repo = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$Upload = Join-Path $Root "docs\brand\github-social-neon-upload.jpg"
$Source = Join-Path $Root "docs\brand\github-social-neon.png"

if (-not (Test-Path $Upload)) {
    if (-not (Test-Path $Source)) {
        Write-Host "ERROR: missing $Source — generate brand assets first."
        exit 1
    }
    Write-Host "Creating under-1MB upload JPEG from neon social PNG…"
    python -c @"
from PIL import Image
from pathlib import Path
src = Path(r'$($Source.Replace('\','/'))')
out = Path(r'$($Upload.Replace('\','/'))')
im = Image.open(src).convert('RGB').resize((1280, 640), Image.Resampling.LANCZOS)
im.save(out, format='JPEG', quality=88, optimize=True)
print(out, out.stat().st_size)
"@
}

if (-not (Test-Path $Upload)) {
    Write-Host "ERROR: could not create $Upload"
    exit 1
}

$size = (Get-Item $Upload).Length
if ($size -ge 1MB) {
    Write-Host "ERROR: upload file is $size bytes (GitHub limit is 1 MB). Recompress first."
    exit 1
}

if (-not $Repo) {
    $Repo = (gh repo view --json nameWithOwner -q .nameWithOwner 2>$null)
}
if (-not $Repo) {
    Write-Host "ERROR: could not detect repo. Pass -Repo owner/name"
    exit 1
}

$SettingsUrl = "https://github.com/$Repo/settings"
Write-Host ""
Write-Host "GitHub Social preview (no API — UI upload required)"
Write-Host "  Repo:     $Repo"
Write-Host "  Upload:   $Upload  ($([math]::Round($size/1KB,1)) KB)"
Write-Host "  Settings: $SettingsUrl"
Write-Host ""
Write-Host "Steps:"
Write-Host "  1. Settings page opens → scroll to Social preview → Edit → Upload an image…"
Write-Host "  2. Choose the file revealed in Explorer (github-social-neon-upload.jpg)"
Write-Host "  3. Save"
Write-Host ""

# Reveal file in Explorer + open settings
Start-Process explorer.exe "/select,$Upload"
Start-Process $SettingsUrl

Write-Host "When finished, paste in chat:"
Write-Host "[HUMAN] GitHub social preview set to docs/brand/github-social-neon-upload.jpg"
