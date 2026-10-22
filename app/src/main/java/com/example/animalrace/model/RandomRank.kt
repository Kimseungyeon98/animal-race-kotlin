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
}
