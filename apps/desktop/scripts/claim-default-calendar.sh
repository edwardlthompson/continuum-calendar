#!/usr/bin/env bash
# Claim Continuum as the default calendar app (MIME + webcal).
# Usage: bash apps/desktop/scripts/claim-default-calendar.sh
set -euo pipefail

DESKTOP_ID="org.continuumcalendar.app.desktop"
APPS_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
INSTALL_APP="${XDG_DATA_HOME:-$HOME/.local/share}/continuum-calendar/app"
DESKTOP_FILE="$APPS_DIR/$DESKTOP_ID"

if [[ ! -x "$INSTALL_APP" ]]; then
  if [[ -x "${XDG_DATA_HOME:-$HOME/.local/share}/continuum-calendar/app-fixed" ]]; then
    INSTALL_APP="${XDG_DATA_HOME:-$HOME/.local/share}/continuum-calendar/app-fixed"
  else
    echo "Install Continuum first (npm run install:local). Missing: $INSTALL_APP" >&2
    exit 1
  fi
fi

mkdir -p "$APPS_DIR"
cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=Continuum Calendar
Comment=Continuum Calendar
Exec="$INSTALL_APP" %u
Icon=org.continuumcalendar.app
Terminal=false
Categories=Office;Calendar;
StartupWMClass=org.continuumcalendar.app
MimeType=text/calendar;application/ics;x-scheme-handler/webcal;x-scheme-handler/webcals;
StartupNotify=true
EOF

update-desktop-database "$APPS_DIR" 2>/dev/null || true

for mime in text/calendar application/ics x-scheme-handler/webcal x-scheme-handler/webcals; do
  xdg-mime default "$DESKTOP_ID" "$mime"
  echo "$mime -> $(xdg-mime query default "$mime" 2>/dev/null || echo '?')"
done

echo "Claimed defaults using $DESKTOP_FILE"
