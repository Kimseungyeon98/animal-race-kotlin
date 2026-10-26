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
            drag(circle(195f, 520f, 42f, 1.15f), 1400); pause(300); snap("1-rolling")
            pause(1400); snap("2-open")
            tap(195f, 430f); pause(250); tap(195f, 430f); pause(300); snap("3-cracks")
            tap(195f, 430f); pause(900); snap("4-safe")
            tap(195f, 720f); pause(600); snap("5-next")
            drag(circle(195f, 520f, 42f, 1.15f), 1400); pause(1700)
            repeat(3) { tap(195f, 430f); pause(250) }; pause(900); snap("6-bust")
            pause(2000); snap("7-final")
        }) { MiniGameScreen(session(MiniGame.GACHA), "커피 쏘기", {}, {}) },
        Scene("t-lotto", script = {
            pause(800); tap(80f, 445f); pause(900); snap("1-big")
            snap("1b")
        }) { MiniGameScreen(session(MiniGame.LOTTO), "커피 쏘기", {}, {}) },
        Scene("t-ladder", script = {
            // 고양이: 빈 높이에 한 줄 (사다리 칸 위 202dp ~ 아래 700dp, 높이 13칸), 돼지·판다: 넘기기 → 출발
            val free = (0 until Ladder.LEVELS).first { lv -> ladder.canAdd(Rung(lv, 0)) }
            pause(800); tap(135f, 202f + 498f * (free + 1) / (Ladder.LEVELS + 1)); pause(500); snap("1-rung")
            tap(333f, 791f); pause(300); tap(333f, 791f); pause(500); snap("2-ready")
            tap(195f, 790f); pause(2000); snap("3-walking")
            pause(9000); snap("4-done")
            pause(1800); snap("5-final")
        }) { MiniGameScreen(MiniGameSession(MiniGame.LADDER, players, ladder, 1), "커피 쏘기", {}, {}) },
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
