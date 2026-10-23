#!/usr/bin/env bash
# 실기기 녹화(1080×2340)에서 README용 움짤 만들기:
#   tools/design/make_gif.sh <영상.mp4> <시작초> <끝초> <출력.gif> [배속=1] [fps=12]
# 상단 상태바·하단 내비게이션 바를 잘라내고(y 96~2240) 360×715로 줄인다 (README의 다른 움짤과 같은 규격)
set -euo pipefail
src="$1"; from="$2"; to="$3"; out="$4"; speed="${5:-1}"; fps="${6:-12}"
ffmpeg -v error -ss "$from" -to "$to" -i "$src" -vf \
  "setpts=PTS/$speed,crop=1080:2144:0:96,fps=$fps,scale=360:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle" \
  -y "$out"
echo "$out $(stat -c%s "$out") bytes"
