package com.example.animalrace

import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.SavedSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 5.5 사람 먼저: 참가자 이름만 넣고, 이름표를 동물에게 끌어다 놓기 */
class NameTagTest {
    private class MemoryRepo : AppRepository {
        var setup: SavedSetup? = null
        override fun loadSetup() = setup
        override fun saveSetup(setup: SavedSetup) { this.setup = setup }
        override fun loadHistory() = emptyList<RaceRecord>()
        override fun saveHistory(history: List<RaceRecord>) {}
    }

    private fun state(vararg people: String) = AppState(MemoryRepo()).apply { setPeople(people.toList()) }

    @Test
    fun peopleWaitUntilTheirTagIsPlaced() {
        val s = state("지은", "민수", " 지은 ", "")
        assertEquals(listOf("지은", "민수"), s.waiting.toList())
        assertTrue(s.placeName("민수", Animal.PANDA))
        assertEquals(listOf(Animal.PANDA), s.selected.toList())
        assertEquals("민수", s.names[Animal.PANDA])
        assertEquals(listOf("지은"), s.waiting.toList())
    }

    @Test
    fun movingATagKeepsTheLane() {
        val s = state("지은", "민수")
        s.placeName("지은", Animal.PANDA); s.placeName("민수", Animal.CAT)
        s.placeName("지은", Animal.HAMSTER)
        assertEquals(listOf(Animal.HAMSTER, Animal.CAT), s.selected.toList())
        assertEquals("지은", s.names[Animal.HAMSTER])
        assertFalse(Animal.PANDA in s.names)
    }

    @Test
    fun droppingOnAnotherTagSwapsOrSendsItBack() {
        val s = state("지은", "민수", "현우")
        s.placeName("지은", Animal.PANDA); s.placeName("민수", Animal.CAT)
        // 붙어 있던 이름표끼리는 서로 바꾸기
        s.placeName("지은", Animal.CAT)
        assertEquals("지은", s.names[Animal.CAT]); assertEquals("민수", s.names[Animal.PANDA])
        // 기다리던 이름표를 놓으면 원래 주인은 다시 줄로
        s.placeName("현우", Animal.CAT)
        assertEquals("현우", s.names[Animal.CAT])
        assertEquals(listOf("지은"), s.waiting.toList())
    }

    @Test
    fun unplacingAndTappingSendPeopleBack() {
        val s = state("지은", "민수")
        s.placeName("지은", Animal.PANDA); s.placeName("민수", Animal.CAT)
        s.unplaceName("지은")
        assertEquals(listOf(Animal.CAT), s.selected.toList())
        s.tapAnimal(Animal.CAT)
        assertTrue(s.selected.isEmpty())
        assertEquals(listOf("지은", "민수"), s.waiting.toList())
    }

    @Test
    fun canStartWithoutEveryoneNamed() {
        val s = state("지은", "민수", "현우")
        s.placeName("지은", Animal.PANDA)
        s.tapAnimal(Animal.CAT)
        assertTrue(s.canStart)
    }

    @Test
    fun removedPeopleLeaveTheirAnimals() {
        val s = state("지은", "민수")
        s.placeName("지은", Animal.PANDA); s.placeName("민수", Animal.CAT)
        s.setPeople(listOf("민수", "서연"))
        assertEquals(listOf(Animal.CAT), s.selected.toList())
        assertEquals(listOf("서연"), s.waiting.toList())
    }
}
