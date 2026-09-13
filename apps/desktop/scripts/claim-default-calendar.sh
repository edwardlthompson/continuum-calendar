#!/usr/bin/env bash
# Claim the packaged Continuum .desktop as the default calendar handler.
# Usage: bash apps/desktop/scripts/claim-default-calendar.sh
set -euo pipefail

SYS_DESKTOP=""
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
  echo "Install Continuum first (npm run install:local / dpkg -i the .deb)." >&2
  exit 1
fi

DESKTOP_ID="$(basename "$SYS_DESKTOP")"
# Do not write a ~/.local copy — that would start a second binary next to the .deb.
for mime in text/calendar application/ics x-scheme-handler/webcal x-scheme-handler/webcals; do
  xdg-mime default "$DESKTOP_ID" "$mime"
  echo "$mime -> $(xdg-mime query default "$mime" 2>/dev/null || echo '?')"
done

echo "Claimed defaults using $SYS_DESKTOP"
