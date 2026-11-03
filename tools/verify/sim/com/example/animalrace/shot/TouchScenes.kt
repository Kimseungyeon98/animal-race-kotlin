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
        Scene("t-gacha", script = {
            pause(800)
            drag(circle(195f, 605f, 44f, 1.15f), 1400); pause(300); snap("1-rolling")
            pause(1400); snap("2-open")
            tap(195f, 430f); pause(250); tap(195f, 430f); pause(300); snap("3-cracks")
            tap(195f, 430f); pause(900); snap("4-safe")
            tap(195f, 720f); pause(600); snap("5-next")
            drag(circle(195f, 605f, 44f, 1.15f), 1400); pause(1700)
            repeat(3) { tap(195f, 430f); pause(250) }; pause(900); snap("6-bust")
            pause(2000); snap("7-final")
        }) { MiniGameScreen(session(MiniGame.GACHA), "커피 쏘기", {}, {}) },
        // 룰렛 (시안): 꾹 누르면 게이지 왕복 → 떼면 그 힘으로 빙글빙글 → 몰빵 칸에서 멈춤
        *listOf(4, 6, 9).map { k ->
            Scene("t-roulette$k", script = {
                pause(800); snap("0-ready")
                press(195f, 795f); pause(380); snap("1-charge"); pause(mapOf(4 to 330L, 6 to 700L, 9 to 250L).getValue(k)); snap("2-charge")
                release(195f, 795f)
                for (t in 1..14) { pause(600); snap("s%02d".format(t)) }
            }) {
                val names = listOf("지은", "서연", "도윤", "민수", "현우", "하린", "재석", "준하", "세호")
                val ps = results(Animal.CAT, Animal.PIG, Animal.PANDA, Animal.HORSE, Animal.RABBIT, Animal.DOG, Animal.HAMSTER, Animal.DUCK, Animal.CAPYBARA)
                    .take(k).mapIndexed { i, r -> r.copy(who = names[i], lane = i) }
                MiniGameScreen(MiniGameSession(MiniGame.ROULETTE, ps, Ladder.random(k), 1), "커피 쏘기", {}, {})
            }
        }.toTypedArray(),
        Scene("t-picker", script = { pause(1000); tap(195f, 318f); pause(900); snap("1-games") }) {
            val who = listOf("지은", "민수", "현우", "서연", "도윤")
            ResultScreen(
                RaceRecord(0L, "커피 쏘기", 3, results(Animal.CAT, Animal.HORSE, Animal.RABBIT, Animal.PIG, Animal.DOG).mapIndexed { i, r -> r.copy(who = who[i]) },
                    Venue.BEACH, listOf(2, 4)),
                onAgain = {}, onHome = {},
            )
        },
        Scene("t-lotto", script = {
            // 이름 없이 복권부터 누르면 "먼저 이름을" → 이름 누르고 다시 → 크게 펼침
            pause(800); tap(80f, 445f); pause(600); snap("1-nudge")
            tap(70f, 143f); pause(600); snap("2-picked")
            tap(80f, 445f); pause(120); snap("3-opening"); pause(780); snap("4-big")
        }) { MiniGameScreen(session(MiniGame.LOTTO), "커피 쏘기", {}, {}) },
        Scene("t-gacha-who", script = {
            // 캡슐: 이름 없이 레버부터 → 빨간 안내, 이름 누르면 "○○ 차례!"
            pause(800); drag(listOf(195f to 470f, 215f to 495f, 235f to 520f), 300); pause(600); snap("1-nudge")
            tap(70f, 795f); pause(600); snap("2-picked")
        }) { MiniGameScreen(session(MiniGame.GACHA), "커피 쏘기", {}, {}) },
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
