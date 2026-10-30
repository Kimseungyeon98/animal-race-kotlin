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
import com.example.animalrace.model.MiniGameResult
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
import com.example.animalrace.ui.setup.RankSheet
import com.example.animalrace.ui.setup.NameSheet
import com.example.animalrace.ui.setup.HistorySheet
import com.example.animalrace.ui.result.ShareSheet
import com.example.animalrace.model.RankMode
import com.example.animalrace.model.RankSetting
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
        Scene("venue-trip", w = 170 * 4 + 24, h = 200 * 4 + 24) {
            // 경기장별 혼자 넘어짐: 줄마다 경기장, 칸마다 시간 0.3·0.8·1.3·1.8초
            Column(Modifier.padding(12.dp)) {
                Venue.entries.forEach { v ->
                    Row {
                        listOf(0.3f, 0.8f, 1.3f, 1.8f).forEach { tm ->
                            Box(Modifier.width(170.dp).padding(top = 40.dp, start = 20.dp)) {
                                AnimalSprite(Animal.RABBIT, 130.dp, fx = Fx.TRIP, fxTime = tm, venue = v)
                            }
                        }
                    }
                }
            }
        },
        Scene("tornado", w = 4 * 260, h = 330, snaps = listOf(1000)) {
            // 사막 혼자 넘어짐 (회오리) 0.3·0.9·1.5·2.1초
            Row(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFE9B979))) {
                listOf(0.3f, 0.9f, 1.5f, 2.1f).forEach { tm ->
                    Box(Modifier.width(260.dp).padding(start = 50.dp, top = 150.dp)) {
                        AnimalSprite(Animal.RABBIT, 100.dp, fx = Fx.TRIP, fxTime = tm, venue = Venue.DESERT)
                    }
                }
            }
        },
        Scene("wave", w = 6 * 220, h = 300, snaps = listOf(1000)) {
            // 해변 혼자 넘어짐 (파도) 한 주기(1.8초): 솟음 → 덮침 → 부서짐 → 젖음
            Row(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF3DDA8))) {
                listOf(0.15f, 0.45f, 0.75f, 0.95f, 1.2f, 1.5f).forEach { tm ->
                    Box(Modifier.width(220.dp).padding(start = 40.dp, top = 120.dp)) {
                        AnimalSprite(Animal.RABBIT, 100.dp, fx = Fx.TRIP, fxTime = tm, venue = Venue.BEACH)
                    }
                }
            }
        },
        Scene("snow", w = 6 * 220, h = 300, snaps = listOf(1000)) {
            // 설원 혼자 넘어짐 1.8초: 미끄덩 → 눈더미에 푹 → 버둥
            Row(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFE6F2FA))) {
                listOf(0.15f, 0.45f, 0.75f, 0.95f, 1.2f, 1.6f).forEach { tm ->
                    Box(Modifier.width(220.dp).padding(start = 40.dp, top = 120.dp)) {
                        AnimalSprite(Animal.RABBIT, 100.dp, fx = Fx.TRIP, fxTime = tm, venue = Venue.SNOW)
                    }
                }
            }
        },
        Scene("meadow", w = 6 * 220, h = 300, snaps = listOf(1000)) {
            // 초원 혼자 넘어짐 1.8초: 돌부리에 걸림 → 데굴 → 꽈당
            Row(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFD9EFC4))) {
                listOf(0.15f, 0.45f, 0.75f, 0.95f, 1.2f, 1.6f).forEach { tm ->
                    Box(Modifier.width(220.dp).padding(start = 40.dp, top = 120.dp)) {
                        AnimalSprite(Animal.RABBIT, 100.dp, fx = Fx.TRIP, fxTime = tm, venue = Venue.MEADOW)
                    }
                }
            }
        },
        *listOf("duo" to listOf(Animal.PIG, Animal.KANGAROO), "trio" to listOf(Animal.CHEETAH, Animal.PENGUIN, Animal.PANDA), "birds" to listOf(Animal.DUCK, Animal.CHICKEN)).map { (name, list) ->
        Scene(name, w = 4 * 230 + 20, h = list.size * 210 + 20) {
            // 크게: 서 있기 + 달리기 자세 3개
            Column(Modifier.padding(10.dp)) {
                list.forEach { a ->
                    Row {
                        AnimalSprite(a, 220.dp, modifier = Modifier.padding(5.dp))
                        (0..2).forEach { f -> AnimalSprite(a, 220.dp, frame = f, modifier = Modifier.padding(5.dp)) }
                    }
                }
            }
        } }.toTypedArray(),
        Scene("review", w = 5 * 2 * 170 + 20, h = 4 * 175 + 20) {
            // 전체 검토: 동물마다 서 있기 + 달리기 자세
            Column(Modifier.padding(10.dp)) {
                Animal.entries.chunked(5).forEach { row ->
                    Row {
                        row.forEach { a ->
                            AnimalSprite(a, 160.dp, modifier = Modifier.padding(5.dp))
                            AnimalSprite(a, 160.dp, frame = 1, modifier = Modifier.padding(5.dp))
                        }
                    }
                }
            }
        },
        Scene("faces", w = 3 * 200 + 20 + 4 * 70, h = 2 * 200 + 20) {
            // 판다·나무늘보 옆얼굴: 서 있기 + 달리기 2 + 실제 크기로 다른 동물과 나란히
            Column(Modifier.padding(10.dp)) {
                listOf(Animal.PANDA, Animal.SLOTH).forEach { a ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimalSprite(a, 190.dp, modifier = Modifier.padding(5.dp))
                        (1..2).forEach { f -> AnimalSprite(a, 190.dp, frame = f, modifier = Modifier.padding(5.dp)) }
                        listOf(a, Animal.CAPYBARA, Animal.HEDGEHOG, Animal.DOG).forEach { o -> AnimalSprite(o, 64.dp, modifier = Modifier.padding(3.dp)) }
                    }
                }
            }
        },
        Scene("hedgehog", w = 3 * 200 + 20 + 4 * 70, h = 200) {
            // 고슴도치: 서 있기 + 달리기 2 + 실제 크기로 햄스터·카피바라·토끼와 나란히
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Bottom) {
                AnimalSprite(Animal.HEDGEHOG, 190.dp, modifier = Modifier.padding(5.dp))
                (1..2).forEach { f -> AnimalSprite(Animal.HEDGEHOG, 190.dp, frame = f, modifier = Modifier.padding(5.dp)) }
                listOf(Animal.HEDGEHOG, Animal.HAMSTER, Animal.CAPYBARA, Animal.RABBIT).forEach { a -> AnimalSprite(a, 64.dp, modifier = Modifier.padding(3.dp)) }
            }
        },
        Scene("hamster", w = 3 * 200 + 20 + 3 * 70, h = 200) {
            // 햄스터: 서 있기 + 달리기 2 + 실제 크기로 토끼·돼지와 나란히
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Bottom) {
                AnimalSprite(Animal.HAMSTER, 190.dp, modifier = Modifier.padding(5.dp))
                (1..2).forEach { f -> AnimalSprite(Animal.HAMSTER, 190.dp, frame = f, modifier = Modifier.padding(5.dp)) }
                listOf(Animal.HAMSTER, Animal.RABBIT, Animal.PIG).forEach { a -> AnimalSprite(a, 64.dp, modifier = Modifier.padding(3.dp)) }
            }
        },
        // ---- 4.0 사람이 주인공
        Scene("people-setup") {
            val state = AppState(FakeRepo).apply {
                listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PIG).forEach { toggle(it) }
                updateNames(mapOf(Animal.HORSE to "민수", Animal.CAT to "지은", Animal.RABBIT to "현우", Animal.PIG to "서연"))
            }
            SetupScreen(state)
        },
        Scene("people-sheet") {
            val state = AppState(FakeRepo).apply {
                listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PIG).forEach { toggle(it) }
                updateNames(mapOf(Animal.CAT to "하린", Animal.RABBIT to "도윤"))
                updateNames(mapOf(Animal.HORSE to "민수", Animal.CAT to "지은", Animal.RABBIT to "현우"))
                saveGroup(listOf("민수", "지은", "현우", "서연")); saveGroup(listOf("아빠", "엄마", "도윤"))
            }
            Box(Modifier.fillMaxSize()) { SetupScreen(state); NameSheet(state) {} }
        },
        Scene("people-race", snaps = listOf(1500, 9000)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PIG), "커피 쏘기", 0, { _, _ -> },
                names = mapOf(Animal.HORSE to "민수", Animal.CAT to "지은", Animal.RABBIT to "현우", Animal.PIG to "서연"))
        },
        Scene("people-result") {
            val who = listOf("지은", "민수", "현우", "서연")
            ResultScreen(
                RaceRecord(0L, "꼴찌가 커피 쏘기", 3, results(Animal.CAT, Animal.HORSE, Animal.RABBIT, Animal.PIG).mapIndexed { i, r -> r.copy(who = who[i]) }, Venue.MEADOW),
                onAgain = {}, onHome = {},
            )
        },
        Scene("people-picks") {
            val who = listOf("지은", "민수", "현우", "서연", "도윤")
            ResultScreen(
                RaceRecord(0L, "커피 쏘기", 3, results(Animal.CAT, Animal.HORSE, Animal.RABBIT, Animal.PIG, Animal.DOG).mapIndexed { i, r -> r.copy(who = who[i]) },
                    Venue.BEACH, listOf(2, 4)),
                onAgain = {}, onHome = {},
            )
        },
        // 복권 3바퀴 (5명부터 2바퀴): 사람 수에 따라 두 칸 / 세 칸 / 네 칸 작은 복권
        *listOf(2, 3, 4, 5, 8).map { k ->
            Scene("lotto-$k") {
                val names = listOf("지은", "서연", "도윤", "민수", "현우", "하린", "재석", "준하")
                val players = results(Animal.CAT, Animal.PIG, Animal.PANDA, Animal.HORSE, Animal.RABBIT, Animal.DOG, Animal.HAMSTER, Animal.DUCK)
                    .take(k).mapIndexed { i, r -> r.copy(who = names[i], lane = i) }
                MiniGameScreen(MiniGameSession(MiniGame.LOTTO, players, Ladder.random(k), k - 1, 0), "커피 쏘기", {}, {})
            }
        }.toTypedArray(),
        // 캡슐 3바퀴 (5명부터 2바퀴): 남은 캡슐 줄은 9개까지 한 줄
        *listOf(2, 3, 5, 8).map { k ->
            Scene("gacha-$k") {
                val names = listOf("지은", "서연", "도윤", "민수", "현우", "하린", "재석", "준하")
                val players = results(Animal.CAT, Animal.PIG, Animal.PANDA, Animal.HORSE, Animal.RABBIT, Animal.DOG, Animal.HAMSTER, Animal.DUCK)
                    .take(k).mapIndexed { i, r -> r.copy(who = names[i], lane = i) }
                MiniGameScreen(MiniGameSession(MiniGame.GACHA, players, Ladder.random(k), k - 1, 0), "커피 쏘기", {}, {})
            }
        }.toTypedArray(),
        // 순서는 자유, 이름 누르고 하기 (4.9): 처음 화면 "누가 고를래요? / 돌릴래요?" (이름 누르기는 t-lotto·t-gacha-who)
        *listOf("free-lotto", "free-gacha").map { name ->
            Scene(name) {
                val players = results(Animal.CAT, Animal.PIG, Animal.PANDA).mapIndexed { i, r -> r.copy(who = listOf("지은", "서연", "도윤")[i], lane = i) }
                MiniGameScreen(MiniGameSession(if (name == "free-lotto") MiniGame.LOTTO else MiniGame.GACHA, players, Ladder.random(3), 2, 0), "커피 쏘기", {}, {})
            }
        }.toTypedArray(),
        Scene("people-lotto") {
            val players = results(Animal.CAT, Animal.PIG, Animal.PANDA).mapIndexed { i, r -> r.copy(who = listOf("지은", "서연", "도윤")[i]) }
            MiniGameScreen(MiniGameSession(MiniGame.LOTTO, players, Ladder.random(3), 1), "커피 쏘기", {}, {})
        },
        // ---- 4.1 결과 공유 카드
        Scene("share-penalty") {
            val who = listOf("지은", "민수", "현우", "서연")
            val rec = RaceRecord(0L, "꼴찌가 커피 쏘기", 3, results(Animal.CAT, Animal.HORSE, Animal.RABBIT, Animal.PIG).mapIndexed { i, r -> r.copy(who = who[i]) }, Venue.MEADOW)
            Box(Modifier.fillMaxSize()) { ResultScreen(rec, onAgain = {}, onHome = {}); ShareSheet(rec) {} }
        },
        Scene("share-mini") {
            val who = listOf("지은", "민수", "현우", "서연", "도윤")
            val rs = results(Animal.CAT, Animal.HORSE, Animal.RABBIT, Animal.PIG, Animal.DOG).mapIndexed { i, r -> r.copy(who = who[i]) }
            val rec = RaceRecord(0L, "치킨 사기", 2, rs, Venue.BEACH, listOf(2, 4), MiniGameResult(MiniGame.LOTTO, Animal.PIG, "서연"))
            Box(Modifier.fillMaxSize()) { ResultScreen(rec, onAgain = {}, onHome = {}); ShareSheet(rec) {} }
        },
        Scene("share-picks") {
            val rec = RaceRecord(0L, "설거지", 1, results(Animal.PENGUIN, Animal.CAT, Animal.RABBIT, Animal.TURTLE), Venue.SNOW, listOf(1, 3))
            Box(Modifier.fillMaxSize()) { ResultScreen(rec, onAgain = {}, onHome = {}); ShareSheet(rec) {} }
        },
        Scene("share-desert") {
            val rec = RaceRecord(0L, "", 1, results(Animal.CHEETAH, Animal.DOG, Animal.MONKEY), Venue.DESERT)
            Box(Modifier.fillMaxSize()) { ResultScreen(rec, onAgain = {}, onHome = {}); ShareSheet(rec) {} }
        },
        // ---- 4.2 우리 기록
        *listOf("ours", "ours-empty", "t-delete").map { name ->
            Scene(name, script = if (name != "t-delete") null else ({
                // 4.10: #8 카드의 🗑 → "지웠어요 · 되돌리기" → 되돌리기
                pause(900); tap(346f, 379f); pause(500); snap("1-deleted")
                tap(337f, 204f); pause(500); snap("2-undone")
            })) {
                val state = AppState(FakeRepo).apply {
                    saveGroup(listOf("민수", "지은", "현우", "서연"))
                    if (name != "ours-empty") {
                        val people = listOf("민수", "지은", "현우", "서연")
                        val animals = listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PIG)
                        val order = listOf(listOf(1, 0, 2, 3), listOf(0, 2, 1, 3), listOf(2, 1, 0, 3), listOf(1, 3, 0, 2), listOf(0, 1, 3, 2),
                            listOf(3, 0, 1, 2), listOf(1, 2, 3, 0), listOf(0, 3, 2, 1), listOf(2, 0, 1, 3))
                        val now = System.currentTimeMillis()
                        val bets = listOf("꼴찌가 커피 쏘기", "아이스크림", "설거지")
                        history.addAll(order.mapIndexed { k, o ->
                            RaceRecord(now - k * 3_600_000L, bets[k % 3], 1, o.mapIndexed { i, p -> RaceResult(animals[p], 30.0 + i, 0, p, people[p]) })
                        })
                    }
                }
                Box(Modifier.fillMaxSize()) { SetupScreen(state); HistorySheet(state) {} }
            }
        }.toTypedArray(),
        sheet("skills", Fx.SKILL),
        sheet("skills-alt", Fx.SKILL_ALT),
        Scene("race", snaps = listOf(1500, 7000, 13000, 20000, 25000, 29000)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.TURTLE, Animal.PIG), "커피 쏘기", 0, { _, _ -> })
        },
        // 랜덤 등수 판: 당첨 등수 자리를 따라가는 카메라 + "N등 당첨!" 카드 (기기 속도에 따라 경기 시간이 달라서 촘촘히 찍음)
        Scene("pick-focus", snaps = (6..40).map { it * 1000L }) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG), "커피 쏘기", 0, { _, _ -> },
                picks = listOf(3, 5), picksFixed = true,
                names = mapOf(Animal.HORSE to "민수", Animal.CAT to "지은", Animal.RABBIT to "현우", Animal.PANDA to "서연", Animal.DOG to "도윤", Animal.PIG to "하린"))
        },
        Scene("roulette", snaps = listOf(900, 2600, 4200)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG), "커피 쏘기", 0, { _, _ -> }, picks = listOf(2, 5))
        },
        Scene("setup") {
            val state = AppState(FakeRepo).apply { listOf(Animal.HORSE, Animal.CAT, Animal.PANDA).forEach { toggle(it) } }
            SetupScreen(state)
        },
        Scene("rank-setup") {
            val state = AppState(FakeRepo).apply {
                listOf(Animal.HORSE, Animal.CAT, Animal.PANDA, Animal.DOG, Animal.PIG, Animal.DUCK).forEach { toggle(it) }
                updateRandomRank(true); updateRankSetting(RankSetting(RankMode.FIXED, fixed = listOf(2, 6)))
            }
            SetupScreen(state)
        },
        Scene("rank-empty") {
            val state = AppState(FakeRepo).apply {
                listOf(Animal.HORSE, Animal.CAT, Animal.PANDA).forEach { toggle(it) }
                updateRandomRank(true); updateRankSetting(RankSetting(RankMode.FIXED))
            }
            SetupScreen(state)
        },
        Scene("rank-sheet-roulette") { RankSheet(RankSetting(count = 3), 7, {}, {}) },
        Scene("rank-sheet-fixed") { RankSheet(RankSetting(RankMode.FIXED, fixed = listOf(1, 4, 7)), 10, {}, {}) },
        Scene("rank-fixed", snaps = listOf(500, 1200, 2600)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG), "커피 쏘기", 0, { _, _ -> },
                picks = listOf(2, 5), picksFixed = true)
        },
        Scene("rank-many", snaps = listOf(2500, 6000)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG, Animal.DUCK, Animal.TURTLE, Animal.CHEETAH, Animal.ELEPHANT), "커피 쏘기", 0, { _, _ -> }, picks = listOf(1, 2, 4, 5, 6, 8))
        },
        Scene("rank-nine", snaps = listOf(3000, 8500)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG, Animal.DUCK, Animal.TURTLE, Animal.CHEETAH, Animal.ELEPHANT), "커피 쏘기", 0, { _, _ -> }, picks = (1..9).toList())
        },
        Scene("rank-fixed9", snaps = listOf(1000)) {
            RaceScreen(listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG, Animal.DUCK, Animal.TURTLE, Animal.CHEETAH, Animal.ELEPHANT), "커피 쏘기", 0, { _, _ -> }, picks = (1..9).toList(), picksFixed = true)
        },
        Scene("rank-setup9") {
            val state = AppState(FakeRepo).apply {
                listOf(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG, Animal.DUCK, Animal.TURTLE, Animal.CHEETAH, Animal.ELEPHANT).forEach { toggle(it) }
                updateRandomRank(true); updateRankSetting(RankSetting(RankMode.FIXED, fixed = (1..9).toList()))
            }
            SetupScreen(state)
        },
        Scene("rank-sheet9") { RankSheet(RankSetting(RankMode.FIXED, fixed = (1..9).toList()), 10, {}, {}) },
        Scene("rank-result9") {
            ResultScreen(
                RaceRecord(0L, "커피 쏘기", 3, results(Animal.HORSE, Animal.CAT, Animal.RABBIT, Animal.PANDA, Animal.DOG, Animal.PIG, Animal.DUCK, Animal.TURTLE, Animal.CHEETAH, Animal.ELEPHANT), Venue.MEADOW, (1..9).toList(), picksFixed = true),
                onAgain = {}, onHome = {},
            )
        },
        Scene("lotto7") {
            val many = results(Animal.CAT, Animal.PIG, Animal.PANDA, Animal.DOG, Animal.DUCK, Animal.HORSE, Animal.RABBIT)
            MiniGameScreen(MiniGameSession(MiniGame.LOTTO, many, Ladder.random(7), 3), "커피 쏘기", {}, {})
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
