package com.example.animalrace

import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.SavedSetup
import org.junit.Assert.assertEquals
import org.junit.Test

/** 4.10 경기 기록 한 판씩 지우기 · 되돌리기 */
class HistoryDeleteTest {
    private class MemoryRepo : AppRepository {
        var saved = emptyList<RaceRecord>()
        override fun loadSetup(): SavedSetup? = null
        override fun saveSetup(setup: SavedSetup) {}
        override fun loadHistory() = saved
        override fun saveHistory(history: List<RaceRecord>) { saved = history }
    }

    private fun record(at: Long) = RaceRecord(at, "커피", 0, listOf(RaceResult(Animal.CAT, 30.0, 0), RaceResult(Animal.PIG, 31.0, 0, 1)))

    @Test
    fun deleteOneAndRestoreToTheSamePlace() {
        val repo = MemoryRepo().apply { saved = listOf(record(3), record(2), record(1)) }
        val s = AppState(repo)
        val i = s.deleteRecord(record(2))
        assertEquals(1, i)
        assertEquals(listOf(3L, 1L), s.history.map { it.at })
        assertEquals(listOf(3L, 1L), repo.saved.map { it.at })
        s.restoreRecord(i, record(2))
        assertEquals(listOf(3L, 2L, 1L), s.history.map { it.at })
        assertEquals(listOf(3L, 2L, 1L), repo.saved.map { it.at })
        // 이미 있으면 두 번 넣지 않고, 없는 판을 지우면 -1
        s.restoreRecord(0, record(2))
        assertEquals(3, s.history.size)
        assertEquals(-1, s.deleteRecord(record(9)))
    }
}
