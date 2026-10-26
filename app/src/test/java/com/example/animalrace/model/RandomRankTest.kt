package com.example.animalrace.model

import com.example.animalrace.race.Race
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 랜덤 등수 모드: 인원 계산, 뽑기 규칙, 공정성. */
class RandomRankTest {

    @Test
    fun winnerCountIsAThirdRoundedDownButAtLeastOne() {
        val expected = mapOf(2 to 1, 3 to 1, 4 to 1, 5 to 1, 6 to 2, 7 to 2, 8 to 2, 9 to 3, 10 to 3)
        expected.forEach { (n, k) -> assertEquals("참가 $n", k, RandomRank.count(n)) }
    }

    @Test
    fun picksAreDistinctSortedAndInRange() {
        val rnd = Random(7)
        for (n in 2..10) repeat(500) {
            val picks = RandomRank.draw(n, rnd)
            assertEquals(RandomRank.count(n), picks.size)
            assertEquals(picks.sorted(), picks)
            assertEquals(picks.size, picks.toSet().size)
            assertTrue(picks.all { it in 1..n })
        }
    }

    @Test
    fun everyPlaceIsEquallyLikelyToBePicked() {
        val rnd = Random(20261023)
        val n = 8
        val draws = 40_000
        val counts = IntArray(n)
        repeat(draws) { RandomRank.draw(n, rnd).forEach { counts[it - 1]++ } }
        val expected = draws.toDouble() * RandomRank.count(n) / n
        val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
        // 자유도 7, 유의수준 0.1% 임계값 24.3
        assertTrue("χ² = $chi, counts = ${counts.toList()}", chi < 24.3)
    }

    @Test
    fun drawingPicksNeverChangesTheRace() {
        val animals = Animal.entries.take(6)
        fun finishOrder(seed: Int): List<Animal> {
            val race = Race(animals, Random(seed))
            while (!race.done) race.step(1 / 60.0)
            return race.ranking().map { it.animal }
        }
        repeat(20) { seed ->
            val plain = finishOrder(seed)
            RandomRank.draw(animals.size, Random(seed * 31 + 1))
            assertEquals(plain, finishOrder(seed))
        }
    }

    @Test
    fun losersFollowThePicks() {
        val results = Animal.entries.take(6).mapIndexed { i, a -> RaceResult(a, 10.0 + i, 0, i) }
        val random = RaceRecord(0, "커피 쏘기", 0, results, picks = listOf(3, 6))
        assertEquals(listOf(results[2], results[5]), random.losers)
        val normal = RaceRecord(0, "커피 쏘기", 0, results)
        assertEquals(listOf(results.last()), normal.losers)
    }

    @Test
    fun manualCountStaysBetweenOneAndAllButOne() {
        for (n in 2..10) {
            assertEquals(RandomRank.count(n), RandomRank.count(n, RankSetting()))
            assertEquals(1, RandomRank.count(n, RankSetting(count = 0)))
            assertEquals(n - 1, RandomRank.count(n, RankSetting(count = 99)))
            val picks = RandomRank.picks(n, RankSetting(count = n - 1), Random(n))
            assertEquals(n - 1, picks.size)
            assertEquals(picks.size, picks.toSet().size)
        }
    }

    @Test
    fun fixedPicksAreKeptSortedAndClippedToRacers() {
        val s = RankSetting(RankMode.FIXED, fixed = listOf(5, 2, 9, 2))
        assertEquals(listOf(2, 5), RandomRank.picks(6, s))
        assertEquals(listOf(2, 5, 9), RandomRank.picks(10, s))
        assertTrue(RandomRank.ready(6, s))
        assertTrue(!RandomRank.ready(4, RankSetting(RankMode.FIXED, fixed = listOf(5, 6))))
        assertTrue(RandomRank.ready(4, RankSetting()))
    }

    @Test
    fun everyAnimalIsEquallyLikelyToLandOnAFixedRank() {
        // 직접 정한 등수(예: 2등)에 어느 동물이 들어올지는 경주가 정한다 → 모두 같은 확률
        val animals = Animal.entries.take(5)
        val fixedRank = 2
        val counts = IntArray(animals.size)
        val runs = 5_000
        repeat(runs) { seed ->
            val race = Race(animals, Random(seed))
            while (!race.done) race.step(1 / 60.0)
            counts[animals.indexOf(race.ranking()[fixedRank - 1].animal)]++
        }
        val expected = runs.toDouble() / animals.size
        val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
        // 자유도 4, 유의수준 0.1% 임계값 18.5
        assertTrue("χ² = $chi, counts = ${counts.toList()}", chi < 18.5)
    }
}
