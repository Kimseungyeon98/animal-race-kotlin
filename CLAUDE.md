# 동물 레이스 — 작업 규칙

Kotlin + Jetpack Compose 안드로이드 앱. 소유자는 Java Spring 개발자이고 **한국어**로 대화한다.
이 문서는 세션이 바뀌거나 대화가 요약돼도 같은 방식으로 일하기 위한 것이다. 규칙이 바뀌면 이 문서도 같이 고친다.

## 절대 규칙

- **공정성**: 최종 순위는 출발 순간 균등 무작위(`race/Race.kt`). 스킬·연출·경기장·랜덤 등수·미니게임은 **확률에 영향을 주면 안 된다.**
  새 무작위 요소를 넣으면 "누구나 같은 확률"을 검사하는 테스트를 같이 넣는다 (`FairnessTest`, `RandomRankTest`, `MiniGameTest` 참고).
- 숨긴 결과(랜덤 등수, 몰빵 칸·복권)는 게임 시작 때 고르게 뽑고, 같은 판에서 다시 열어도 바뀌지 않게 한다 (다시 뽑기 방지).
- **앱 ID `com.animalrace.android`는 절대 바꾸지 않는다** (5.0부터 구글 플레이 출시용. 바꾸면 다른 앱이 됨). 코드 패키지 `com.example.animalrace`는 별개.
- **서명 키·비밀번호는 저장소에 올리지 않는다**: `keystore.properties`(gitignore)로만 연결.
- 사용자에게 보이는 문구는 한국어. 코드 식별자는 영어로, 뜻이 바로 보이게 (예: `Mollbang` 대신 `MiniGame`).

## 작업 순서 (기능·디자인 변경)

1. **시안 먼저**: 디자인 캔버스에 보드를 만들어 보여 주고 확인받는다. "바로 수정하지 말고 시안부터"가 기본.
2. 확인받으면 앱에 반영 → `tools/verify`로 컴파일·테스트.
3. **버전 올리기**: `app/build.gradle.kts`의 `versionCode` +1, `versionName` 올림 (기능 = 3.x, 수정·문서 = 3.x.y).
4. **README**: 버전 배지, 📋 전체 패치노트에 새 버전 항목(맨 위), 큰 변화면 "이렇게 자라 왔어요" 표 **맨 아래**에도 한 줄 (표는 오래된 버전부터 순서대로).
5. **캔버스 변경 이력 보드**(`Changelog.dc.html`)에 새 버전을 맨 위에 추가하고 직전 버전 색은 흰색으로. 반영한 시안 보드 제목에 "(앱 x.y 반영)".
6. main에 커밋·푸시 (아래 커밋 형식). PR은 요청할 때만.
7. 태그는 소유자가 직접 붙인다. 이렇게 안내한다:
   ```
   git tag vX.Y <커밋>
   git push origin vX.Y
   ```
   "태그 붙였어"라고 하면 `git ls-remote --tags origin vX.Y`로 확인한다.

## 커밋 형식

```
X.Y: 한 줄 요약

- 바뀐 점

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: <세션 링크>
```
커밋·PR·코드에 모델 ID를 쓰지 않는다. 커밋 전에 훅이 테스트를 돌린다 (아래).

## 검증 (Android SDK 없음)

- 이 환경엔 Android SDK가 없다 (Google Maven은 3.18 이후 허용됨). 대신 `tools/verify`가 앱 소스를 **데스크톱 Compose**로 컴파일하고 단위 테스트를 돌린다:
  `cd tools/verify && ./gradlew compileKotlin test -q --offline` (캐시가 없으면 `--offline` 빼고)
- `MainActivity.kt`, `data/PrefsRepository.kt`, `data/AndroidSound.kt`, `ui/theme/Fonts.kt`는 안드로이드 전용이라 **검증에서 빠진다.** 이 파일을 고치면 코드를 눈으로 한 번 더 확인하고, 실기기 확인이 필요하다고 말한다.
- **앱 화면 PNG**: `cd tools/verify && ./gradlew shot [-Pscene=이름]` → `build/shots/*.png` (진짜 글꼴, 폰 390×844dp).
  화면·그림을 바꾸면 찍어서 직접 보고 확인한다. 장면은 `tools/verify/sim/com/example/animalrace/shot/Shot.kt`에 추가.
  손맛(터치·진동·소리·애니메이션 흐름)은 여전히 "실기기에서 확인 필요"라고 정직하게 말한다.
- **그림 작업**(캐릭터·연출·미니게임 소품)은 먼저 `.claude/skills/animal-art/SKILL.md`를 읽는다. 레퍼런스 이미지는 `docs/design/refs/`.
- 커밋 전 훅: `.claude/settings.json` → `tools/hooks/test-before-commit.sh` (테스트 실패 시 커밋 차단).

## 디자인 캔버스

- 주소: https://claude.ai/artifact/8KhwSxQTVQjeQMT5Ku1tXt (Design 타입, 보드는 `project/*.dc.html`)
- **게시할 때 함정**: `root`는 project의 **상위** 폴더, `file_path`는 `.../project/canvas.json`, 보드 파일은 `files`에 `"project/이름.dc.html"`로.
  (`root`를 project 자체로 하면 캔버스가 안 읽는 `canvas.json`에 올라가서 아무것도 안 바뀐다.)
- 게시 전에 서버 쪽이 바뀌었을 수 있으니 거절되면 `Artifact read`로 다시 받아서 합친다. `edits`와 `files`는 같이 못 쓴다.
- 캔버스 댓글 알림은 연결되지 않는다(mint_failed). 의견은 대화로 받는다.
- 보드 미리보기: `tools/design/shoot.sh` (캔버스 project/ 폴더가 있는 곳에서 실행). 한 보드 안에서 SVG `id`(마스크·패턴)는 겹치지 않게 고유하게.
- 섹션 순서: 방향 시안 → 디자인 시스템 → 캐릭터 → 화면 → 변경 이력 → … → 랜덤 등수 → 몰아주기 미니게임 → 소리. (보류 시안은 두지 않고, 안 쓰기로 한 시안은 지운다)

## README 움짤

- 소유자가 실기기 녹화(1080×2340)를 GitHub 릴리스 draft나 대화에 올린다. draft 자산은 `gh api repos/Kimseungyeon98/animal-race-kotlin/releases`로 찾는다.
- `tools/design/make_gif.sh <영상> <시작> <끝> <출력.gif> [배속] [fps]` — **상단 상태바·하단 내비게이션 바는 반드시 잘라낸다**(y 96~2240), 360×715, 3MB 안팎.
- 장면 경계는 프레임을 뽑아 눈으로 확인한 뒤 자른다. 움짤은 `docs/media/`.
- 경기장 연출처럼 작은 장면은 7번째 인자로 그 부분만 잘라 크게 (`600:600:x:y`, 폭 320).
- README는 움짤을 **핵심만** 둔다(지금 12개: 대표 1 · 이렇게 놀아요 3 · 경기장 4 · 우리 기록 1 · 미니게임 3). 버전 표·패치노트에는 그림을 넣지 않고, 새 움짤은 기존 것을 바꾸는 방향으로.

## 코드 지도

| 무엇 | 어디 |
|---|---|
| 경주 엔진·전개·해설 | `race/Race.kt` |
| 앱 상태·화면 전환·랜덤 등수/미니게임 추첨 | `AppState.kt` |
| 랜덤 등수 / 미니게임·사다리 모델 | `model/RandomRank.kt`, `model/MiniGame.kt` |
| 사람 이름(참가자·최근 멤버·멤버 묶음) / 우리 기록 통계 | `AppState.kt`의 `names`·`members`·`groups`, `ui/setup/SetupScreen.kt`의 `NameSheet` / `model/Stats.kt`, `ui/setup/OurRecords.kt` |
| 결과 공유 카드 (카드 / 안드로이드 공유·저장) | `ui/result/ShareCard.kt` / `data/AndroidSharer.kt` (검증 빌드에서 제외, `res/xml/share_paths.xml`) |
| 메인 / 경주(룰렛 포함) / 결과 / 미니게임 화면 | `ui/setup/`, `ui/race/RaceScreen.kt`, `ui/result/`, `ui/minigame/` |
| 캐릭터 그림·연출, 경기장 | `ui/art/` (달리기 걸음은 `ui/art/Gait.kt`), `ui/race/Stadium.kt`, `ui/race/Guests.kt` |
| 색·폰트·공통 컴포넌트 | `ui/theme/`, `ui/components/` |
| 효과음·배경음악 (종류 / 재생 / 파일 만들기) | `ui/sound/Sound.kt`, `data/AndroidSound.kt`, `res/raw/sfx_*.ogg`·`bgm_*.ogg` ← `tools/sound/make_sfx.sh` (sfx.html·bgm.html). 다시 돌리면 효과음의 잡음이 매번 달라지니, 바꾼 소리 파일만 커밋 |

## 스토어 배포 전 할 일

패키지명 변경(`com.example` → 스토어 배포 전 필수), targetSdk 올리기. (신규 캐릭터·해적 룰렛·악어 이빨 시안은 3.18에서 접고 지움. 결과 공유 카드는 4.1에서 새로 만듦)
