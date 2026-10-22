package com.example.animalrace.model

import kotlin.random.Random

/** 몰아주기 미니게임 종류. */
enum class MiniGame(val emoji: String, val title: String, val desc: String) {
    LADDER("🪜", "사다리 타기", "동물들이 직접 타고 내려가요"),
    LOTTO("🎫", "복권 긁기", "돌려 가며 손가락으로 슥슥"),
}

/** 몰아주기 결과: 어떤 게임으로 누가 몰빵했는지. */
data class MiniGameResult(val game: MiniGame, val loser: Animal)

/**
 * 몰아주기: 랜덤 등수로 2명 이상 걸렸을 때 그중 한 명에게 몰빵.
 * 몰빵 1명은 게임 시작 전에 고르게 뽑아 두고, 미니게임은 그 결과를 보여 주는 연출이다.
 */
object MiniGameDraw {
    /** 몰빵할 사람(후보 목록의 위치) */
    fun loser(players: Int, rnd: Random = Random.Default): Int = rnd.nextInt(players)

    /** 게임을 고르지 않았을 때(아무거나) */
    fun game(rnd: Random = Random.Default): MiniGame = MiniGame.entries.random(rnd)
}

/** 사다리의 가로줄: [level] 높이에서 [left]번 세로줄과 그 오른쪽 세로줄을 잇는다. */
data class Rung(val level: Int, val left: Int)

/**
 * 사다리. 세로줄 [cols]개, 가로줄이 놓일 수 있는 높이 [levels]칸.
 * 같은 높이에 맞붙은 가로줄은 없으므로 출발점마다 도착점이 하나씩 다르다.
 */
class Ladder(val cols: Int, val levels: Int, val rungs: List<Rung>) {

    /** 지나가는 꺾임점들: (세로줄, 높이). 높이 -1은 맨 위, [levels]는 맨 아래. */
    fun path(start: Int): List<Pair<Int, Int>> {
        var col = start
        val pts = mutableListOf(col to -1)
        for (r in rungs.sortedBy { it.level }) {
            val next = when (col) {
                r.left -> r.left + 1
                r.left + 1 -> r.left
                else -> continue
            }
            pts += col to r.level
            col = next
            pts += col to r.level
        }
        pts += col to levels
        return pts
    }

    fun end(start: Int): Int = path(start).last().first

    companion object {
        const val LEVELS = 9

        fun random(cols: Int, rnd: Random = Random.Default): Ladder {
            while (true) {
                val rungs = mutableListOf<Rung>()
                for (level in 0 until LEVELS) {
                    if (rnd.nextDouble() < 0.25) continue
                    // 세로줄이 3개 이상이면 한 높이에 한 줄만 (맞붙지 않게)
                    rungs += Rung(level, rnd.nextInt(cols - 1))
                }
                // 너무 밋밋하지 않게 세로줄마다 가로줄이 두 개 이상 닿도록
                val touches = (0 until cols).all { c -> rungs.count { it.left == c || it.left + 1 == c } >= 2 }
                if (touches) return Ladder(cols, LEVELS, rungs)
            }
        }
    }
}
