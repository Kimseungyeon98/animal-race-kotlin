package com.example.animalrace.shot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.AppState
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Fx
import com.example.animalrace.model.Venue
import com.example.animalrace.ui.art.volcanoTripStyle
import com.example.animalrace.ui.race.Stadium
import com.example.animalrace.ui.race.forceStadium
import com.example.animalrace.race.Racer
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.CartoonButton
import com.example.animalrace.ui.components.Icons2
import com.example.animalrace.ui.components.InkIcon
import com.example.animalrace.ui.components.OutlinedText
import com.example.animalrace.ui.components.Tag
import com.example.animalrace.ui.race.Bomb
import com.example.animalrace.ui.race.RaceScreen
import com.example.animalrace.ui.setup.SetupScreen
import com.example.animalrace.ui.theme.NumberStyle
import com.example.animalrace.ui.theme.Pal

/** 시안 전용 장면 (앱 코드 아님): 사진 판정 · 처음 안내. 반영하면 앱 쪽으로 옮기고 이 파일은 지운다. */
internal fun ideaScenes(): List<Scene> {
    val names = mapOf(Animal.HORSE to "민수", Animal.CAT to "지은", Animal.RABBIT to "현우", Animal.PANDA to "서연", Animal.DOG to "도윤", Animal.PIG to "하린")
    return listOf(
        // ① 사진 판정: 당첨 자리와 바로 앞 등수가 간발의 차이일 때 결과 전에 잠깐
        // 화산 넘어지는 장면 두 가지 (0 화산탄 · 1 증기): 줄마다 동물, 칸마다 시간
        *listOf("bomb" to 0, "steam" to 1).map { (n, style) ->
            Scene("idea-vtrip-$n", w = 190 * 7 + 24, h = 160 * 2 + 40) {
                volcanoTripStyle = style
                Column(Modifier.fillMaxSize().background(Stadium.VOLCANO.dirt).padding(12.dp)) {
                    listOf(Animal.RABBIT, Animal.PIG).forEach { a ->
                        Row {
                            listOf(0.15f, 0.4f, 0.6f, 0.85f, 1.1f, 1.4f, 1.8f).forEach { tm ->
                                Box(Modifier.size(190.dp, 160.dp).padding(top = 56.dp, start = 40.dp)) {
                                    AnimalSprite(a, 112.dp, fx = Fx.TRIP, fxTime = tm, venue = Venue.VOLCANO)
                                }
                            }
                        }
                    }
                }
            }
        }.toTypedArray(),
        Scene("idea-vrace", snaps = (4..15).map { it * 1000L }) {
            volcanoTripStyle = 0
            forceStadium = Stadium.VOLCANO
            RaceScreen(names.keys.toList(), "커피 쏘기", 0, { _, _ -> }, names = names)
        },
        Scene("idea-photo", snaps = listOf(30000L)) {
            var last by remember { mutableStateOf<List<Racer>?>(null) }
            Box(Modifier.fillMaxSize()) {
                RaceScreen(names.keys.toList(), "커피 쏘기", 0, { race, _ -> last = race.ranking().takeLast(2) }, names = names)
                last?.let { (safe, hit) -> PhotoFinish(names[safe.animal]!!, safe.animal, names[hit.animal]!!, hit.animal, names.size) }
            }
        },
        // ② 처음 켰을 때 한 장 안내
        Scene("idea-howto") {
            val state = remember { AppState(FakeRepo) }
            Box(Modifier.fillMaxSize()) { SetupScreen(state); HowTo() }
        },
    )
}

// ---------------------------------------------------------------- ① 사진 판정

@Composable
private fun PhotoFinish(safeName: String, safe: Animal, hitName: String, hit: Animal, players: Int) {
    Box(Modifier.fillMaxSize().background(Pal.Ink.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
        Box {
            Column(
                Modifier
                    .padding(top = 16.dp)
                    .width(338.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Pal.Ivory)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(26.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 사진: 결승선에 코를 내민 두 마리
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Pal.Dirt)
                        .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp)),
                ) {
                    val line = 214.dp
                    Canvas(Modifier.fillMaxSize()) {
                        // 레인 줄 + 체커 결승선
                        drawRect(Pal.DirtAlt, Offset(0f, size.height / 2), Size(size.width, size.height / 2))
                        drawLine(Pal.DirtLine, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3.dp.toPx())
                        val sq = 9.dp.toPx()
                        var y = 0f; var k = 0
                        while (y < size.height) {
                            for (c in 0..1) drawRect(if ((k + c) % 2 == 0) Pal.Ink else Color.White, Offset(line.toPx() + c * sq, y), Size(sq, sq))
                            y += sq; k++
                        }
                        // 판정선: 결승선 앞 가장자리
                        drawLine(
                            Pal.Tomato, Offset(line.toPx(), 0f), Offset(line.toPx(), size.height), 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                        )
                    }
                    AnimalSprite(safe, 92.dp, modifier = Modifier.offset(x = line - 70.dp, y = 2.dp), running = false, frame = 1)
                    AnimalSprite(hit, 92.dp, modifier = Modifier.offset(x = line - 98.dp, y = 100.dp), running = false, frame = 2)
                    Tag("${players - 1}등", Pal.Paper, Pal.Ink, Modifier.offset(x = 10.dp, y = 10.dp), fontSize = 14.sp)
                    Tag("꼴찌", Pal.Tomato, Color.White, Modifier.offset(x = 10.dp, y = 108.dp), fontSize = 14.sp)
                    Text(safeName, fontSize = 17.sp, color = Pal.Ink, modifier = Modifier.offset(x = 12.dp, y = 38.dp))
                    Text(hitName, fontSize = 17.sp, color = Pal.TomatoText, modifier = Modifier.offset(x = 12.dp, y = 136.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("코 하나 차이", fontSize = 16.sp, color = Pal.InkSoft)
                    Tag("0.09초", Pal.Sun, Pal.Ink, fontSize = 15.sp, number = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bomb(width = 30.dp, time = 0.2f, urgency = 1f)
                    Text(hitName, fontSize = 28.sp, color = Pal.TomatoText)
                    Text("당첨!", fontSize = 28.sp, color = Pal.Ink)
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .rotate(-4f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pal.Tomato)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) { OutlinedText("사진 판정!", 24.sp, outlineWidth = 6f) }
        }
    }
}

// ---------------------------------------------------------------- ② 처음 안내

@Composable
private fun HowTo() {
    Box(Modifier.fillMaxSize().background(Pal.Ink.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(28.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedText("이렇게 놀아요", 26.sp)
            Step(1, "랜덤 등수 모드", "켜면 룰렛으로 뽑은 등수가 당첨\n끄면 꼴찌가 당첨") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(Pal.Sun).border(2.5.dp, Pal.Ink, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                        InkIcon(Icons2.Check, size = 14.dp, stroke = 3.5f)
                    }
                    Tag("3등", Pal.Tomato, Color.White, fontSize = 14.sp)
                }
            }
            Step(2, "누가 하나요?", "함께할 사람 이름을 넣어요\n이름 없이 동물만 골라도 돼요") {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Pal.Sun)
                        .border(2.dp, Pal.Ink, RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) { Text("민수·지은", fontSize = 13.sp, color = Pal.Ink) }
            }
            Step(3, "벌칙을 정해요", "당첨된 사람이 할 일을 적어요") {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Pal.Pink)
                        .border(2.5.dp, Pal.Ink, RoundedCornerShape(12.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("커피 쏘기", fontSize = 13.sp, color = Pal.Ink)
                    InkIcon(Icons2.Pencil, size = 11.dp)
                }
            }
            Step(4, "이름표를 동물에게", "이름표를 끌어다 원하는 동물에 붙여요") {
                Box(Modifier.fillMaxSize()) {
                    AnimalSprite(Animal.CAT, 44.dp, modifier = Modifier.align(Alignment.BottomEnd))
                    Text(
                        "민수", fontSize = 12.sp, color = Pal.Ink,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .rotate(-8f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Pal.Sun)
                            .border(2.dp, Pal.Ink, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                    InkIcon(listOf("M5 9c5-2 9 0 12 5", "M17 9v5h-5"), Modifier.offset(x = 26.dp, y = 4.dp), size = 18.dp, stroke = 2.6f)
                }
            }
            Step(5, "출발!", "20초 동안 엎치락뒤치락\n어떤 동물이든 확률은 같아요") {
                Box(Modifier.fillMaxSize()) {
                    AnimalSprite(Animal.PIG, 46.dp, modifier = Modifier.align(Alignment.BottomCenter))
                    Bomb(Modifier.align(Alignment.TopEnd).offset(x = (-4).dp), width = 20.dp, time = 0.3f, urgency = 0.5f)
                }
            }
            CartoonButton(onClick = {}, modifier = Modifier.fillMaxWidth(), height = 54.dp) {
                OutlinedText("시작하기", 22.sp, outlineWidth = 6f)
            }
            Text("ⓘ 정보에서 다시 볼 수 있어요", fontSize = 12.sp, color = Pal.InkSoft)
        }
    }
}

@Composable
private fun Step(n: Int, title: String, sub: String, art: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Pal.Paper)
            .border(2.5.dp, Pal.Ink, RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(84.dp, 56.dp).clip(RoundedCornerShape(12.dp)).background(Pal.Cream).padding(4.dp),
            contentAlignment = Alignment.Center,
        ) { art() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(Pal.Sun).border(2.dp, Pal.Ink, CircleShape), contentAlignment = Alignment.Center) {
                    Text("$n", style = NumberStyle, fontSize = 12.sp, color = Pal.Ink)
                }
                Text(title, fontSize = 17.sp, color = Pal.Ink)
            }
            Text(sub, fontSize = 12.5.sp, lineHeight = 16.sp, color = Pal.InkSoft)
        }
    }
}
