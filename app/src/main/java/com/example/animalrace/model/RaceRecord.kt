package com.example.animalrace.model

/** 한 판의 결과. [results]는 1위부터 순서대로. */
data class RaceRecord(
    val at: Long,
    val bet: String,
    val leadChanges: Int,
    val results: List<RaceResult>,
    /** 경기장 (3.5.1 이전 기록은 null) */
    val venue: Venue? = null,
    /** 랜덤 등수 모드로 뽑은 당첨 등수(1등부터, 작은 순). null이면 꼴찌 벌칙 판 */
    val picks: List<Int>? = null,
    /** 랜덤 등수 당첨자끼리 몰아주기를 했다면 그 결과 */
    val miniGame: MiniGameResult? = null,
    /** 당첨 등수를 룰렛 대신 미리 직접 정한 판 */
    val picksFixed: Boolean = false,
) {
    /** 벌칙 당첨 결과: 랜덤 판이면 뽑힌 등수의 동물들, 아니면 꼴찌 */
    val losers: List<RaceResult> get() = picks?.mapNotNull { results.getOrNull(it - 1) } ?: listOf(results.last())
}

data class RaceResult(
    val animal: Animal,
    val time: Double,
    val skillUses: Int,
    /** 출전 번호(0부터) */
    val lane: Int = 0,
    /** 이 동물로 달린 사람 이름 (4.0, 없으면 null) */
    val who: String? = null,
) {
    /** 화면에 부를 이름: 사람 이름이 있으면 사람, 없으면 동물 */
    val name: String get() = who ?: animal.label
}

data class SavedSetup(
    val animals: List<Animal>,
    val bet: String,
    val randomRank: Boolean = false,
    val rank: RankSetting = RankSetting(),
    /** 최근에 쓴 사람 이름 (최근 것이 앞) */
    val members: List<String> = emptyList(),
    /** 멤버 묶음: 자주 모이는 사람들 */
    val groups: List<List<String>> = emptyList(),
)

/** 기록·설정 저장소. 실제 구현은 Android의 SharedPreferences([com.example.animalrace.data.PrefsRepository]). */
interface AppRepository {
    fun loadSetup(): SavedSetup?
    fun saveSetup(setup: SavedSetup)
    fun loadHistory(): List<RaceRecord>
    fun saveHistory(history: List<RaceRecord>)
    fun loadSoundOn(): Boolean = true
    fun saveSoundOn(on: Boolean) {}
    fun loadMusicOn(): Boolean = true
    fun saveMusicOn(on: Boolean) {}
}
