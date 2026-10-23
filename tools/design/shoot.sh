#!/usr/bin/env bash
# 캔버스 보드를 PNG로: tools/design/shoot.sh <보드.dc.html> <너비> <높이> <출력.png> [tick]
# DC_PROJECT=캔버스 project/ 폴더 (Artifact read로 받은 곳). 크롬은 창 높이보다 화면이 ~90px 작게 찍히니 높이에 여유를 둘 것
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
board="$1"; w="$2"; h="$3"; out="$4"; tick="${5:-6}"
chrome="${CHROME:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"
html="$(mktemp --suffix=.html)"
node "$here/dc_render.js" "$board" "$tick" "$html"
"$chrome" --headless --no-sandbox --disable-gpu --hide-scrollbars --window-size="$w,$((h + 100))" --screenshot="$(realpath -m "$out")" "file://$html" 2>/dev/null
rm -f "$html"
echo "$out"
