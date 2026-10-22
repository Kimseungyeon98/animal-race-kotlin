package com.example.animalrace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 몰아주기: 사다리 규칙과 몰빵 뽑기의 공정성. */
class MiniGameTest {

    @Test
    fun everyStartEndsInADifferentSlot() {
        val rnd = Random(11)
        for (cols in 2..3) repeat(500) {
            val ladder = Ladder.random(cols, rnd)
            assertEquals((0 until cols).toSet(), (0 until cols).map { ladder.end(it) }.toSet())
        }
    }

    @Test
    fun pathsMoveOnlyAlongPostsAndRungs() {
        val rnd = Random(12)
        repeat(300) {
            val ladder = Ladder.random(3, rnd)
            for (start in 0 until 3) {
                val path = ladder.path(start)
                assertEquals(start to -1, path.first())
                assertEquals(ladder.levels, path.last().second)
                path.zipWithNext().forEach { (a, b) ->
                    // 같은 세로줄을 따라 내려가거나, 같은 높이의 가로줄로 옆 세로줄로
                    val down = a.first == b.first && b.second >= a.second
                    val across = a.second == b.second && kotlin.math.abs(a.first - b.first) == 1 &&
                        Rung(a.second, minOf(a.first, b.first)) in ladder.rungs
                    assertTrue("$a → $b", down || across)
                }
            }
        }
    }

    @Test
    fun everyPlayerIsEquallyLikelyToPayItAll() {
        val rnd = Random(20261024)
        for (players in 2..3) {
            val draws = 30_000
            val counts = IntArray(players)
            repeat(draws) { counts[MiniGameDraw.loser(players, rnd)]++ }
            val expected = draws.toDouble() / players
            val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
            // 자유도 1~2, 유의수준 0.1% 임계값 10.8 / 13.8
            assertTrue("χ² = $chi, counts = ${counts.toList()}", chi < if (players == 2) 10.8 else 13.8)
        }
    }
}
