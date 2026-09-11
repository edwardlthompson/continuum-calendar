#!/usr/bin/env bash
# Install Continuum Calendar locally (release only) on Linux.
# Usage (from apps/desktop):  npm run install:local
# Or:  bash scripts/install-local.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DESKTOP_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ ! -f "$DESKTOP_ROOT/package.json" ]]; then
  echo "Run from apps/desktop (scripts/install-local.sh). Found root: $DESKTOP_ROOT" >&2
  exit 1
fi

cwd_leaf="$(basename "$PWD")"
if [[ "$cwd_leaf" == "debug" && "$PWD" == *"/target/debug" ]]; then
  echo "Refuse to run install-local from target/debug. Use apps/desktop." >&2
  exit 1
fi

INSTALL_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/continuum-calendar"
INSTALL_BIN="${XDG_BIN_HOME:-$HOME/.local/bin}/continuum-calendar"
RELEASE_BIN="$DESKTOP_ROOT/src-tauri/target/release/app"
DIST_INDEX="$DESKTOP_ROOT/dist/index.html"
DESKTOP_FILE="${XDG_DATA_HOME:-$HOME/.local/share}/applications/org.continuumcalendar.app.desktop"
AUTOSTART_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/autostart"
AUTOSTART_FILE="$AUTOSTART_DIR/org.continuumcalendar.app.desktop"
ICON_SRC="$DESKTOP_ROOT/src-tauri/icons"
ICON_DST="${XDG_DATA_HOME:-$HOME/.local/share}/icons/hicolor"
VERSION="$(node -p "require('$DESKTOP_ROOT/package.json').version")"
SKIP_BUILD="${CONTINUUM_INSTALL_SKIP_BUILD:-0}"

echo "Stopping installed Continuum (if running)..."
pkill -f "$INSTALL_DIR/app" 2>/dev/null || true
pkill -x continuum-calendar 2>/dev/null || true
sleep 1

cd "$DESKTOP_ROOT"
if [[ "$SKIP_BUILD" != "1" ]]; then
  echo "Building release with embedded frontend (tauri build --no-bundle)..."
  npm run tauri:build -- --no-bundle
fi

if [[ ! -f "$DIST_INDEX" ]]; then
  echo "Missing $DIST_INDEX after build — frontend was not embedded." >&2
  exit 1
fi
if [[ ! -f "$RELEASE_BIN" ]]; then
  echo "Missing $RELEASE_BIN after build." >&2
  exit 1
fi
if [[ "$RELEASE_BIN" == *"/target/debug/"* ]]; then
  echo "Refuse to install a debug binary: $RELEASE_BIN" >&2
  exit 1
fi

mkdir -p "$INSTALL_DIR" "$(dirname "$INSTALL_BIN")" "$(dirname "$DESKTOP_FILE")" "$AUTOSTART_DIR"
mkdir -p "$ICON_DST/32x32/apps" "$ICON_DST/128x128/apps" "$ICON_DST/256x256/apps"
cp -f "$RELEASE_BIN" "$INSTALL_DIR/app"
chmod +x "$INSTALL_DIR/app"
ln -sfn "$INSTALL_DIR/app" "$INSTALL_BIN"

if [[ -f "$ICON_SRC/32x32.png" ]]; then
  cp -f "$ICON_SRC/32x32.png" "$ICON_DST/32x32/apps/org.continuumcalendar.app.png"
fi
if [[ -f "$ICON_SRC/128x128.png" ]]; then
  cp -f "$ICON_SRC/128x128.png" "$ICON_DST/128x128/apps/org.continuumcalendar.app.png"
  cp -f "$ICON_SRC/128x128.png" "$INSTALL_DIR/icon.png"
fi
if [[ -f "$ICON_SRC/128x128@2x.png" ]]; then
  cp -f "$ICON_SRC/128x128@2x.png" "$ICON_DST/256x256/apps/org.continuumcalendar.app.png"
fi

cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=Continuum Calendar
Comment=Continuum Calendar
Exec="$INSTALL_DIR/app" %u
Icon=org.continuumcalendar.app
Terminal=false
Categories=Office;Calendar;
StartupWMClass=org.continuumcalendar.app
MimeType=text/calendar;application/ics;x-scheme-handler/webcal;x-scheme-handler/webcals;
StartupNotify=true
EOF

# Start at login (XDG). Plugin-autostart can rewrite this; keep a release Exec path.
cp -f "$DESKTOP_FILE" "$AUTOSTART_FILE"
# Autostart must not pass a leftover %u from a desktop-file open.
sed -i 's| %u||g' "$AUTOSTART_FILE" 2>/dev/null || true
if ! grep -q '^X-GNOME-Autostart-enabled=' "$AUTOSTART_FILE"; then
  echo 'X-GNOME-Autostart-enabled=true' >> "$AUTOSTART_FILE"
fi

update-desktop-database "$(dirname "$DESKTOP_FILE")" 2>/dev/null || true
gtk-update-icon-cache -f "$ICON_DST" 2>/dev/null || true

# Become the default calendar handler (Thunderbird-style).
bash "$SCRIPT_DIR/claim-default-calendar.sh" || echo "Warning: could not claim default calendar (xdg-mime)."

# Optional: rename bundled AppImage if present (stable GitHub Releases name)
BUNDLE_DIR="$DESKTOP_ROOT/src-tauri/target/release/bundle/appimage"
if [[ -d "$BUNDLE_DIR" ]]; then
  shopt -s nullglob
  for img in "$BUNDLE_DIR"/*.AppImage; do
    dest="$BUNDLE_DIR/Continuum-Calendar-${VERSION}-x86_64.AppImage"
    if [[ "$(basename "$img")" != "$(basename "$dest")" ]]; then
      cp -f "$img" "$dest"
      echo "Stable AppImage name: $dest"
    fi
  done
fi

echo "Installed: $INSTALL_DIR/app"
echo "Launcher:  $INSTALL_BIN"
echo "Desktop:   $DESKTOP_FILE"
echo "Autostart: $AUTOSTART_FILE"
echo "Done. Launch with continuum-calendar (or the path above), not target/debug."
