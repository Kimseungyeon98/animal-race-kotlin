package com.example.animalrace.race

import com.example.animalrace.model.Animal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
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

    /**
     * 큰 움직임은 눈에 보이는 스킬 구간에서만 일어나야 한다.
     * 스킬이 아닌 구간에서는 기준 속도의 PACE_MIN~PACE_MAX 배 안에서만 달리고(방해받은 뒤 이유 없이 훅 따라잡지 않음),
     * 멈춤(방해·낮잠) 구간에서는 정말 멈춰 있어야 한다.
     */
    @Test
    fun bigMovesOnlyHappenDuringVisibleSkills() {
        val rnd = Random(11)
        val dt = 1 / 60.0
        repeat(400) {
            val race = Race(Animal.entries.shuffled(rnd).take(2 + rnd.nextInt(13)), rnd)
            val prev = DoubleArray(race.racers.size)
            while (!race.done) {
                race.racers.forEachIndexed { i, r -> prev[i] = r.x }
                race.step(dt)
                race.racers.forEachIndexed { i, r ->
                    if (r.finished || race.t < 1.6) return@forEachIndexed
                    val pace = (r.x - prev[i]) / dt / race.baseSpeed(r, race.t)
                    if (!race.inSkill(r, race.t) && !race.inSkill(r, race.t - dt)) {
                        assertTrue("${r.animal} t=${race.t} pace=$pace", pace in (PACE_MIN - 0.02)..(PACE_MAX + 0.02))
                    }
                    if (r.motionAt(race.t) == 0.0 && r.motionAt(race.t - dt) == 0.0) {
                        assertTrue("${r.animal} 멈춤 중 이동 t=${race.t}", abs(pace) < 0.02)
                    }
                }
            }
        }
    }
}
