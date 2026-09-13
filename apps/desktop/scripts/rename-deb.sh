#!/usr/bin/env bash
# Copy the Tauri .deb to a stable GitHub Releases name (no spaces).
# Usage: bash scripts/rename-deb.sh [version]
set -euo pipefail
DESKTOP_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUNDLE_DIR="$DESKTOP_ROOT/src-tauri/target/release/bundle/deb"
VERSION="${1:-}"
if [[ -z "$VERSION" ]]; then
  VERSION="$(node -p "require('$DESKTOP_ROOT/package.json').version")"
fi

if [[ ! -d "$BUNDLE_DIR" ]]; then
  echo "No .deb bundle dir at $BUNDLE_DIR — run: npm run tauri:build" >&2
  exit 1
fi

shopt -s nullglob
debs=("$BUNDLE_DIR"/*.deb)
if [[ ${#debs[@]} -eq 0 ]]; then
  echo "No .deb in $BUNDLE_DIR" >&2
  exit 1
fi

dest="$BUNDLE_DIR/continuum-calendar_${VERSION}_amd64.deb"
src="${debs[0]}"
for candidate in "${debs[@]}"; do
  if [[ "$(basename "$candidate")" == "$(basename "$dest")" ]]; then
    src="$candidate"
    break
  fi
done
if [[ "$(basename "$src")" != "$(basename "$dest")" ]]; then
  cp -f "$src" "$dest"
fi
echo "Stable .deb name: $dest"
