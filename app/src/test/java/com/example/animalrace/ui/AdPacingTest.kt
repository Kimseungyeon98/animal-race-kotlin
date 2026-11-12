package com.example.animalrace.ui

import com.example.animalrace.ui.ads.AdPacing
import org.junit.Assert.assertEquals
import org.junit.Test

/** 6.0 전면 광고: 결과에서 넘어가는 3번째마다 한 번 */
class AdPacingTest {
    @Test
    fun everyThirdLeave() {
        assertEquals(listOf(3, 6, 9), (0..10).filter { AdPacing.due(it) })
    }
}
