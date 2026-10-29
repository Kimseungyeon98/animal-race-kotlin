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
    fun addedRungsNeverTangleTheLadder() {
        val rnd = Random(13)
        for (cols in 2..3) repeat(500) {
            var ladder = Ladder.random(cols, rnd)
            // 사람마다 아무 데나 탭: 그을 수 있는 자리면 긋기
            repeat(cols) {
                val r = Rung(rnd.nextInt(ladder.levels), rnd.nextInt(cols - 1))
                if (ladder.canAdd(r)) ladder += r
            }
            assertEquals((0 until cols).toSet(), (0 until cols).map { ladder.end(it) }.toSet())
            // 같은 높이에 맞붙은 줄이 없어야 함
            ladder.rungs.groupBy { it.level }.values.forEach { same ->
                same.sortedBy { it.left }.zipWithNext().forEach { (a, b) -> assertTrue(b.left - a.left >= 2) }
            }
        }
    }

    @Test
    fun ladderIsFairWhereverPeopleDrawLines() {
        // 몰빵 칸은 고르게 숨기고, 사람들은 (보이는 것만 보고) 아무 데나 또는 항상 왼쪽 위에 줄을 긋는다
        val rnd = Random(20261024)
        for (players in 2..3) for (strategy in 0..1) {
            val draws = 30_000
            val counts = IntArray(players)
            repeat(draws) {
                val bomb = MiniGameDraw.bomb(players, rnd)
                var ladder = Ladder.random(players, rnd)
                repeat(players) {
                    val r = if (strategy == 0) Rung(rnd.nextInt(ladder.levels), rnd.nextInt(players - 1))
                    else (0 until ladder.levels).map { Rung(it, 0) }.firstOrNull { ladder.canAdd(it) } ?: Rung(0, 0)
                    if (ladder.canAdd(r)) ladder += r
                }
                counts[(0 until players).first { ladder.end(it) == bomb }]++
            }
            val expected = draws.toDouble() / players
            val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
            // 자유도 1~2, 유의수준 0.1% 임계값 10.8 / 13.8
            assertTrue("${players}명 전략${strategy} χ² = $chi, counts = ${counts.toList()}", chi < if (players == 2) 10.8 else 13.8)
        }
    }

    @Test
    fun lottoIsFairWhateverTheOrderAndPicks() {
        // 몰빵 복권 자리는 고르게, 사람들은 차례로 남은 장 중 하나를 고름 (항상 왼쪽부터 / 아무거나 두 가지 전략)
        val rnd = Random(20261025)
        for (players in 2..3) for (strategy in 0..1) {
            val draws = 30_000
            val counts = IntArray(players)
            repeat(draws) {
                val bomb = MiniGameDraw.bomb(players, rnd)
                val left = (0 until players).toMutableList()
                for (turn in 0 until players) {
                    val pick = if (strategy == 0) left.first() else left.random(rnd)
                    left.remove(pick)
                    if (pick == bomb) { counts[turn]++; break }
                }
            }
            val expected = draws.toDouble() / players
            val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
            assertTrue("${players}명 전략${strategy} χ² = $chi, counts = ${counts.toList()}", chi < if (players == 2) 10.8 else 13.8)
        }
    }

    @Test
    fun roundsKeepEveryoneEqual() {
        // 4.7: 사람 수 × 바퀴 수(4명까지 3바퀴, 5명부터 2바퀴). 몰빵이 나올 때까지 출전 번호 순으로 돌아가며 고른다
        assertEquals(6, MiniGameDraw.slots(2))
        assertEquals(12, MiniGameDraw.slots(4))
        assertEquals(10, MiniGameDraw.slots(5))
        assertEquals(18, MiniGameDraw.slots(9))
        val rnd = Random(20261029)
        for (players in listOf(2, 3, 5)) for (strategy in 0..1) {
            val tickets = MiniGameDraw.slots(players)
            val draws = 30_000
            val counts = IntArray(players)
            repeat(draws) {
                val bomb = MiniGameDraw.bomb(tickets, rnd)
                val left = (0 until tickets).toMutableList()
                for (k in 0 until tickets) {
                    val pick = if (strategy == 0) left.first() else left.random(rnd)
                    left.remove(pick)
                    if (pick == bomb) { counts[MiniGameDraw.turn(k, players)]++; break }
                }
            }
            val expected = draws.toDouble() / players
            val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
            // 자유도 n-1, 유의수준 0.001
            val limit = mapOf(2 to 10.8, 3 to 13.8, 5 to 18.5).getValue(players)
            assertTrue("${players}명 전략${strategy} χ² = $chi, counts = ${counts.toList()}", chi < limit)
        }
    }

    @Test
    fun gachaRoundsGiveEveryoneTheSameCapsules() {
        // 4.8: 캡슐 k번째는 출전 번호 순으로 돌아가며 → 사람마다 바퀴 수만큼, 몰빵 순번이 균등하면 사람도 균등
        for (n in 2..9) {
            val owners = (0 until MiniGameDraw.slots(n)).map { MiniGameDraw.turn(it, n) }
            assertEquals(List(n) { MiniGameDraw.rounds(n) }, (0 until n).map { p -> owners.count { it == p } })
        }
    }

    @Test
    fun gachaGivesEveryoneOneCapsule() {
        // k번째 캡슐은 k번째 차례 사람의 것 → 몰빵 캡슐 위치가 균등하면 사람도 균등
        for (n in 2..8) {
            val owners = (0 until n).map { MiniGameDraw.turn(it, n) }
            assertEquals((0 until n).toList(), owners)
        }
    }

    @Test(timeout = 10_000)
    fun ladderCanBeMadeForAnyNumberOfPlayers() {
        // 직접 지정으로 당첨자가 많아져도 사다리가 만들어져야 한다 (예전엔 6명부터 무한 반복)
        val rnd = Random(3)
        for (cols in 2..10) repeat(30) {
            val l = Ladder.random(cols, rnd)
            assertEquals((0 until cols).toSet(), (0 until cols).map { l.end(it) }.toSet())
            val spots = (0 until l.levels).sumOf { lv -> (0 until cols - 1).count { c -> l.canAdd(Rung(lv, c)) } }
            assertTrue("cols=$cols spots=$spots", spots >= cols + 2)
        }
    }
}
