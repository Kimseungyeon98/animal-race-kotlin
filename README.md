# 복불복 동물 레이스 (Kotlin + Jetpack Compose)

**현재 버전: 2.0**

벌칙을 정하고 각자 응원할 동물을 2~10마리 골라 경주시키는 사내용 게임 앱입니다.

## 빌드
1. 프로젝트 루트에 `local.properties` 를 두거나(`sdk.dir=...`), IntelliJ/Android Studio가 자동으로 만들게 합니다.
2. Gradle JDK는 17 또는 21을 사용합니다. (25는 Gradle 8.14가 지원하지 않음)
3. 실행: IDE에서 `app` 실행 구성으로 ▶, 또는 `gradlew installDebug`

## 코드 구조
```
app/src/main/java/com/example/animalrace/
├── MainActivity.kt          진입점. 화면 켜짐 유지, 뒤로가기 처리
├── AppState.kt              앱 전체 상태 + 화면 전환(Setup → Race → Result)
├── model/
│   ├── Animal.kt            동물 14종 (이모지·스킬·대표색·밸런스 계수)
│   └── RaceRecord.kt        경기 기록 모델 + 저장소 인터페이스
├── race/Race.kt             경주 시뮬레이션 엔진 (순수 Kotlin, UI와 무관)
├── data/PrefsRepository.kt  SharedPreferences(JSON)에 설정·최근 10판 저장
└── ui/
    ├── App.kt               화면 전환 애니메이션
    ├── theme/               색상(라이트/다크), 폰트, 출전 번호 색
    ├── components/          공통 컴포넌트(출전 번호, 동물 배지, 큰 버튼 등)
    ├── setup/SetupScreen.kt 메인: 한 화면 동물 선택, 벌칙 칩, 최근 경기 시트
    ├── race/RaceScreen.kt   경주: 따라가는 카메라, 관중석·트랙·흙바닥(Canvas), 순위 띠, 중계 채팅
    └── result/ResultScreen.kt 결과: 벌칙 당첨, 시상대, 전체 기록, 꽃가루
```

## 고쳐 보기 좋은 곳
- 동물 밸런스: `model/Animal.kt` 의 `speed`, `race/Race.kt` 의 `trait()` / `skillRate()`
- 경기 길이: `race/Race.kt` 의 `val base = 24 + 3 * t` (숫자가 클수록 빨리 끝남)
- 색상: `ui/theme/Theme.kt`
- 카메라: `ui/race/RaceScreen.kt` 의 `cameraTarget()` (확대 범위 130~420m, 선두 위치 70%)

## 패치노트

### 2.0
**출전 동물 선택**
- 동물 14마리를 스크롤 없이 한 화면에 보여 주는 3열 타일로 변경 (화면 높이에 맞춰 타일 크기 자동 조절, 태블릿은 5열)
- 고르면 이모지가 튀어 오르고 출전 번호 배지 표시
- 스킬 설명은 마지막으로 누른 동물만 하단에 크게 표시
- 벌칙 입력은 상단 칩으로 축소 (누르면 입력창 + 추천 벌칙)
- 최근 경기 기록은 오른쪽 위 아이콘(개수 배지)을 누르면 아래에서 올라오는 시트로 이동
- 제목 아래에 "지난 판 꼴찌" 표시

**경주 화면**
- 세로 화면 전체를 쓰는 레이아웃 (순위 띠 / 관중석 / 트랙 / 흙바닥)
- 선두 무리를 따라가는 카메라: 앞쪽 4마리가 화면에 들어오도록 자동 확대·축소
- 배경이 흘러가는 연출: 10m 레일 기둥, 100m 거리 표지판, 잔디 줄무늬, 더 느리게 흐르는 관중석(패럴랙스)
- 출발 게이트에서 시작하고, 결승선과 FINISH 아치는 후반에 등장
- 1착 골인 시 화면 중앙 배너, 이후 카메라가 남은 동물의 꼴찌 싸움을 추적
- 화면 왼쪽 밖으로 밀려난 동물은 선두와의 거리 표시 (`◀ 🐢 -42m`)
- 가속 중인 동물에 불꽃 잔상, 스킬 아이콘 말풍선 확대
- 중계를 트랙 아래 흙바닥 영역의 반투명 채팅 4줄로 변경 (최신이 아래, 주자를 가리지 않음)

### 1.0
- 첫 완성본: 동물 14종과 고유 스킬, 후반 가속·역전 보정 경주 엔진
- 메인(벌칙 입력, 동물 선택, 최근 10판), 경주, 결과(시상대, 벌칙 당첨, 꽃가루) 화면
- 라이트/다크 테마, 진동 피드백, 기록 로컬 저장

폰트: Black Han Sans, IBM Plex Mono (SIL Open Font License 1.1)
