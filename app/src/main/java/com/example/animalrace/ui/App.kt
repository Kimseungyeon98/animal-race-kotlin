package com.example.animalrace.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.animalrace.ui.ads.AdPacing
import com.example.animalrace.ui.ads.LocalAds
import com.example.animalrace.AppState
import com.example.animalrace.Screen
import com.example.animalrace.ui.minigame.MiniGameScreen
import com.example.animalrace.ui.race.RaceScreen
import com.example.animalrace.ui.result.ResultScreen
import com.example.animalrace.ui.setup.SetupScreen
import com.example.animalrace.ui.sound.LocalSound
import com.example.animalrace.ui.sound.Music

/** 화면 전환: [AppState.screen] 값에 따라 화면을 바꾸고, 바뀔 때 페이드로 넘어간다. */
@Composable
fun App(state: AppState) {
    // 메인 화면은 메인 곡, 미니게임은 메인 곡을 은은하게, 경기 곡은 경주 화면이 "출발!"에 맞춰 튼다, 결과는 효과음만
    val sound = LocalSound.current
    val ads = LocalAds.current
    // 결과 화면에서 넘어간 횟수 (앱을 켤 때마다 0부터): 3번째마다 전면 광고 (6.0)
    val leaves = remember { intArrayOf(0) }
    fun leaveResult(next: () -> Unit) {
        leaves[0]++
        if (AdPacing.due(leaves[0])) ads.showInterstitial(next) else next()
    }
    LaunchedEffect(state.screen) {
        when (state.screen) {
            Screen.Setup -> sound.music(Music.MAIN)
            Screen.MiniGame -> sound.music(Music.MAIN, soft = true)
            else -> sound.music(null)
        }
    }
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
                picksFixed = state.picksFixed,
                onFinished = state::finishRace,
                confirmExit = state.confirmExit,
                onKeepRacing = state::keepRacing,
                onQuit = state::goHome,
                names = state.names.toMap(),
            )
            Screen.Result -> state.lastRecord?.let { record ->
                ResultScreen(
                    record,
                    onAgain = { leaveResult(state::startRace) },
                    onHome = { leaveResult(state::goHome) },
                    lastGame = state.lastGame, onMiniGame = state::openMiniGame,
                )
            }
            Screen.MiniGame -> state.miniGame?.let { session ->
                MiniGameScreen(
                    session, bet = state.lastRecord?.bet.orEmpty(),
                    onDecided = state::recordMiniGame,
                    onClose = state::closeMiniGame,
                )
            }
        }
    }
}
