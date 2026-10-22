package com.example.animalrace.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.example.animalrace.AppState
import com.example.animalrace.Screen
import com.example.animalrace.ui.minigame.MiniGameScreen
import com.example.animalrace.ui.race.RaceScreen
import com.example.animalrace.ui.result.ResultScreen
import com.example.animalrace.ui.setup.SetupScreen

/** 화면 전환: [AppState.screen] 값에 따라 화면을 바꾸고, 바뀔 때 페이드로 넘어간다. */
@Composable
fun App(state: AppState) {
    AnimatedContent(
        targetState = state.screen,
        transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
        label = "screen",
    ) { screen ->
        when (screen) {
            Screen.Setup -> SetupScreen(state)
            Screen.Race -> RaceScreen(
                animals = state.selected.toList(),
                bet = state.bet.trim(),
                raceKey = state.raceSeq,
                picks = state.picks,
                onFinished = state::finishRace,
                confirmExit = state.confirmExit,
                onKeepRacing = state::keepRacing,
                onQuit = state::goHome,
            )
            Screen.Result -> state.lastRecord?.let { record ->
                ResultScreen(record, onAgain = state::startRace, onHome = state::goHome, lastGame = state.lastGame, onMollbang = state::openMollbang)
            }
            Screen.MiniGame -> state.mollbang?.let { session ->
                MiniGameScreen(
                    session, bet = state.lastRecord?.bet.orEmpty(),
                    onDecided = state::recordMollbang,
                    onClose = state::closeMollbang,
                    onAgain = { state.closeMollbang(); state.startRace() },
                )
            }
        }
    }
}
