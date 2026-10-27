package com.example.animalrace

import com.example.animalrace.model.Animal
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.MiniGameResult
import com.example.animalrace.model.Period
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.ourStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/** 4.2 우리 기록: 사람별 벌칙·1등·연속 당첨 */
class StatsTest {
    private val animals = listOf(Animal.HORSE, Animal.CAT, Animal.PIG, Animal.RABBIT)
    private fun day(d: Int) = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, d, 12, 0, 0) }.timeInMillis
    /** 사람 이름을 1등부터 순서대로 */
    private fun race(at: Long, vararg order: String?, picks: List<Int>? = null, mini: MiniGameResult? = null) =
        RaceRecord(at, "커피", 0, order.mapIndexed { i, w -> RaceResult(animals[i], 30.0 + i, 0, i, w) }, picks = picks, miniGame = mini)

    private val now = day(28) // 2026-10-28 수요일

    @Test
    fun countsPenaltiesWinsAndGamesPerPerson() {
        val h = listOf(
            race(day(28), "지은", "민수", "서연"),
            race(day(27), "민수", "지은", "서연"),
            race(day(26), "서연", "민수", "지은"),
        )
        val s = ourStats(h, Period.ALL, now = now)
        assertEquals(3, s.games)
        assertEquals("서연", s.penaltyKing!!.who)
        assertEquals(2, s.penaltyKing!!.penalties)
        assertEquals(3, s.people.first { it.who == "민수" }.games)
        assertEquals(0, s.people.first { it.who == "민수" }.penalties)
        assertEquals("민수", s.lucky!!.who)
        assertEquals(1, s.winKing!!.wins)
        assertEquals("서연" to 2, s.streak)
    }

    @Test
    fun miniGameLoserIsTheOnlyPenaltyAndPicksCountEachPerson() {
        val picks = race(day(28), "지은", "민수", "서연", picks = listOf(1, 3))
        val mini = picks.copy(miniGame = MiniGameResult(MiniGame.LOTTO, Animal.HORSE, "지은"))
        assertEquals(1, ourStats(listOf(picks), Period.ALL, now = now).people.first { it.who == "지은" }.penalties)
        assertEquals(1, ourStats(listOf(picks), Period.ALL, now = now).people.first { it.who == "서연" }.penalties)
        val s = ourStats(listOf(mini), Period.ALL, now = now)
        assertEquals(0, s.people.first { it.who == "서연" }.penalties)
        assertEquals(listOf("지은"), s.recent.single().second)
    }

    @Test
    fun periodGroupAndUnnamedFilters() {
        val h = listOf(
            race(day(27), "지은", "민수"),               // 이번 주 (월 26일부터)
            race(day(20), "아빠", "엄마"),               // 지난주, 이번 달
            race(day(3) - 40L * 24 * 3600 * 1000, "지은", "민수"), // 지난달
            race(day(27), null, null),                   // 이름 없는 판은 안 셈
        )
        assertEquals(1, ourStats(h, Period.WEEK, now = now).games)
        assertEquals(2, ourStats(h, Period.MONTH, now = now).games)
        assertEquals(3, ourStats(h, Period.ALL, now = now).games)
        assertEquals(1, ourStats(h, Period.ALL, listOf("아빠", "엄마", "도윤"), now).games)
        assertNull(ourStats(h, Period.ALL, now = now).streak)
    }
}
