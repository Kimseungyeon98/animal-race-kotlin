# tools — 개발 보조 도구

앱 빌드와는 상관없는, 검증·디자인·문서 작업용 도구. 규칙은 루트의 `CLAUDE.md`.

| 폴더 | 내용 |
|---|---|
| `verify/` | Android SDK 없이 앱 소스를 **데스크톱 Compose(JVM)**로 컴파일하고 단위 테스트를 돌리는 Gradle 프로젝트. `stub/`는 안드로이드 전용 폰트 대신 쓰는 대역, `sim/`은 공정성·전개 시뮬레이션 |
| `verify/sim/.../shot/Shot.kt` | **앱 화면 PNG**: `cd verify && ./gradlew shot [-Pscene=characters]` → `verify/build/shots/` (진짜 글꼴, 390×844dp, 2배). 장면: characters·crowns·fx·skills·skills-alt·race(시간대별)·roulette·setup·result·ladder·lotto·gacha, 터치 각본 `t-*` (`-Pscene=t` 로 전부): t-gacha·t-lotto·t-ladder·t-setup(기록)·t-dex·t-bet·t-result·t-exit |
| `design/dc_render.js` | 디자인 캔버스 보드(`.dc.html`)를 정적 HTML로 그리기 (sc-for / sc-if / `{{값}}` / Animal 컴포넌트) |
| `design/shoot.sh` | 보드 → PNG (헤드리스 크롬) |
| `design/make_gif.sh` | 실기기 녹화 → README 움짤 (상태바·내비게이션 바 잘라내고 360×715) |
| `sound/make_sfx.sh` | 효과음 레시피(`sound/sfx.html`, 캔버스 "효과음 키트")·배경음악 악보(`sound/bgm.html`, 캔버스 "배경음악 시안") → 헤드리스 크롬 합성 → `app/src/main/res/raw/sfx_*.ogg`, `bgm_*.ogg` |
| `hooks/test-before-commit.sh` | Claude Code 훅: `git commit` 전에 `verify` 테스트, 실패하면 커밋 차단 |

```bash
# 테스트
cd tools/verify && ./gradlew compileKotlin test -q --offline

# 보드 미리보기 (캔버스 project/ 폴더가 있는 곳에서)
DC_PROJECT=./project tools/design/shoot.sh MiniGame.dc.html 1400 3720 out.png

# 움짤
tools/design/make_gif.sh 녹화.mp4 12.5 20.0 docs/media/새움짤.gif 1.2 12
```
