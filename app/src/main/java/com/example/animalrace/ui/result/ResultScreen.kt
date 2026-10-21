package com.example.animalrace.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.CartoonButton
import com.example.animalrace.ui.components.Crown
import com.example.animalrace.ui.components.Icons2
import com.example.animalrace.ui.components.InkIcon
import com.example.animalrace.ui.components.LaneSticker
import com.example.animalrace.ui.components.OutlinedText
import com.example.animalrace.ui.components.Tag
import com.example.animalrace.ui.components.shortBet
import com.example.animalrace.ui.components.withRo
import com.example.animalrace.ui.theme.Fredoka
import com.example.animalrace.ui.theme.NumberStyle
import com.example.animalrace.ui.theme.Pal
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * 결과: 꼴찌 벌칙 당첨 → 시상대 → 전체 기록.
 */
@Composable
fun ResultScreen(record: RaceRecord, onAgain: () -> Unit, onHome: () -> Unit) {
    val results = record.results
    val winner = results.first()
    val loser = results.last()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(record) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    fun lane(r: RaceResult) = r.lane

    Box(Modifier.fillMaxSize().background(Pal.Sky)) {
        Confetti(Modifier.fillMaxSize())

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 20.dp, bottom = bottomInset + 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedText("경기 결과", 30.sp)
                        record.venue?.let { Tag(it.label, Pal.Paper, Pal.Ink, fontSize = 13.sp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatChip("역전 ", "${record.leadChanges}", "회")
                        StatChip("1등 ", "%.2f".format(winner.time), "초")
                        StatChip("1등과 꼴찌 ", "%.2f".format(loser.time - winner.time), "초 차이")
                    }
                }
            }
            item { PenaltyCard(record.bet, loser, lane(loser)) }
            item { Podium(results, ::lane) }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Pal.Ivory)
                        .border(3.dp, Pal.Ink, RoundedCornerShape(22.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("전체 기록", fontSize = 18.sp, color = Pal.Ink, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp))
                    results.forEachIndexed { i, r -> RecordRow(i, r, lane(r), winner.time, i == results.lastIndex) }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Pal.Grass)) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Pal.Ink))
            Row(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CartoonButton(onClick = onHome, color = Pal.Paper, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    InkIcon(Icons2.Home, size = 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("처음으로", fontSize = 17.sp, color = Pal.Ink)
                }
                CartoonButton(onClick = onAgain, modifier = Modifier.weight(1f)) {
                    InkIcon(Icons2.Again, size = 20.dp, color = Color.White, stroke = 3f)
                    Spacer(Modifier.width(8.dp))
                    OutlinedText("같은 동물로 한 판 더", 20.sp, outlineWidth = 5f)
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, unit: String) {
    Text(
        buildAnnotatedString {
            append(label)
            withStyle(SpanStyle(fontFamily = Fredoka)) { append(value) }
            append(unit)
        },
        fontSize = 14.sp, color = Pal.Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Pal.Paper)
            .border(2.dp, Pal.Ink, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun PenaltyCard(bet: String, loser: RaceResult, lane: Int) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(loser) {
        delay(600)
        repeat(4) { shake.animateTo(7f, tween(55)); shake.animateTo(-7f, tween(55)) }
        shake.animateTo(0f, spring())
    }
    Box(Modifier.padding(bottom = 6.dp)) {
        Box(Modifier.matchParentSize().offset(y = 6.dp).clip(RoundedCornerShape(24.dp)).background(Pal.Ink))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Pal.Pink)
                .border(3.dp, Pal.Ink, RoundedCornerShape(24.dp))
                .padding(start = 6.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AnimalSprite(loser.animal, 110.dp, modifier = Modifier.graphicsLayer { translationX = shake.value * density; rotationZ = shake.value })
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Tag("꼴찌 벌칙 당첨!", Pal.Tomato, fontSize = 14.sp)
                Text(shortBet(bet).ifEmpty { "오늘의 꼴찌" }, fontSize = 26.sp, color = Pal.Ink, lineHeight = 30.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LaneSticker(lane, size = 22.dp)
                    Text("${withRo(loser.animal.label)} 출전한 사람!", fontSize = 15.sp, color = Pal.Ink)
                }
            }
        }
    }
}

@Composable
private fun Podium(results: List<RaceResult>, lane: (RaceResult) -> Int) {
    val slots = listOfNotNull(results.getOrNull(1)?.let { 1 to it }, 0 to results[0], results.getOrNull(2)?.let { 2 to it })
    Box(Modifier.fillMaxWidth().height(270.dp)) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(40.dp).background(Pal.Grass))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 37.dp).fillMaxWidth().height(3.dp).background(Pal.Ink))
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            slots.forEach { (place, r) -> PodiumColumn(place, r, lane(r), Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PodiumColumn(place: Int, r: RaceResult, lane: Int, modifier: Modifier) {
    val color = listOf(Pal.Sun, Pal.Silver, Pal.Bronze)[place]
    val blockH: Dp = listOf(100.dp, 70.dp, 50.dp)[place]
    val rise = remember { Animatable(0f) }
    LaunchedEffect(r) {
        delay(listOf(700L, 350L, 0L)[place])
        rise.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 180f))
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (place == 0) Crown(width = 34.dp)
        AnimalSprite(r.animal, if (place == 0) 108.dp else 96.dp, running = place == 0)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LaneSticker(lane, size = 20.dp)
            Text(r.animal.label, fontSize = if (place == 0) 19.sp else 17.sp, color = Pal.Ink, maxLines = 1)
        }
        Text("%.2f초".format(r.time), style = NumberStyle, fontSize = 13.sp, color = Color(0xFF3E3456))
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(blockH * rise.value.coerceAtLeast(0.02f))
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(color)
                .border(3.dp, Pal.Ink, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            OutlinedText("${place + 1}", if (place == 0) 40.sp else 32.sp, fontFamily = Fredoka, outlineWidth = 6f,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun RecordRow(i: Int, r: RaceResult, lane: Int, winnerTime: Double, last: Boolean) {
    val first = i == 0
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (first) Pal.SunPale else if (last) Pal.Pink else Pal.Paper)
            .border(2.dp, if (first || last) Pal.Ink else Pal.Line, RoundedCornerShape(14.dp))
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "${i + 1}", style = NumberStyle, fontSize = 20.sp, modifier = Modifier.width(30.dp),
            color = if (first) Color(0xFFC98A00) else if (last) Color(0xFFD23A26) else Pal.Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box {
            AnimalSprite(r.animal, 50.dp)
            LaneSticker(lane, Modifier.offset(x = (-4).dp, y = (-4).dp), size = 18.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(r.animal.label, fontSize = 17.sp, color = Pal.Ink)
            Text("${r.animal.skill} ${r.skillUses}회", fontSize = 12.sp, color = Pal.InkSoft)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("%.2f초".format(r.time), style = NumberStyle, fontSize = 16.sp, color = Pal.Ink)
            Text(
                if (first) "우승" else "+%.2f".format(r.time - winnerTime),
                style = NumberStyle, fontSize = 12.sp,
                color = if (first) Color(0xFF9A6B00) else if (last) Pal.TomatoText else Pal.InkSoft,
            )
        }
    }
}

private class Piece(val x: Float, val delay: Float, val fall: Float, val sway: Float, val w: Float, val h: Float, val spin: Float, val color: Color)

/** 꽃가루: 카드 뒤 배경에서만 내려서 글자를 가리지 않는다. 4.5초 뒤 사라짐. */
@Composable
private fun Confetti(modifier: Modifier) {
    val palette = listOf(Pal.Sun, Pal.Tomato, Color(0xFF3FD0A6), Color.White, Color(0xFFB983FF), Color(0xFF5AA9FF))
    val pieces = remember {
        List(60) {
            Piece(Random.nextFloat(), Random.nextFloat() * 1.2f, 0.25f + Random.nextFloat() * 0.25f, Random.nextFloat() * 6f,
                8f + Random.nextFloat() * 6f, 12f + Random.nextFloat() * 6f, 180f + Random.nextFloat() * 360f, palette.random())
        }
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (time < 6f) withFrameNanos { time = (it - start) / 1e9f }
    }
    if (time >= 6f) return
    Canvas(modifier) {
        val fade = 1f - ((time - 5f) / 1f).coerceIn(0f, 1f)
        pieces.forEach { p ->
            val tt = time - p.delay
            if (tt < 0) return@forEach
            val y = -30f + tt * p.fall * size.height
            val x = p.x * size.width + sin(tt * 2.5f + p.sway) * 16.dp.toPx()
            val w = p.w.dp.toPx(); val h = p.h.dp.toPx()
            rotate(tt * p.spin, Offset(x, y)) {
                drawRoundRect(p.color.copy(alpha = fade), Offset(x - w / 2, y - h / 2), Size(w, h),
                    androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                drawRoundRect(Pal.Ink.copy(alpha = fade), Offset(x - w / 2, y - h / 2), Size(w, h),
                    androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }
    }
}
