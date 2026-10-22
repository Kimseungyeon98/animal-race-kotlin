package com.example.animalrace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.Ladder
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.Mollbang
import com.example.animalrace.model.MollbangDraw
import com.example.animalrace.model.RandomRank
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.SavedSetup
import com.example.animalrace.model.Venue
import com.example.animalrace.race.Race

enum class Screen { Setup, Race, Result, MiniGame }

/**
 * 진행 중인 몰아주기 한 판. [loser]는 [players] 중 몰빵할 사람의 위치로, 게임을 시작하기 전에 뽑아 둔다.
 * 사다리는 무작위로 깐 뒤 몰빵할 사람이 도착하는 칸을 몰빵 칸으로 정한다.
 */
data class MollbangSession(
    val game: MiniGame,
    val players: List<RaceResult>,
    val loser: Int,
    val ladder: Ladder,
)

/**
 * 앱 전체 상태. 화면 전환도 이 상태값([screen])으로 처리한다.
 * Compose의 mutableState라서 값이 바뀌면 해당 값을 읽는 화면이 자동으로 다시 그려진다.
 */
class AppState(private val repo: AppRepository) {
    var screen by mutableStateOf(Screen.Setup)
        private set
    val selected = mutableStateListOf<Animal>()
    var bet by mutableStateOf("꼴찌가 커피 쏘기")

    /** 랜덤 등수 모드: 켜면 출발 전에 룰렛으로 당첨 등수를 뽑는다 (끄면 꼴찌 벌칙) */
    var randomRank by mutableStateOf(false)
        private set

    /** 이번 판의 당첨 등수. 랜덤 등수 모드가 아니면 null */
    var picks by mutableStateOf<List<Int>?>(null)
        private set
    val history = mutableStateListOf<RaceRecord>()
    var lastRecord by mutableStateOf<RaceRecord?>(null)
        private set

    /** 같은 멤버로 다시 할 때 경주 화면을 새로 만들기 위한 키 */
    var raceSeq by mutableIntStateOf(0)
        private set

    val canStart get() = selected.size >= MIN_RACERS

    /** 몰아주기 미니게임 (결과 화면에서 시작) */
    var mollbang by mutableStateOf<MollbangSession?>(null)
        private set

    /** 마지막으로 고른 미니게임 (게임 고르기 창에서 먼저 선택돼 있게) */
    var lastGame by mutableStateOf(MiniGame.LADDER)
        private set

    // 같은 판에서 게임을 바꾸거나 다시 열어도 몰빵할 사람은 그대로 (다시 뽑기 방지)
    private var loserDraw: Pair<Long, Int>? = null

    /** 경주 중 뒤로 가기를 눌러 "그만둘까요?" 확인창이 떠 있는지 (떠 있는 동안 경주 일시정지) */
    var confirmExit by mutableStateOf(false)
        private set

    init {
        // 앱을 켤 때마다 출전 동물은 비운 채로 시작하고, 벌칙 문구와 랜덤 등수 모드만 지난번 것을 이어 쓴다
        repo.loadSetup()?.let { bet = it.bet; randomRank = it.randomRank }
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

    fun updateRandomRank(on: Boolean) { randomRank = on; persistSetup() }

    fun startRace() {
        if (!canStart) return
        // 경주와 따로 뽑으므로 경주 결과에는 영향이 없다
        picks = if (randomRank) RandomRank.draw(selected.size) else null
        mollbang = null
        raceSeq++
        confirmExit = false
        screen = Screen.Race
    }

    fun finishRace(race: Race, venue: Venue) {
        val record = RaceRecord(
            at = System.currentTimeMillis(),
            bet = bet.trim(),
            leadChanges = race.leadChanges,
            results = race.ranking().map { RaceResult(it.animal, it.finish ?: 0.0, it.uses, it.lane) },
            venue = venue,
            picks = picks,
        )
        lastRecord = record
        history.add(0, record)
        while (history.size > HISTORY_SIZE) history.removeAt(history.lastIndex)
        repo.saveHistory(history.toList())
        screen = Screen.Result
    }

    /** 몰아주기 시작. [game]이 null이면 아무거나. */
    fun openMollbang(game: MiniGame?) {
        val record = lastRecord ?: return
        val players = record.losers
        if (record.picks == null || players.size < 2 || record.mollbang != null) return
        val chosen = game ?: MollbangDraw.game()
        if (game != null) lastGame = game
        val loser = loserDraw?.takeIf { it.first == record.at }?.second ?: MollbangDraw.loser(players.size)
        loserDraw = record.at to loser
        mollbang = MollbangSession(chosen, players, loser, Ladder.random(players.size))
        screen = Screen.MiniGame
    }

    /** 미니게임에서 몰빵이 공개되면 기록에 남긴다. */
    fun recordMollbang() {
        val session = mollbang ?: return
        val record = lastRecord ?: return
        if (record.mollbang != null) return
        val updated = record.copy(mollbang = Mollbang(session.game, session.players[session.loser].animal))
        lastRecord = updated
        val i = history.indexOfFirst { it.at == record.at }
        if (i >= 0) history[i] = updated
        repo.saveHistory(history.toList())
    }

    /** 미니게임을 닫고 결과 화면으로 */
    fun closeMollbang() { mollbang = null; screen = Screen.Result }

    fun clearHistory() { history.clear(); repo.saveHistory(emptyList()) }

    fun goHome() { confirmExit = false; mollbang = null; screen = Screen.Setup }

    /** 뒤로 가기: 경주 중이면 확인창을 열고(이미 열려 있으면 닫고 계속), 미니게임이면 결과 화면으로, 결과 화면이면 바로 메인으로 */
    fun back() {
        when (screen) {
            Screen.Race -> confirmExit = !confirmExit
            Screen.MiniGame -> closeMollbang()
            else -> goHome()
        }
    }

    fun keepRacing() { confirmExit = false }

    private fun persistSetup() = repo.saveSetup(SavedSetup(selected.toList(), bet, randomRank))

    companion object {
        const val MIN_RACERS = 2
        const val MAX_RACERS = 10
        const val HISTORY_SIZE = 10
    }
}
