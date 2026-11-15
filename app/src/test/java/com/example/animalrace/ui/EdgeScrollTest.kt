package com.example.animalrace.ui

import com.example.animalrace.ui.setup.EdgeScroll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 6.1 이름표 끌기 자동 스크롤: 목록 밖에서 들어올 땐 가만히, 안쪽에 들어온 뒤 가장자리로 가면 스크롤 */
class EdgeScrollTest {
    private val top = 400f
    private val bottom = 1000f
    private fun scroll() = EdgeScroll(edge = 100f)

    @Test
    fun enteringFromAboveDoesNotScroll() {
        val s = scroll()
        // 위쪽 이름표 줄(목록 밖) → 위 가장자리 띠를 지나 안쪽으로
        for (y in listOf(250f, 320f, 390f, 410f, 460f, 499f)) assertEquals("y=$y", 0f, s.speed(y, top, bottom))
        assertEquals(0f, s.speed(600f, top, bottom))
    }

    @Test
    fun scrollsUpAfterBeingInside() {
        val s = scroll()
        s.speed(350f, top, bottom)
        s.speed(600f, top, bottom) // 안쪽에 들어옴
        assertTrue(s.speed(450f, top, bottom) < 0f)
        assertEquals(-1f, s.speed(380f, top, bottom))
    }

    @Test
    fun scrollsDownAfterComingFromAbove() {
        val s = scroll()
        s.speed(350f, top, bottom)
        s.speed(600f, top, bottom)
        assertTrue(s.speed(950f, top, bottom) > 0f)
        assertEquals(1f, s.speed(1050f, top, bottom))
        // 너무 멀리 벗어나면(아래 버튼 쪽) 멈춤
        assertEquals(0f, s.speed(1200f, top, bottom))
    }

    @Test
    fun startingInTopBandWaitsUntilInside() {
        val s = scroll()
        // 맨 윗줄 동물에 붙은 이름표를 집었을 때: 바로 위로 말려 올라가지 않는다
        assertEquals(0f, s.speed(430f, top, bottom))
        s.speed(520f, top, bottom)
        assertTrue(s.speed(430f, top, bottom) < 0f)
    }
}
