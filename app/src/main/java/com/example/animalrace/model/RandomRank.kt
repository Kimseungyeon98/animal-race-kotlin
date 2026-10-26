package com.example.animalrace.model

import kotlin.random.Random

/**
 * 랜덤 등수 모드(번외): 출발 전에 벌칙 받을 등수를 뽑는다.
 * 등수는 경주와 따로 뽑으므로 어느 동물이든 당첨 확률은 [count] ÷ 참가 인원으로 같다.
 */
object RandomRank {
    /** 당첨 인원: 참가 ÷ 3 (나머지 버림), 최소 1명 */
    fun count(racers: Int): Int = maxOf(1, racers / 3)

    /** 1등부터 센 당첨 등수 [count]개, 작은 순. 같은 등수는 두 번 나오지 않는다. */
    fun draw(racers: Int, rnd: Random = Random.Default): List<Int> =
        (1..racers).shuffled(rnd).take(count(racers)).sorted()

    /** "3등·6등" */
    fun label(picks: List<Int>): String = picks.joinToString("·") { "${it}등" }

    /** 설정대로의 당첨 인원: 직접 정한 인원이 있으면 1 ~ 참가 −1 사이로, 없으면 자동 */
    fun count(racers: Int, setting: RankSetting): Int =
        setting.count?.coerceIn(1, maxOf(1, racers - 1)) ?: count(racers)

    /**
     * 이번 판 당첨 등수. 룰렛이면 [count]개를 고르게 뽑고, 직접 지정이면 지정한 등수 중 참가 인원 안의 것.
     * 어느 쪽이든 경주와 따로 정하므로 어느 동물이 그 등수로 들어올지는 출발 순간 무작위다.
     */
    fun picks(racers: Int, setting: RankSetting, rnd: Random = Random.Default): List<Int> = when (setting.mode) {
        RankMode.ROULETTE -> (1..racers).shuffled(rnd).take(count(racers, setting)).sorted()
        RankMode.FIXED -> setting.fixed.filter { it in 1..racers }.distinct().sorted()
    }

    /** 출발할 수 있는지: 직접 지정인데 남는 등수가 없으면 못 간다 */
    fun ready(racers: Int, setting: RankSetting): Boolean =
        setting.mode == RankMode.ROULETTE || setting.fixed.any { it in 1..racers }
}

enum class RankMode { ROULETTE, FIXED }

/** 랜덤 등수 설정. 기본은 룰렛 + 자동 인원(참가 ÷ 3) */
data class RankSetting(
    val mode: RankMode = RankMode.ROULETTE,
    /** 룰렛 당첨 인원. null이면 자동 */
    val count: Int? = null,
    /** 직접 지정한 등수 (1등부터) */
    val fixed: List<Int> = emptyList(),
)
