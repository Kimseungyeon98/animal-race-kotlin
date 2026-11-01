package com.example.animalrace.ui

import com.example.animalrace.model.MiniGameDraw
import com.example.animalrace.ui.minigame.gaugeAt
import com.example.animalrace.ui.minigame.sliceAt
import com.example.animalrace.ui.minigame.spinMillis
import com.example.animalrace.ui.minigame.spinTarget
import com.example.animalrace.ui.minigame.spinTurns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 5.1 룰렛: 힘은 도는 바퀴 수·시간만 바꾸고, 바늘은 늘 숨겨 둔 몰빵 칸에 멈춘다. */
class RouletteTest {

    @Test
    fun alwaysStopsOnTheHiddenSlice() {
        val rnd = Random(51)
        for (players in 2..10) repeat(2_000) {
            val bomb = MiniGameDraw.bomb(players, rnd)
            // 이전 판에서 아무 각도로 멈춰 있어도, 힘이 얼마든
            val from = rnd.nextFloat() * 3600f
            val power = rnd.nextFloat()
            val to = spinTarget(from, bomb, players, power, 0.15f + rnd.nextFloat() * 0.7f)
            assertEquals("players=$players from=$from power=$power", bomb, sliceAt(to, players))
            // 최소 바퀴 수 이상은 앞으로 돈다
            assertTrue(to - from >= spinTurns(power) * 360f)
        }
    }

    @Test
    fun powerChangesOnlyHowLongItSpins() {
        // 같은 판(같은 몰빵 칸)이면 살살 돌리든 세게 돌리든 멈추는 칸은 같다
        for (power in listOf(0f, 0.2f, 0.5f, 0.8f, 1f)) {
            assertEquals(3, sliceAt(spinTarget(123f, 3, 6, power, 0.5f), 6))
        }
        assertTrue(spinTurns(1f) > spinTurns(0f))
        assertTrue(spinMillis(1f) > spinMillis(0f))
    }

    @Test
    fun hiddenSliceIsFairForEveryone() {
        val rnd = Random(20261101)
        for (players in listOf(2, 3, 5)) {
            val draws = 30_000
            val counts = IntArray(players)
            repeat(draws) {
                val bomb = MiniGameDraw.bomb(players, rnd)
                counts[sliceAt(spinTarget(rnd.nextFloat() * 360f, bomb, players, rnd.nextFloat(), 0.5f), players)]++
            }
            val expected = draws.toDouble() / players
            val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
            val limit = mapOf(2 to 10.8, 3 to 13.8, 5 to 18.5).getValue(players)
            assertTrue("${players}명 χ² = $chi, counts = ${counts.toList()}", chi < limit)
        }
    }

    @Test
    fun gaugeGoesUpAndDown() {
        assertEquals(0f, gaugeAt(0f), 1e-4f)
        assertEquals(1f, gaugeAt(0.6f), 1e-4f)
        assertEquals(0f, gaugeAt(1.2f), 1e-4f)
        for (k in 0..100) assertTrue(gaugeAt(k * 0.037f) in 0f..1f)
    }
}
