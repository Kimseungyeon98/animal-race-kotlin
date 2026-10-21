package com.example.animalrace.race

import com.example.animalrace.model.Animal
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 전개가 읽히지 않는지 검사한다. 중반(우승자 결승 시각의 절반)에 앞선 동물이
 * 항상 지거나 항상 이기면 몇 판 만에 결과가 보이므로, 목표 비율 근처여야 한다.
 */
class StoryTest {

    private fun midLeaderWinRate(field: Int, races: Int, seed: Int): Double {
        val rnd = Random(seed)
        var wins = 0
        repeat(races) {
            val race = Race(Animal.entries.shuffled(rnd).take(field), rnd)
            val winner = race.racers.minBy { race.finishTimeOf(it) }
            val half = race.finishTimeOf(winner) * 0.5
            var midLeader: Racer? = null
            while (!race.done) {
                race.step(1 / 60.0)
                if (midLeader == null && race.t >= half) midLeader = race.racers.maxBy { it.x }
            }
            if (midLeader === winner) wins++
        }
        return wins.toDouble() / races
    }

    @Test
    fun twoRacersMidLeaderWinsAboutHalf() {
        val rate = midLeaderWinRate(2, 1500, 3)
        assertTrue("2마리 중반 선두 우승 비율 $rate (목표 $LEADER_WINS_2)", rate in 0.42..0.58)
    }

    @Test
    fun manyRacersMidLeaderSometimesWins() {
        val rate = midLeaderWinRate(6, 1500, 4)
        assertTrue("6마리 중반 선두 우승 비율 $rate (목표 $LEADER_WINS_N)", rate in 0.32..0.48)
    }
}
