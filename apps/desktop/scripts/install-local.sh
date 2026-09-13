#!/usr/bin/env bash
# Install Continuum Calendar as a Debian package (single system copy).
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

USER_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/continuum-calendar"
USER_BIN="${XDG_BIN_HOME:-$HOME/.local/bin}/continuum-calendar"
USER_DESKTOP="${XDG_DATA_HOME:-$HOME/.local/share}/applications/org.continuumcalendar.app.desktop"
AUTOSTART_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/autostart"
USER_AUTOSTART="$AUTOSTART_DIR/org.continuumcalendar.app.desktop"
VERSION="$(node -p "require('$DESKTOP_ROOT/package.json').version")"
SKIP_BUILD="${CONTINUUM_INSTALL_SKIP_BUILD:-0}"
STABLE_DEB="$DESKTOP_ROOT/src-tauri/target/release/bundle/deb/continuum-calendar_${VERSION}_amd64.deb"

echo "Stopping Continuum (user-local and packaged)..."
pkill -f "$USER_DIR/app" 2>/dev/null || true
pkill -f '/usr/bin/continuum-calendar' 2>/dev/null || true
sleep 1

cd "$DESKTOP_ROOT"
if [[ "$SKIP_BUILD" != "1" ]]; then
  echo "Building Linux .deb (tauri build)..."
  npm run tauri:build
fi

bash "$SCRIPT_DIR/rename-deb.sh" "$VERSION"
if [[ ! -f "$STABLE_DEB" ]]; then
  echo "Missing $STABLE_DEB after build." >&2
  exit 1
fi

echo "Removing leftover user-local launchers so only the .deb can start..."
rm -f "$USER_BIN"
rm -rf "$USER_DIR"
rm -f "$USER_DESKTOP"
rm -f "$USER_AUTOSTART"
rm -f "${XDG_DATA_HOME:-$HOME/.local/share}/applications/continuum-calendar.desktop"

echo "Installing $STABLE_DEB (needs admin)..."
if command -v pkexec >/dev/null 2>&1 && [[ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
  pkexec dpkg -i "$STABLE_DEB" || pkexec apt-get install -f -y
else
  sudo dpkg -i "$STABLE_DEB" || sudo apt-get install -f -y
fi

SYS_DESKTOP=""
shopt -s nullglob
for candidate in \
  /usr/share/applications/continuum-calendar.desktop \
  /usr/share/applications/org.continuumcalendar.app.desktop \
  /usr/share/applications/Continuum\ Calendar.desktop; do
  if [[ -f "$candidate" ]]; then
    SYS_DESKTOP="$candidate"
    break
  fi
done
if [[ -z "$SYS_DESKTOP" ]]; then
  echo "dpkg installed, but no Continuum .desktop was found under /usr/share/applications." >&2
  exit 1
fi

mkdir -p "$AUTOSTART_DIR"
cp -f "$SYS_DESKTOP" "$AUTOSTART_DIR/$(basename "$SYS_DESKTOP")"
sed -i 's| %u||g' "$AUTOSTART_DIR/$(basename "$SYS_DESKTOP")" 2>/dev/null || true
if ! grep -q '^X-GNOME-Autostart-enabled=' "$AUTOSTART_DIR/$(basename "$SYS_DESKTOP")"; then
  echo 'X-GNOME-Autostart-enabled=true' >> "$AUTOSTART_DIR/$(basename "$SYS_DESKTOP")"
fi

update-desktop-database "${XDG_DATA_HOME:-$HOME/.local/share}/applications" 2>/dev/null || true
bash "$SCRIPT_DIR/claim-default-calendar.sh" || echo "Warning: could not claim default calendar (xdg-mime)."

echo "Installed: $STABLE_DEB"
echo "Launcher:  $(command -v continuum-calendar || echo /usr/bin/continuum-calendar)"
echo "Desktop:   $SYS_DESKTOP"
echo "Autostart: $AUTOSTART_DIR/$(basename "$SYS_DESKTOP")"
echo "Done. Launch with continuum-calendar (one instance)."
