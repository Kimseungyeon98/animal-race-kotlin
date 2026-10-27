package com.example.animalrace

import com.example.animalrace.model.Animal
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.MiniGameResult
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.ui.result.shareText
import org.junit.Assert.assertEquals
import org.junit.Test

/** 4.1 결과 공유 카드와 같이 보내는 글 */
class ShareTextTest {
    private fun rs(vararg who: Pair<Animal, String?>) = who.mapIndexed { i, (a, w) -> RaceResult(a, 30.0 + i, 0, i, w) }

    @Test
    fun lastPlaceByName() {
        val r = RaceRecord(0L, "꼴찌가 커피 쏘기", 1, rs(Animal.CAT to "지은", Animal.PIG to "서연"))
        assertEquals("🏁 동물 대소동 레이스 · 커피 쏘기 → 서연 당첨!", shareText(r))
    }

    @Test
    fun animalNameWhenNobodyNamedAndDefaultBet() {
        val r = RaceRecord(0L, "", 1, rs(Animal.CAT to null, Animal.PIG to null))
        assertEquals("🏁 동물 대소동 레이스 · 오늘의 꼴찌 → 돼지 당첨!", shareText(r))
    }

    @Test
    fun picksAndMiniGame() {
        val results = rs(Animal.CAT to "지은", Animal.HORSE to "민수", Animal.PIG to "서연")
        val picked = RaceRecord(0L, "치킨", 1, results, picks = listOf(1, 3))
        assertEquals("🏁 동물 대소동 레이스 · 치킨 → 지은·서연 당첨!", shareText(picked))
        val mini = picked.copy(miniGame = MiniGameResult(MiniGame.LOTTO, Animal.CAT, "지은"))
        assertEquals("🏁 동물 대소동 레이스 · 치킨 → 지은 당첨!", shareText(mini))
    }
}
