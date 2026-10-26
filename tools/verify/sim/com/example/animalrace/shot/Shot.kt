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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
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
import com.example.animalrace.ui.art.headTop
import com.example.animalrace.ui.components.Crown
import androidx.compose.foundation.layout.offset
import com.example.animalrace.ui.minigame.MiniGameScreen
import com.example.animalrace.ui.result.ResultScreen
import com.example.animalrace.ui.race.RaceScreen
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
    val scenes = scenes() + touchScenes()
    scenes.filter { only == "all" || it.name == only || it.name.startsWith("$only-") }.forEach { s -> shoot(out, s) }
}

/**
 * [snaps]: 찍을 순간(밀리초). 여러 개면 이름-1000ms.png 처럼 여러 장. 화면 안 delay()가 실제 시간으로 흐르므로 실제 시간에 맞춰 돌린다.
 * [script]가 있으면 snaps 대신 각본대로 탭·드래그하며 snap("이름")마다 이름-이름.png
 */
internal class Scene(
    val name: String, val w: Int = 390, val h: Int = 844, val snaps: List<Long> = listOf(1000),
    val script: (Act.() -> Unit)? = null, val content: @Composable () -> Unit,
)

/** 각본: 좌표는 dp (화면 왼쪽 위 기준) */
@OptIn(ExperimentalComposeUiApi::class)
internal class Act(private val sc: ImageComposeScene, private val out: File, private val name: String, private val d: Float) {
    private val start = System.nanoTime()
    private fun now() = System.nanoTime() - start
    private fun frame() { sc.render(now()); Thread.sleep(16) }
    fun pause(ms: Long) { val end = now() + ms * 1_000_000; while (now() < end) frame() }
    fun snap(label: String) {
        val file = File(out, "$name-$label.png")
        file.writeBytes(sc.render(now()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        println(file.path)
    }
    private fun at(x: Float, y: Float) = Offset(x * d, y * d)
    fun tap(x: Float, y: Float) {
        sc.sendPointerEvent(PointerEventType.Press, at(x, y)); frame(); frame()
        sc.sendPointerEvent(PointerEventType.Release, at(x, y)); pause(120)
    }
    fun hold(x: Float, y: Float, ms: Long) {
        sc.sendPointerEvent(PointerEventType.Press, at(x, y)); pause(ms)
        sc.sendPointerEvent(PointerEventType.Release, at(x, y)); pause(120)
    }
    /** 점들을 [ms] 동안 이어서 드래그 */
    fun drag(points: List<Pair<Float, Float>>, ms: Long) {
        val (x0, y0) = points.first()
        sc.sendPointerEvent(PointerEventType.Press, at(x0, y0)); frame()
        val steps = (ms / 16).toInt().coerceAtLeast(points.size)
        for (k in 1..steps) {
            val t = k.toFloat() / steps * (points.size - 1)
            val i = t.toInt().coerceAtMost(points.size - 2)
            val f = t - i
            val (ax, ay) = points[i]; val (bx, by) = points[i + 1]
            sc.sendPointerEvent(PointerEventType.Move, at(ax + (bx - ax) * f, ay + (by - ay) * f)); frame()
        }
        val (x1, y1) = points.last()
        sc.sendPointerEvent(PointerEventType.Release, at(x1, y1)); pause(120)
    }
}

private fun shoot(out: File, s: Scene) {
    val d = 2f
    ImageComposeScene((s.w * d).toInt(), (s.h * d).toInt(), Density(d)) {
        AnimalRaceTheme { Box(Modifier.fillMaxSize().background(Pal.Ivory)) { s.content() } }
    }.use { sc ->
        s.script?.let { Act(sc, out, s.name, d).it(); return }
        val start = System.nanoTime()
        s.snaps.forEach { ms ->
            while ((System.nanoTime() - start) / 1_000_000 < ms) { sc.render(System.nanoTime() - start); Thread.sleep(16) }
            val file = File(out, if (s.snaps.size == 1) "${s.name}.png" else "${s.name}-${ms}ms.png")
            file.writeBytes(sc.render(System.nanoTime() - start).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            println(file.path)
        }
    }
}

internal object FakeRepo : AppRepository {
    var history: List<RaceRecord> = emptyList()
    override fun loadSetup(): SavedSetup? = null
    override fun saveSetup(setup: SavedSetup) {}
    override fun loadHistory(): List<RaceRecord> = history
    override fun saveHistory(history: List<RaceRecord>) {}
}

/** 동물 20마리 × 한 가지 연출 (0.6초 지점) */
private fun sheet(name: String, fx: Fx) = Scene(name, w = 120 * 5 + 24, h = 140 * 4 + 24) {
    Column(Modifier.padding(12.dp)) {
        Animal.entries.chunked(5).forEach { row ->
            Row {
                row.forEach { a ->
                    Column(Modifier.width(120.dp).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimalSprite(a, 104.dp, fx = fx, fxTime = 0.6f)
                        Text(a.label, fontSize = 14.sp, color = Pal.Ink)
                    }
                }
            }
        }
    }
}

internal fun results(vararg a: Animal) = a.mapIndexed { i, x -> RaceResult(x, 30.0 + i * 0.6, 1, lane = i) }

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
        Scene("crowns", w = 120 * 5 + 24, h = 140 * 4 + 24) {
            // 경주 1등 왕관 자리 확인 (RaceScreen과 같은 계산)
            Column(Modifier.padding(12.dp)) {
                Animal.entries.chunked(5).forEach { row ->
                    Row {
                        row.forEach { a ->
                            Column(Modifier.width(120.dp).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box {
                                    AnimalSprite(a, 104.dp)
                                    val head = headTop(a)
                                    Crown(Modifier.offset(x = 104.dp * (head.x / 200f) - 15.dp, y = 104.dp * (head.y / 200f) - 19.dp), width = 30.dp)
                                }
                                Text(a.label, fontSize = 14.sp, color = Pal.Ink)
                            }
                        }
                    }
                }
            }
        },
        sheet("skills", Fx.SKILL),
        sheet("skills-alt", Fx.SKILL_ALT),
        Scene("race", snaps = listOf(1500, 7000, 13000, 20000, 25000, 29000)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.TURTLE, Animal.PIG), "커피 쏘기", 0, { _, _ -> })
        },
        Scene("roulette", snaps = listOf(900, 2600, 4200)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG), "커피 쏘기", 0, { _, _ -> }, picks = listOf(2, 5))
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
