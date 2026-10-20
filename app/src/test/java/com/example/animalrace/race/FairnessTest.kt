package com.example.animalrace.race

import com.example.animalrace.model.Animal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 스킬은 연출일 뿐이므로 어떤 동물이든 모든 순위에 같은 확률로 와야 한다.
 * 14마리 전원 출전을 반복해 동물 × 순위 분포를 카이제곱으로 검정한다.
 */
class FairnessTest {

    @Test
    fun everyAnimalIsEquallyLikelyToFinishInEveryPlace() {
        val rnd = Random(20261020)
        val animals = Animal.entries
        val k = animals.size
        val races = 20_000
        val counts = Array(k) { IntArray(k) }

        repeat(races) {
            val race = Race(animals.shuffled(rnd), rnd)
            while (!race.done) race.step(1 / 60.0)
            race.ranking().forEachIndexed { place, r -> counts[r.animal.ordinal][place]++ }
        }

        val expected = races.toDouble() / k
        val chi = counts.sumOf { row -> row.sumOf { (it - expected) * (it - expected) / expected } }
        // 자유도 14 × 13 = 182, 유의수준 0.1% 임계값 ≈ 242
        assertTrue("χ² = $chi (공정하다면 182 근처)", chi < 242)
    }

    @Test
    fun raceAlwaysEndsWithEveryoneFinishedInOrder() {
        val rnd = Random(7)
        repeat(500) {
            val race = Race(Animal.entries.shuffled(rnd).take(2 + rnd.nextInt(9)), rnd)
            while (!race.done) race.step(1 / 60.0)
            val times = race.ranking().map { it.finish!! }
            assertEquals(times.sorted(), times)
            assertTrue(race.racers.all { it.x == TRACK })
        }
    }
}
