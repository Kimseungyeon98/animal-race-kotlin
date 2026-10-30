package com.example.animalrace.ui

import com.example.animalrace.model.MiniGameDraw
import com.example.animalrace.ui.minigame.FreeTurns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 4.9 미니게임 순서 자유: 이름 누르고 하기, 한 바퀴에 한 사람 한 번 */
class FreeTurnsTest {
    @Test
    fun everyoneOncePerRoundAndLastOneGoesAutomatically() {
        val t = FreeTurns(listOf(0, 1, 2))
        assertNull(t.current)
        t.picked = 2; assertEquals(2, t.current)
        t.finish(2)
        assertEquals(listOf(0, 1), t.left)
        t.picked = 0; t.finish(0)
        // 한 명만 남으면 이름을 안 눌러도 그 사람 차례
        assertEquals(1, t.current)
        t.finish(1)
        assertEquals(1, t.round)
        assertEquals(listOf(0, 1, 2), t.left)
        assertNull(t.current)
    }

    @Test
    fun anyOrderIsStillFair() {
        // 복권: 매번 남은 사람 중 아무나 먼저 하고, 아무 장이나 고름. 몰빵이 나오면 그 사람이 몰빵
        // (몰빵을 찾을 때까지 지난 결과를 보고 순서를 정해도 걸릴 확률은 모두 같아야 한다)
        val rnd = Random(20261030)
        for (players in listOf(2, 3, 5)) {
            val slots = MiniGameDraw.slots(players)
            val draws = 30_000
            val counts = IntArray(players)
            repeat(draws) {
                val bomb = MiniGameDraw.bomb(slots, rnd)
                val turns = FreeTurns((0 until players).toList())
                val left = (0 until slots).toMutableList()
                while (true) {
                    val p = turns.current ?: turns.left.random(rnd).also { turns.picked = it }
                    val pick = left.random(rnd); left.remove(pick)
                    if (pick == bomb) { counts[p]++; break }
                    turns.finish(p)
                }
            }
            val expected = draws.toDouble() / players
            val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
            val limit = mapOf(2 to 10.8, 3 to 13.8, 5 to 18.5).getValue(players)
            assertTrue("${players}명 χ² = $chi, counts = ${counts.toList()}", chi < limit)
        }
    }
}
