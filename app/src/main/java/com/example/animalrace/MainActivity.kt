package com.example.animalrace

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.animalrace.data.AndroidSound
import com.example.animalrace.data.PrefsRepository
import com.example.animalrace.ui.App
import com.example.animalrace.ui.sound.LocalSound
import com.example.animalrace.ui.theme.AnimalRaceTheme

class MainActivity : ComponentActivity() {
    private lateinit var sound: AndroidSound

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 밝은 하늘 배경이라 시스템 다크 모드와 상관없이 상태바·내비게이션 아이콘을 어둡게
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // 경기 중 화면 꺼짐 방지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val repository = PrefsRepository(applicationContext)
        sound = AndroidSound(applicationContext)
        setContent {
            val state = remember { AppState(repository) }
            // 🔊 효과음 · 🎵 배경음악 버튼
            LaunchedEffect(state.soundOn) { sound.sfxOn = state.soundOn }
            LaunchedEffect(state.musicOn) { sound.musicOn = state.musicOn }
            // 경기 중 뒤로가기 → "그만둘까요?" 확인창, 결과 화면 → 메인. 메인에서는 기본 동작(앱 종료)
            BackHandler(enabled = state.screen != Screen.Setup) { state.back() }
            CompositionLocalProvider(LocalSound provides sound) {
                AnimalRaceTheme { App(state) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::sound.isInitialized) sound.onForeground()
    }

    override fun onStop() {
        super.onStop()
        sound.onBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        sound.release()
    }
}
