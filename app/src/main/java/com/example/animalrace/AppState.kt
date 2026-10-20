package com.example.animalrace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.SavedSetup
import com.example.animalrace.race.Race

enum class Screen { Setup, Race, Result }

/**
 * 앱 전체 상태. 화면 전환도 이 상태값([screen])으로 처리한다.
 * Compose의 mutableState라서 값이 바뀌면 해당 값을 읽는 화면이 자동으로 다시 그려진다.
 */
class AppState(private val repo: AppRepository) {
    var screen by mutableStateOf(Screen.Setup)
        private set
    val selected = mutableStateListOf<Animal>()
    var bet by mutableStateOf("꼴찌가 커피 쏘기")
    val history = mutableStateListOf<RaceRecord>()
    var lastRecord by mutableStateOf<RaceRecord?>(null)
        private set

    /** 같은 멤버로 다시 할 때 경주 화면을 새로 만들기 위한 키 */
    var raceSeq by mutableIntStateOf(0)
        private set

    val canStart get() = selected.size >= MIN_RACERS

    init {
        val saved = repo.loadSetup()
        if (saved != null) {
            selected.addAll(saved.animals.distinct().take(MAX_RACERS))
            bet = saved.bet
        } else {
            selected.addAll(listOf(Animal.HORSE, Animal.TURTLE, Animal.RABBIT, Animal.MONKEY))
        }
        history.addAll(repo.loadHistory().take(HISTORY_SIZE))
    }

    /** 선택/해제. 최대 인원을 넘기면 false. */
    fun toggle(animal: Animal): Boolean {
        if (animal in selected) selected.remove(animal)
        else if (selected.size < MAX_RACERS) selected.add(animal)
        else return false
        persistSetup()
        return true
    }

    fun clearSelection() { selected.clear(); persistSetup() }

    fun pickRandom(count: Int) {
        selected.clear()
        selected.addAll(Animal.entries.shuffled().take(count))
        persistSetup()
    }

    fun updateBet(value: String) { bet = value.take(30); persistSetup() }

    fun startRace() {
        if (!canStart) return
        raceSeq++
        screen = Screen.Race
    }

    fun finishRace(race: Race) {
        val record = RaceRecord(
            at = System.currentTimeMillis(),
            bet = bet.trim(),
            leadChanges = race.leadChanges,
            results = race.ranking().map { RaceResult(it.animal, it.finish ?: 0.0, it.uses, it.lane) },
        )
        lastRecord = record
        history.add(0, record)
        while (history.size > HISTORY_SIZE) history.removeAt(history.lastIndex)
        repo.saveHistory(history.toList())
        screen = Screen.Result
    }

    fun clearHistory() { history.clear(); repo.saveHistory(emptyList()) }

    fun goHome() { screen = Screen.Setup }

    private fun persistSetup() = repo.saveSetup(SavedSetup(selected.toList(), bet))

    companion object {
        const val MIN_RACERS = 2
        const val MAX_RACERS = 10
        const val HISTORY_SIZE = 10
    }
}
