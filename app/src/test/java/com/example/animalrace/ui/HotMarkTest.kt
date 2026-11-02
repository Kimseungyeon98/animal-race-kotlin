package com.example.animalrace.ui

import com.example.animalrace.ui.race.HotMark
import com.example.animalrace.ui.race.hotMark
import org.junit.Assert.assertEquals
import org.junit.Test

/** 5.4 머리 위 표시: 당첨 자리 폭탄, 당첨이 절반을 넘으면 안전한 자리 클로버. */
class HotMarkTest {

    private fun marks(players: Int, picks: List<Int>?) = (0 until players).map { hotMark(it, players, picks) }

    @Test
    fun lastPlaceRaceBombsTheLastPlace() {
        assertEquals(listOf(null, null, null, HotMark.BOMB), marks(4, null))
    }

    @Test
    fun fewPicksGetBombs() {
        assertEquals(listOf(null, HotMark.BOMB, null, null, HotMark.BOMB, null), marks(6, listOf(2, 5)))
        // 딱 절반까지는 폭탄
        assertEquals(3, marks(6, listOf(1, 3, 5)).count { it == HotMark.BOMB })
    }

    @Test
    fun manyPicksMarkTheSafeSpotsInstead() {
        val m = marks(10, (1..8).toList())
        assertEquals(List(8) { null } + listOf(HotMark.CLOVER, HotMark.CLOVER), m)
    }
}
