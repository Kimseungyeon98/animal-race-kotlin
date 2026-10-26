package com.example.animalrace.shot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.use
import com.example.animalrace.AppState
import com.example.animalrace.MiniGameSession
import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.Fx
import com.example.animalrace.model.Ladder
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.SavedSetup
import com.example.animalrace.model.Venue
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.minigame.MiniGameScreen
import com.example.animalrace.ui.result.ResultScreen
import com.example.animalrace.ui.setup.SetupScreen
import com.example.animalrace.ui.theme.AnimalRaceTheme
import com.example.animalrace.ui.theme.Pal
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * 앱 화면을 PNG로 찍는다 (Android 없이 데스크톱 Compose로). 폰 크기 390×844dp, 2배 해상도.
 * 장면: characters(캐릭터 전체), fx(연출), setup, result, ladder, lotto, gacha
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/shots" }).apply { mkdirs() }
    val only = args.getOrElse(1) { "all" }
    val scenes = scenes()
    scenes.filter { only == "all" || it.name == only }.forEach { s ->
        val file = File(out, "${s.name}.png")
        shoot(file, s.w, s.h, s.frames, s.content)
        println(file.path)
    }
}

private class Scene(val name: String, val w: Int = 390, val h: Int = 844, val frames: Int = 30, val content: @Composable () -> Unit)

private fun shoot(file: File, w: Int, h: Int, frames: Int, content: @Composable () -> Unit) {
    val d = 2f
    ImageComposeScene((w * d).toInt(), (h * d).toInt(), Density(d)) {
        AnimalRaceTheme { Box(Modifier.fillMaxSize().background(Pal.Ivory)) { content() } }
    }.use { sc ->
        var t = 0L
        repeat(frames) { t += 33_000_000L; sc.render(t) }
        file.writeBytes(sc.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
}

private object FakeRepo : AppRepository {
    override fun loadSetup(): SavedSetup? = null
    override fun saveSetup(setup: SavedSetup) {}
    override fun loadHistory(): List<RaceRecord> = emptyList()
    override fun saveHistory(history: List<RaceRecord>) {}
}

private fun results(vararg a: Animal) = a.mapIndexed { i, x -> RaceResult(x, 30.0 + i * 0.6, 1, lane = i) }

private fun scenes(): List<Scene> {
    val players = results(Animal.CAT, Animal.PIG, Animal.PANDA)
    fun session(g: MiniGame) = MiniGameSession(g, players, Ladder.random(3), 1)
    val cols = 5
    return listOf(
        Scene("characters", w = 120 * cols + 24, h = 140 * ((Animal.entries.size + cols - 1) / cols) + 24) {
            Column(Modifier.padding(12.dp)) {
                Animal.entries.chunked(cols).forEach { row ->
                    Row {
                        row.forEach { a ->
                            Column(Modifier.width(120.dp).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimalSprite(a, 104.dp)
                                Text(a.label, fontSize = 15.sp, color = Pal.Ink)
                            }
                        }
                    }
                }
            }
        },
        Scene("fx", w = 120 * 5 + 24, h = 140 * Fx.entries.size / 5 + 180) {
            // 고양이로 연출 전체 (fxTime 0.5초 지점)
            Column(Modifier.padding(12.dp)) {
                Fx.entries.chunked(5).forEach { row ->
                    Row {
                        row.forEach { fx ->
                            Column(Modifier.width(120.dp).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimalSprite(Animal.CAT, 104.dp, fx = fx, fxTime = 0.5f)
                                Text(fx.name, fontSize = 12.sp, color = Pal.Ink)
                            }
                        }
                    }
                }
            }
        },
        Scene("setup") {
            val state = AppState(FakeRepo).apply { listOf(Animal.HORSE, Animal.CAT, Animal.PANDA).forEach { toggle(it) } }
            SetupScreen(state)
        },
        Scene("result") {
            ResultScreen(
                RaceRecord(0L, "꼴찌가 커피 쏘기", 3, results(Animal.HORSE, Animal.CAT, Animal.PANDA, Animal.DOG, Animal.PIG), Venue.MEADOW),
                onAgain = {}, onHome = {},
            )
        },
        Scene("ladder") { MiniGameScreen(session(MiniGame.LADDER), "커피 쏘기", {}, {}) },
        Scene("lotto") { MiniGameScreen(session(MiniGame.LOTTO), "커피 쏘기", {}, {}) },
        Scene("gacha") { MiniGameScreen(session(MiniGame.GACHA), "커피 쏘기", {}, {}) },
    )
}
