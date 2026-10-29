package com.example.animalrace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.Ladder
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.MiniGameResult
import com.example.animalrace.model.MiniGameDraw
import com.example.animalrace.model.RandomRank
import com.example.animalrace.model.RankMode
import com.example.animalrace.model.RankSetting
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.SavedSetup
import com.example.animalrace.model.Venue
import com.example.animalrace.race.Race

enum class Screen { Setup, Race, Result, MiniGame }

/**
 * 진행 중인 몰아주기 한 판. [bomb]은 몰빵이 숨은 자리로 시작할 때 고르게 뽑아 둔다.
 * 사다리: [ladder]는 처음 깔린 사다리, [bomb]은 몰빵 칸(아래 칸 위치). 사람들이 줄을 더 그은 뒤 그 칸에 도착한 사람이 몰빵.
 * 복권: [roundBomb]은 똑같은 복권 [MiniGameDraw.slots]장 중 몰빵이 숨은 장의 위치. 그 장을 고른 사람이 몰빵.
 * 가챠: [roundBomb]번째(0부터)로 나오는 캡슐이 몰빵 (캡슐 [MiniGameDraw.slots]개, 출전 번호 순으로 돌아가며 뽑음).
 */
data class MiniGameSession(
    val game: MiniGame,
    val players: List<RaceResult>,
    val ladder: Ladder,
    val bomb: Int,
    val roundBomb: Int = bomb,
)

/** 한 판의 숨긴 결과 (같은 판에서 다시 열어도 그대로) */
private data class MiniGameDrawn(val at: Long, val bomb: Int, val ladder: Ladder, val roundBomb: Int)

/**
 * 앱 전체 상태. 화면 전환도 이 상태값([screen])으로 처리한다.
 * Compose의 mutableState라서 값이 바뀌면 해당 값을 읽는 화면이 자동으로 다시 그려진다.
 */
class AppState(private val repo: AppRepository) {
    var screen by mutableStateOf(Screen.Setup)
        private set
    val selected = mutableStateListOf<Animal>()
    var bet by mutableStateOf("꼴찌가 커피 쏘기")

    /** 배경음악 켜기/끄기 (메인 화면 🎵 버튼) */
    var musicOn by mutableStateOf(repo.loadMusicOn())
        private set

    fun toggleMusic() {
        musicOn = !musicOn
        repo.saveMusicOn(musicOn)
    }

    /** 효과음 켜기/끄기 (진동과 별개, 메인 화면 🔊 버튼) */
    var soundOn by mutableStateOf(repo.loadSoundOn())
        private set

    fun toggleSound() {
        soundOn = !soundOn
        repo.saveSoundOn(soundOn)
    }

    /** 랜덤 등수 모드: 켜면 출발 전에 룰렛으로 당첨 등수를 뽑는다 (끄면 꼴찌 벌칙) */
    var randomRank by mutableStateOf(false)
        private set

    /** 랜덤 등수 설정: 룰렛/직접 지정, 당첨 인원 (시안: "랜덤 등수 · 인원·등수 직접 정하기") */
    var rankSetting by mutableStateOf(RankSetting())
        private set

    /** 출발 가능: 직접 지정인데 지금 출전 수 안에 남는 등수가 없으면 못 간다 */
    val rankReady get() = !randomRank || RandomRank.ready(selected.size, rankSetting)

    /** 이번 판의 당첨 등수. 랜덤 등수 모드가 아니면 null */
    var picks by mutableStateOf<List<Int>?>(null)
        private set

    /** 이번 판 당첨 등수를 직접 정했는지 (경주 시작 때 룰렛 대신 발표 카드) */
    var picksFixed by mutableStateOf(false)
        private set
    /** 동물마다 달릴 사람 이름 (4.0 사람이 주인공). 출전 동물처럼 앱을 켤 때마다 비운다 */
    val names = mutableStateMapOf<Animal, String>()

    /** 최근에 쓴 이름 (최근 것이 앞, 이름 붙이기 창의 빠른 칩) */
    val members = mutableStateListOf<String>()

    /** 멤버 묶음: 한 번에 불러오는 자주 모이는 사람들 */
    val groups = mutableStateListOf<List<String>>()

    val history = mutableStateListOf<RaceRecord>()
    var lastRecord by mutableStateOf<RaceRecord?>(null)
        private set

    /** 같은 멤버로 다시 할 때 경주 화면을 새로 만들기 위한 키 */
    var raceSeq by mutableIntStateOf(0)
        private set

    val canStart get() = selected.size >= MIN_RACERS

    /** 몰아주기 미니게임 (결과 화면에서 시작) */
    var miniGame by mutableStateOf<MiniGameSession?>(null)
        private set

    /** 마지막으로 고른 미니게임 (게임 고르기 창에서 먼저 선택돼 있게) */
    var lastGame by mutableStateOf(MiniGame.LADDER)
        private set

    // 같은 판에서 게임을 바꾸거나 다시 열어도 몰빵 자리·처음 사다리는 그대로 (다시 뽑기 방지)
    private var draw: MiniGameDrawn? = null

    /** 경주 중 뒤로 가기를 눌러 "그만둘까요?" 확인창이 떠 있는지 (떠 있는 동안 경주 일시정지) */
    var confirmExit by mutableStateOf(false)
        private set

    init {
        // 앱을 켤 때마다 출전 동물은 비운 채로 시작하고, 벌칙 문구와 랜덤 등수 모드만 지난번 것을 이어 쓴다
        repo.loadSetup()?.let {
            bet = it.bet; randomRank = it.randomRank; rankSetting = it.rank
            members.addAll(it.members.take(MEMBERS_SIZE)); groups.addAll(it.groups.take(GROUPS_SIZE))
        }
        history.addAll(repo.loadHistory().take(HISTORY_SIZE))
    }

    /** 선택/해제. 최대 인원을 넘기면 false. */
    fun toggle(animal: Animal): Boolean {
        if (animal in selected) { selected.remove(animal); names.remove(animal) }
        else if (selected.size < MAX_RACERS) selected.add(animal)
        else return false
        persistSetup()
        return true
    }

    fun clearSelection() { selected.clear(); names.clear(); persistSetup() }

    fun pickRandom(count: Int) {
        // 이름은 동물이 아니라 사람을 따라간다: 새로 뽑은 동물에 같은 순서로 다시 붙인다
        val people = selected.mapNotNull { names[it] }
        selected.clear()
        selected.addAll(Animal.entries.shuffled().take(count))
        names.clear()
        people.zip(selected).forEach { (who, a) -> names[a] = who }
        persistSetup()
    }

    /** 이름 붙이기 창에서 [확인]: 출전 동물의 이름을 한꺼번에 바꾸고, 새 이름은 최근 멤버 앞에 */
    fun updateNames(draft: Map<Animal, String>) {
        names.clear()
        draft.forEach { (a, n) -> val who = cleanName(n); if (a in selected && who.isNotEmpty()) names[a] = who }
        val used = selected.mapNotNull { names[it] }
        val next = (used + members).distinct().take(MEMBERS_SIZE)
        members.clear(); members.addAll(next)
        persistSetup()
    }

    /** 멤버 묶음 불러오기 준비: 출전 동물이 사람 수보다 적으면 남은 동물 중 무작위로 채운다 */
    fun ensureRacers(count: Int) {
        val need = count.coerceAtMost(MAX_RACERS) - selected.size
        if (need > 0) selected.addAll(Animal.entries.filter { it !in selected }.shuffled().take(need))
        persistSetup()
    }

    fun saveGroup(people: List<String>) {
        val g = people.map(::cleanName).filter { it.isNotEmpty() }.distinct()
        if (g.size < 2 || g in groups) return
        groups.add(0, g)
        while (groups.size > GROUPS_SIZE) groups.removeAt(groups.lastIndex)
        persistSetup()
    }

    fun deleteGroup(group: List<String>) { groups.remove(group); persistSetup() }

    fun forgetMember(who: String) { members.remove(who); persistSetup() }

    fun updateBet(value: String) { bet = value.take(30); persistSetup() }

    fun updateRandomRank(on: Boolean) { randomRank = on; persistSetup() }

    fun updateRankSetting(setting: RankSetting) { rankSetting = setting; persistSetup() }

    fun startRace() {
        if (!canStart || !rankReady) return
        // 경주와 따로 정하므로 경주 결과에는 영향이 없다
        picks = if (randomRank) RandomRank.picks(selected.size, rankSetting) else null
        picksFixed = randomRank && rankSetting.mode == RankMode.FIXED
        miniGame = null
        raceSeq++
        confirmExit = false
        screen = Screen.Race
    }

    fun finishRace(race: Race, venue: Venue) {
        val record = RaceRecord(
            at = System.currentTimeMillis(),
            bet = bet.trim(),
            leadChanges = race.leadChanges,
            results = race.ranking().map { RaceResult(it.animal, it.finish ?: 0.0, it.uses, it.lane, names[it.animal]) },
            venue = venue,
            picks = picks,
            picksFixed = picksFixed,
        )
        lastRecord = record
        history.add(0, record)
        while (history.size > HISTORY_SIZE) history.removeAt(history.lastIndex)
        repo.saveHistory(history.toList())
        screen = Screen.Result
    }

    /** 몰아주기 시작. [game]이 null이면 아무거나. */
    fun openMiniGame(game: MiniGame?) {
        val record = lastRecord ?: return
        val players = record.losers
        if (record.picks == null || players.size < 2 || record.miniGame != null) return
        val chosen = game ?: MiniGameDraw.game()
        if (game != null) lastGame = game
        val d = draw?.takeIf { it.at == record.at } ?: MiniGameDrawn(
            record.at, MiniGameDraw.bomb(players.size), Ladder.random(players.size),
            MiniGameDraw.bomb(MiniGameDraw.slots(players.size)),
        )
        draw = d
        miniGame = MiniGameSession(chosen, players, d.ladder, d.bomb, d.roundBomb)
        screen = Screen.MiniGame
    }

    /** 미니게임에서 몰빵이 공개되면 기록에 남긴다. [loser]는 [MiniGameSession.players] 중 몰빵한 사람의 위치. */
    fun recordMiniGame(loser: Int) {
        val session = miniGame ?: return
        val record = lastRecord ?: return
        if (record.miniGame != null) return
        val p = session.players[loser]
        val updated = record.copy(miniGame = MiniGameResult(session.game, p.animal, p.who))
        lastRecord = updated
        val i = history.indexOfFirst { it.at == record.at }
        if (i >= 0) history[i] = updated
        repo.saveHistory(history.toList())
    }

    /** 미니게임을 닫고 결과 화면으로 */
    fun closeMiniGame() { miniGame = null; screen = Screen.Result }

    fun clearHistory() { history.clear(); repo.saveHistory(emptyList()) }

    fun goHome() { confirmExit = false; miniGame = null; screen = Screen.Setup }

    /** 뒤로 가기: 경주 중이면 확인창을 열고(이미 열려 있으면 닫고 계속), 미니게임이면 결과 화면으로, 결과 화면이면 바로 메인으로 */
    fun back() {
        when (screen) {
            Screen.Race -> confirmExit = !confirmExit
            Screen.MiniGame -> closeMiniGame()
            else -> goHome()
        }
    }

    fun keepRacing() { confirmExit = false }

    private fun persistSetup() = repo.saveSetup(SavedSetup(selected.toList(), bet, randomRank, rankSetting, members.toList(), groups.toList()))

    companion object {
        const val MIN_RACERS = 2
        const val MAX_RACERS = 10
        const val HISTORY_SIZE = 200
        const val MEMBERS_SIZE = 12
        const val GROUPS_SIZE = 5
        const val NAME_MAX = 8

        /** 이름 정리: 앞뒤 공백 제거, 최대 [NAME_MAX]자 */
        fun cleanName(raw: String) = raw.trim().replace(Regex("\\s+"), " ").take(NAME_MAX)
    }
}
