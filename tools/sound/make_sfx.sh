#!/usr/bin/env bash
# 효과음 레시피(sfx.html) → app/src/main/res/raw/sfx_*.ogg
# 헤드리스 크롬(OfflineAudioContext)으로 합성하고 ffmpeg으로 ogg(vorbis) 변환
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
out="$here/../../app/src/main/res/raw"
tmp="$(mktemp -d)"
NODE_PATH="${NODE_PATH:-$(npm root -g)}" node "$here/render.js" > "$tmp/all.txt"
mkdir -p "$out"
grep -v '^DONE$' "$tmp/all.txt" | while read -r name b64; do
  echo "$b64" | base64 -d > "$tmp/$name.wav"
  ffmpeg -nostdin -loglevel error -y -i "$tmp/$name.wav" -c:a libvorbis -q:a 4 "$out/sfx_$name.ogg"
  echo "sfx_$name.ogg"
done
rm -rf "$tmp"
