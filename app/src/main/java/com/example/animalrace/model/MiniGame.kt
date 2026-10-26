package com.example.animalrace.model

import kotlin.random.Random

/** 몰아주기 미니게임 종류. */
enum class MiniGame(val emoji: String, val title: String, val desc: String) {
    LADDER("🪜", "사다리 타기", "동물들이 직접 타고 내려가요"),
    LOTTO("🎫", "복권 긁기", "돌려 가며 손가락으로 슥슥"),
    GACHA("🎰", "가챠 캡슐", "레버 돌리고 톡톡 깨기"),
}

/** 몰아주기 결과: 어떤 게임으로 누가 몰빵했는지. */
data class MiniGameResult(val game: MiniGame, val loser: Animal)

/**
 * 몰아주기: 랜덤 등수로 2명 이상 걸렸을 때 그중 한 명에게 몰빵.
 * 모든 게임이 시작할 때 몰빵을 한 자리에 고르게 숨겨 둔다
 * (사다리: 아래 칸 하나, 복권: 한 장, 가챠: 몇 번째로 나오는 캡슐).
 * 숨긴 자리는 끝까지 가려져 있으므로, 사람들이 줄을 어디에 긋든 몇 번째로 고르든 걸릴 확률은 같다.
 */
object MiniGameDraw {
    /** 몰빵이 숨은 자리 (사다리 아래 칸 / 복권 / 가챠 몇 번째 캡슐) */
    fun bomb(places: Int, rnd: Random = Random.Default): Int = rnd.nextInt(places)

    /** [pressed]번째 캡슐은 출전 순서상 몇 번째 사람 차례인지 */
    fun turn(pressed: Int, players: Int): Int = pressed % players

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

    /** [r]을 더 그을 수 있는지: 같은 높이에 같은 자리나 맞붙는 가로줄이 있으면 사다리가 꼬여서 안 된다. */
    fun canAdd(r: Rung): Boolean =
        r.level in 0 until levels && r.left in 0 until cols - 1 &&
            rungs.none { it.level == r.level && kotlin.math.abs(it.left - r.left) <= 1 }

    operator fun plus(r: Rung): Ladder = Ladder(cols, levels, rungs + r)

    companion object {
        const val LEVELS = 13

        fun random(cols: Int, rnd: Random = Random.Default): Ladder {
            var tries = 0
            while (true) {
                tries++
                val rungs = mutableListOf<Rung>()
                for (level in 0 until LEVELS) {
                    if (rnd.nextDouble() < 0.45) continue
                    // 한 높이에 여러 줄도 되지만 맞붙지는 않게 (맞붙으면 사다리가 꼬인다)
                    val lefts = (0 until cols - 1).shuffled(rnd)
                    for ((k, left) in lefts.withIndex()) {
                        if (k > 0 && rnd.nextDouble() < 0.6) continue
                        if (rungs.none { it.level == level && kotlin.math.abs(it.left - left) <= 1 }) rungs += Rung(level, left)
                    }
                }
                // 너무 밋밋하지 않게 세로줄마다 가로줄이 두 개 이상 닿도록 (오래 안 나오면 하나로 완화),
                // 그리고 사람마다 한 줄씩 그을 자리가 넉넉하게 남도록
                val need = if (tries < 500) 2 else 1
                val touches = (0 until cols).all { c -> rungs.count { it.left == c || it.left + 1 == c } >= need }
                val ladder = Ladder(cols, LEVELS, rungs)
                val spots = (0 until LEVELS).sumOf { l -> (0 until cols - 1).count { c -> ladder.canAdd(Rung(l, c)) } }
                if (touches && spots >= cols + 2) return ladder
            }
        }
    }
}
