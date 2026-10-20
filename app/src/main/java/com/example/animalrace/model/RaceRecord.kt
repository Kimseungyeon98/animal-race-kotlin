package com.example.animalrace.model

/** 한 판의 결과. [results]는 1위부터 순서대로. */
data class RaceRecord(
    val at: Long,
    val bet: String,
    val leadChanges: Int,
    val results: List<RaceResult>,
)

data class RaceResult(
    val animal: Animal,
    val time: Double,
    val skillUses: Int,
    /** 출전 번호(0부터) */
    val lane: Int = 0,
)

data class SavedSetup(
    val animals: List<Animal>,
    val bet: String,
)

/** 기록·설정 저장소. 실제 구현은 Android의 SharedPreferences([com.example.animalrace.data.PrefsRepository]). */
interface AppRepository {
    fun loadSetup(): SavedSetup?
    fun saveSetup(setup: SavedSetup)
    fun loadHistory(): List<RaceRecord>
    fun saveHistory(history: List<RaceRecord>)
}
