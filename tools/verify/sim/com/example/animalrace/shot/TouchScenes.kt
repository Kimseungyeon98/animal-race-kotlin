package com.example.animalrace.shot

import com.example.animalrace.AppState
import com.example.animalrace.MiniGameSession
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Ladder
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.MiniGameResult
import com.example.animalrace.model.Rung
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.Venue
import com.example.animalrace.ui.minigame.MiniGameScreen
import com.example.animalrace.ui.race.RaceScreen
import com.example.animalrace.ui.result.ResultScreen
import com.example.animalrace.ui.setup.SetupScreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 터치해야 열리는 화면들 (각본으로 탭·드래그) */
internal fun touchScenes(): List<Scene> {
    val players = results(Animal.CAT, Animal.PIG, Animal.PANDA)
    // 몰빵은 두 번째 차례(돼지)
    fun session(g: MiniGame) = MiniGameSession(g, players, Ladder.random(3), 1)
    val history = listOf(
        RaceRecord(1_000L, "커피 쏘기", 4, results(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.PIG), Venue.BEACH, listOf(2, 4),
            MiniGameResult(MiniGame.GACHA, Animal.CAT)),
        RaceRecord(900L, "아이스크림 사기", 2, results(Animal.DOG, Animal.DUCK, Animal.SNAIL, Animal.HAMSTER), Venue.SNOW),
        RaceRecord(800L, "설거지", 5, results(Animal.CHEETAH, Animal.TURTLE, Animal.SKUNK, Animal.SLOTH, Animal.CAPYBARA, Animal.ELEPHANT), Venue.DESERT, listOf(3)),
    )
    val ladder = Ladder.random(3)
    fun circle(cx: Float, cy: Float, r: Float, turns: Float) =
        (0..48).map { k -> val a = -PI / 2 + 2 * PI * turns * k / 48; (cx + (cos(a) * r).toFloat()) to (cy + (sin(a) * r).toFloat()) }
    return listOf(
        // 5.7: 순서 없이 · 몰빵이 나오면 "누가 긁었어요/돌렸어요?" (3명)
        Scene("n-lotto-play", script = {
            fun scratch() { drag((0..56).map { k -> (if (k % 2 == 0) 45f else 350f) to (275f + k * 8f) }, 2400) }
            pause(900); snap("0-start")
            tap(70f, 335f); pause(700); scratch(); pause(500); snap("1-pass")
            tap(195f, 792f); pause(500); snap("2-next")
            tap(195f, 490f); pause(700); scratch(); pause(300); snap("3-bust")
            pause(1200); snap("4-who")
            tap(244f, 486f); pause(700); snap("5-picked"); pause(1800); snap("6-final")
        }) {
            val ps = results(Animal.CAT, Animal.PIG, Animal.PANDA).mapIndexed { i, r -> r.copy(who = listOf("지은", "민수", "현우")[i]) }
            MiniGameScreen(MiniGameSession(MiniGame.LOTTO, ps, Ladder.random(3), 1, 4), "커피 쏘기", {}, {})
        },
        Scene("n-gacha-play", script = {
            pause(900); snap("0-start")
            drag(circle(195f, 585f, 44f, 1.15f), 1400); pause(1700)
            repeat(3) { tap(195f, 430f); pause(250) }; pause(700); snap("1-pass")
            tap(195f, 700f); pause(600)
            drag(circle(195f, 585f, 44f, 1.15f), 1400); pause(1700)
            repeat(3) { tap(195f, 430f); pause(250) }; pause(300); snap("2-bust")
            pause(1300); snap("3-who")
            tap(244f, 448f); pause(700); snap("4-picked"); pause(1800); snap("5-final")
        }) {
            val ps = results(Animal.CAT, Animal.PIG, Animal.PANDA).mapIndexed { i, r -> r.copy(who = listOf("지은", "민수", "현우")[i]) }
            MiniGameScreen(MiniGameSession(MiniGame.GACHA, ps, Ladder.random(3), 1, 1), "커피 쏘기", {}, {})
        },
        Scene("n-lotto", snaps = listOf(1200)) {
            val ps = results(Animal.CAT, Animal.PIG, Animal.PANDA).mapIndexed { i, r -> r.copy(who = listOf("지은", "민수", "현우")[i]) }
            MiniGameScreen(MiniGameSession(MiniGame.LOTTO, ps, Ladder.random(3), 1, 4), "커피 쏘기", {}, {})
        },
        Scene("n-gacha", snaps = listOf(1200)) {
            val ps = results(Animal.CAT, Animal.PIG, Animal.PANDA).mapIndexed { i, r -> r.copy(who = listOf("지은", "민수", "현우")[i]) }
            MiniGameScreen(MiniGameSession(MiniGame.GACHA, ps, Ladder.random(3), 1, 1), "커피 쏘기", {}, {})
        },
        Scene("lotto6", snaps = listOf(1000)) {
            MiniGameScreen(MiniGameSession(MiniGame.LOTTO, results(Animal.CAT, Animal.PIG, Animal.PANDA, Animal.DOG, Animal.DUCK, Animal.HORSE), Ladder.random(6), 2), "커피 쏘기", {}, {})
        },
        Scene("t-ladder", script = {
            // 출발 전까지 아무나 자유롭게: 빈 자리 세 곳에 긋고, 하나는 다시 탭해서 지우기
            val spots = (0 until Ladder.LEVELS).flatMap { lv -> listOf(Rung(lv, 0), Rung(lv, 1)) }.filter { ladder.canAdd(it) }
            fun y(r: Rung) = 170f + 35.6f * (r.level + 1)
            fun x(r: Rung) = if (r.left == 0) 135f else 255f
            val picks = listOf(spots[0], spots.first { it.left == 1 && it.level > spots[0].level + 1 }, spots.last())
            pause(800); snap("0-start")
            picks.forEach { tap(x(it), y(it)); pause(350) }; snap("1-drawn")
            tap(x(picks[2]), y(picks[2])); pause(400); snap("2-erased")
            tap(330f, 793f); pause(2500); snap("3-walking")
        }) { MiniGameScreen(MiniGameSession(MiniGame.LADDER, players, ladder, 1), "커피 쏘기", {}, {}) },
        // 5.5: 참가자 8명을 넣은 뒤 이름표를 동물 칸에 끌어다 놓기
        Scene("t-people", script = {
            val chip = 49f to 186f
            fun dragTo(x: Float, y: Float, from: Pair<Float, Float> = chip, mid: String? = null) {
                press(from.first, from.second); pause(80)
                moveTo(from.first, from.second, (from.first + x) / 2, (from.second + y) / 2, 200)
                moveTo((from.first + x) / 2, (from.second + y) / 2, x, y, 250)
                pause(150); mid?.let { snap(it) }
                release(x, y); pause(450)
            }
            pause(900); snap("00")
            dragTo(317f, 371f, mid = "01"); snap("02")
            dragTo(72f, 371f); snap("03")
            dragTo(317f, 456f); snap("04")
            // 판다에 붙은 지은 이름표를 햄스터로 옮기기
            dragTo(195f, 340f, from = 317f to 362f, mid = "05"); snap("06")
            listOf(72f to 456f, 195f to 456f, 195f to 541f, 72f to 541f).forEachIndexed { i, (x, y) ->
                dragTo(x, y); snap("%02d".format(7 + i))
            }
            pause(500); snap("11")
        }) {
            // 한 줄에 들어가는 7명 (줄 수가 바뀌면 동물 칸 위치도 바뀌어서)
            val people = listOf("지은", "민수", "현우", "서연", "도윤", "하린", "재석")
            SetupScreen(androidx.compose.runtime.remember { AppState(FakeRepo).apply { setPeople(people) } })
        },
        // 5.6.1: 두 줄(8명)에서 하나를 붙이면, 끄는 동안은 높이 그대로 → 놓고 나면 한 줄로 줄어듦
        Scene("t-people-shrink", script = {
            pause(900); snap("0-two-rows")
            press(49f, 186f); pause(80)
            moveTo(49f, 186f, 200f, 300f, 200); moveTo(200f, 300f, 317f, 371f, 250); pause(150); snap("1-dragging")
            release(317f, 371f); pause(700); snap("2-one-row")
        }) {
            val people = listOf("지은", "민수", "현우", "서연", "도윤", "하린", "재석", "준하")
            SetupScreen(androidx.compose.runtime.remember { AppState(FakeRepo).apply { setPeople(people) } })
        },
        // 5.5: 이름표를 동물 목록 아래 가장자리로 끌고 있으면 저절로 스크롤 → 아래쪽 동물에 놓기
        Scene("t-people-scroll", script = {
            pause(900)
            press(49f, 186f); pause(80)
            moveTo(49f, 186f, 195f, 655f, 400)
            pause(1500); snap("1-scrolled")
            moveTo(195f, 655f, 195f, 560f, 200); pause(200); snap("2-hover")
            release(195f, 560f); pause(500); snap("3-dropped")
        }) {
            val people = listOf("지은", "민수", "현우", "서연", "도윤", "하린", "재석", "준하")
            SetupScreen(androidx.compose.runtime.remember { AppState(FakeRepo).apply { setPeople(people) } })
        },
        Scene("t-setup", script = {
            pause(900); tap(348f, 43f); pause(900); snap("1-history")
        }) { FakeRepo.history = history; SetupScreen(AppState(FakeRepo).apply { listOf(Animal.HORSE, Animal.CAT).forEach { toggle(it) } }) },
        Scene("t-dex", script = {
            pause(900); tap(288f, 43f); pause(900); snap("1-dex")
        }) { SetupScreen(AppState(FakeRepo)) },
        Scene("t-bet", script = {
            pause(900); tap(195f, 108f); pause(900); snap("1-bet")
        }) { SetupScreen(AppState(FakeRepo)) },
        Scene("t-result", snaps = listOf(1200)) {
            ResultScreen(RaceRecord(0L, "커피 쏘기", 3, results(Animal.HORSE, Animal.CAT, Animal.PANDA, Animal.DOG, Animal.PIG, Animal.RABBIT), Venue.MEADOW, listOf(2, 5)),
                onAgain = {}, onHome = {})
        },
        Scene("t-exit", snaps = listOf(5000)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA), "커피 쏘기", 0, { _, _ -> }, confirmExit = true)
        },
    )
}
