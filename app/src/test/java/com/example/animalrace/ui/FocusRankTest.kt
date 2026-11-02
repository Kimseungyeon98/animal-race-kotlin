package com.example.animalrace.ui

import com.example.animalrace.model.Animal
import com.example.animalrace.model.Fx
import com.example.animalrace.race.RaceView
import com.example.animalrace.race.RacerView
import com.example.animalrace.ui.race.focusRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 5.3 카메라: 막판 전엔 선두, 그 뒤엔 아직 안 정해진 다음 당첨 등수 (꼴찌 판은 꼴찌). */
class FocusRankTest {

    /** [xs]: 레인별 위치, 위치 순으로 등수. 결승선(1000m)을 넘으면 골인 */
    private fun view(vararg xs: Double) = RaceView(
        0.0,
        xs.mapIndexed { lane, x ->
            RacerView(lane, Animal.entries[lane], x, xs.count { it > x }, Fx.NONE, 0.0, false, false, x >= 1000.0, if (x >= 1000.0) 10.0 else null)
        },
        emptyList(), 0, false,
    )

    @Test
    fun followsTheLeaderUntilTheHomeStretch() {
        assertNull(focusRank(view(700.0, 650.0, 600.0, 500.0), listOf(3)))
        assertNull(focusRank(view(700.0, 650.0, 600.0, 500.0), null))
    }

    @Test
    fun thenTheNextPickThatIsStillOpen() {
        assertEquals(3, focusRank(view(820.0, 790.0, 760.0, 700.0, 680.0), listOf(3, 5)))
        // 3등이 들어오면 다음 당첨 등수 5등
        assertEquals(5, focusRank(view(1010.0, 1005.0, 1001.0, 900.0, 880.0), listOf(3, 5)))
        // 당첨 등수가 다 정해지면 다시 선두 무리
        assertNull(focusRank(view(1010.0, 1005.0, 1001.0, 1000.5, 1000.2), listOf(3, 5)))
    }

    @Test
    fun lastPlaceRaceWatchesTheLastPlaceFight() {
        assertEquals(4, focusRank(view(850.0, 790.0, 760.0, 700.0), null))
    }
}
