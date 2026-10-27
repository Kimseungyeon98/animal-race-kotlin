package com.example.animalrace

import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.SavedSetup
import com.example.animalrace.race.Race
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 4.0 사람이 주인공: 이름 붙이기·최근 멤버·멤버 묶음 */
class PeopleTest {
    private class MemoryRepo : AppRepository {
        var setup: SavedSetup? = null
        override fun loadSetup() = setup
        override fun saveSetup(setup: SavedSetup) { this.setup = setup }
        override fun loadHistory() = emptyList<RaceRecord>()
        override fun saveHistory(history: List<RaceRecord>) {}
    }

    private fun state(vararg animals: Animal) = AppState(MemoryRepo()).apply { animals.forEach { toggle(it) } }

    @Test
    fun namesAreTrimmedShortenedAndOnlyForSelectedAnimals() {
        val s = state(Animal.HORSE, Animal.CAT)
        s.updateNames(mapOf(Animal.HORSE to "  민수  ", Animal.CAT to "아주아주긴이름입니다", Animal.PIG to "서연"))
        assertEquals("민수", s.names[Animal.HORSE])
        assertEquals(AppState.NAME_MAX, s.names[Animal.CAT]!!.length)
        assertNull(s.names[Animal.PIG])
    }

    @Test
    fun blankNameMeansAnimalName() {
        val s = state(Animal.HORSE, Animal.CAT)
        s.updateNames(mapOf(Animal.HORSE to "   "))
        assertTrue(s.names.isEmpty())
        assertEquals("말", RaceResult(Animal.HORSE, 1.0, 0).name)
        assertEquals("민수", RaceResult(Animal.HORSE, 1.0, 0, who = "민수").name)
    }

    @Test
    fun recentMembersPutNewNamesFirstWithoutDuplicates() {
        val s = state(Animal.HORSE, Animal.CAT)
        s.updateNames(mapOf(Animal.HORSE to "민수", Animal.CAT to "지은"))
        s.updateNames(mapOf(Animal.HORSE to "서연", Animal.CAT to "민수"))
        assertEquals(listOf("서연", "민수", "지은"), s.members.toList())
    }

    @Test
    fun removingAnAnimalRemovesItsName() {
        val s = state(Animal.HORSE, Animal.CAT, Animal.PIG)
        s.updateNames(mapOf(Animal.HORSE to "민수", Animal.CAT to "지은"))
        s.toggle(Animal.HORSE)
        assertFalse(Animal.HORSE in s.names)
        s.clearSelection()
        assertTrue(s.names.isEmpty())
    }

    @Test
    fun randomPickKeepsThePeople() {
        val s = state(Animal.HORSE, Animal.CAT, Animal.PIG)
        s.updateNames(mapOf(Animal.HORSE to "민수", Animal.CAT to "지은", Animal.PIG to "서연"))
        s.pickRandom(4)
        assertEquals(setOf("민수", "지은", "서연"), s.names.values.toSet())
        assertTrue(s.names.keys.all { it in s.selected })
    }

    @Test
    fun loadingAGroupFillsRacersAndGroupsNeedTwoDistinctPeople() {
        val s = state(Animal.HORSE)
        s.ensureRacers(4)
        assertEquals(4, s.selected.size)
        s.saveGroup(listOf("민수"))
        assertTrue(s.groups.isEmpty())
        s.saveGroup(listOf("민수", "지은", "민수"))
        assertEquals(listOf(listOf("민수", "지은")), s.groups.toList())
        s.saveGroup(listOf("민수", "지은"))
        assertEquals(1, s.groups.size)
    }

    @Test
    fun membersAndGroupsSurviveRestartButNamesDoNot() {
        val repo = MemoryRepo()
        AppState(repo).apply {
            toggle(Animal.HORSE); toggle(Animal.CAT)
            updateNames(mapOf(Animal.HORSE to "민수", Animal.CAT to "지은"))
            saveGroup(listOf("민수", "지은"))
        }
        val again = AppState(repo)
        assertEquals(listOf("민수", "지은"), again.members.toList())
        assertEquals(listOf(listOf("민수", "지은")), again.groups.toList())
        assertTrue(again.names.isEmpty())
    }

    @Test
    fun namesNeverChangeTheRaceResult() {
        // 이름은 중계 문장에만 쓰인다: 같은 난수면 이름이 있든 없든 순위·기록이 똑같다
        val animals = listOf(Animal.HORSE, Animal.CAT, Animal.PIG, Animal.RABBIT)
        repeat(50) { seed ->
            val plain = Race(animals, Random(seed))
            val named = Race(animals, Random(seed), names = mapOf(Animal.HORSE to "민수", Animal.PIG to "서연"))
            assertEquals(plain.ranking().map { it.animal to it.finish }, named.ranking().map { it.animal to it.finish })
        }
    }
}
