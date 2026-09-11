#!/usr/bin/env bash
# After `tauri build` (with AppImage target), copy/rename to the stable release asset name.
# Usage: bash scripts/rename-appimage.sh [version]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DESKTOP_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION="${1:-$(node -p "require('$DESKTOP_ROOT/package.json').version")}"
BUNDLE_DIR="$DESKTOP_ROOT/src-tauri/target/release/bundle/appimage"
DEST="$BUNDLE_DIR/Continuum-Calendar-${VERSION}-x86_64.AppImage"

if [[ ! -d "$BUNDLE_DIR" ]]; then
  echo "No AppImage bundle dir at $BUNDLE_DIR — run: npm run tauri:build" >&2
  exit 1
fi

shopt -s nullglob
imgs=("$BUNDLE_DIR"/*.AppImage)
if [[ ${#imgs[@]} -eq 0 ]]; then
  echo "No .AppImage files in $BUNDLE_DIR" >&2
  exit 1
fi

src=""
for img in "${imgs[@]}"; do
  base="$(basename "$img")"
  if [[ "$base" == "$(basename "$DEST")" ]]; then
    echo "Already named: $DEST"
    exit 0
  fi
  if [[ "$base" != Continuum-Calendar-*-x86_64.AppImage ]]; then
    src="$img"
    break
  fi
done
src="${src:-${imgs[0]}}"
cp -f "$src" "$DEST"
chmod +x "$DEST"
echo "Wrote $DEST"
