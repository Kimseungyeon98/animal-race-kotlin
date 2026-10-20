package com.example.animalrace

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.animalrace.data.PrefsRepository
import com.example.animalrace.ui.App
import com.example.animalrace.ui.theme.AnimalRaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 경기 중 화면 꺼짐 방지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val repository = PrefsRepository(applicationContext)
        setContent {
            val state = remember { AppState(repository) }
            // 경기·결과 화면에서 뒤로가기 → 메인. 메인에서는 기본 동작(앱 종료)
            BackHandler(enabled = state.screen != Screen.Setup) { state.goHome() }
            AnimalRaceTheme { App(state) }
        }
    }
}
