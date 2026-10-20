# 복불복 동물 레이스 (Kotlin + Jetpack Compose)

**현재 버전: 1.0**

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
    ├── setup/SetupScreen.kt 메인: 벌칙 입력, 동물 선택, 최근 경기
    ├── race/RaceScreen.kt   경주: 트랙(Canvas), 실시간 순위, 중계
    └── result/ResultScreen.kt 결과: 벌칙 당첨, 시상대, 전체 기록, 꽃가루
```

## 고쳐 보기 좋은 곳
- 동물 밸런스: `model/Animal.kt` 의 `speed`, `race/Race.kt` 의 `trait()` / `skillRate()`
- 경기 길이: `race/Race.kt` 의 `val base = 24 + 3 * t` (숫자가 클수록 빨리 끝남)
- 색상: `ui/theme/Theme.kt`

폰트: Black Han Sans, IBM Plex Mono (SIL Open Font License 1.1)
