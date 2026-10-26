package com.example.animalrace.ui.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.MiniGameResult
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
fun ResultScreen(
    record: RaceRecord,
    onAgain: () -> Unit,
    onHome: () -> Unit,
    lastGame: MiniGame = MiniGame.LADDER,
    onMiniGame: (MiniGame?) -> Unit = {},
) {
    var picker by remember { mutableStateOf(false) }
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
            item {
                val picks = record.picks
                if (picks == null) PenaltyCard(record.bet, loser, lane(loser))
                else PickCard(record.bet, picks, record.losers, record.miniGame, onMiniGame = { picker = true })
            }
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
                    results.forEachIndexed { i, r ->
                        val hit = record.picks?.contains(i + 1) == true
                        RecordRow(i, r, lane(r), winner.time, last = record.picks == null && i == results.lastIndex, hit = hit)
                    }
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

        AnimatedVisibility(picker, enter = fadeIn(), exit = fadeOut()) {
            GamePicker(record.losers, lastGame, onDismiss = { picker = false }) { picker = false; onMiniGame(it) }
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

/** 랜덤 등수 모드의 당첨 카드: 뽑힌 등수마다 그 등수로 들어온 동물을 한 줄씩. */
@Composable
private fun PickCard(bet: String, picks: List<Int>, losers: List<RaceResult>, miniGame: MiniGameResult?, onMiniGame: () -> Unit) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(losers) {
        delay(600)
        repeat(4) { shake.animateTo(7f, tween(55)); shake.animateTo(-7f, tween(55)) }
        shake.animateTo(0f, spring())
    }
    Box(Modifier.padding(bottom = 6.dp)) {
        Box(Modifier.matchParentSize().offset(y = 6.dp).clip(RoundedCornerShape(24.dp)).background(Pal.Ink))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Pal.Pink)
                .border(3.dp, Pal.Ink, RoundedCornerShape(24.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("🎯 랜덤 등수 당첨!", Pal.Tomato, fontSize = 14.sp)
                Text(shortBet(bet).ifEmpty { "오늘의 당첨" }, fontSize = 24.sp, color = Pal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // 많이 걸리면 줄을 낮게
            val sprite = if (losers.size > 3) 48.dp else 70.dp
            picks.zip(losers).forEach { (place, r) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Pal.Paper)
                        .border(2.dp, Pal.Ink, RoundedCornerShape(16.dp))
                        .padding(start = 4.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnimalSprite(r.animal, sprite, modifier = Modifier.graphicsLayer { translationX = shake.value * density; rotationZ = shake.value })
                    Tag("${place}등", Pal.Sun, Pal.Ink, fontSize = 15.sp)
                    LaneSticker(r.lane, size = 22.dp)
                    Text("${withRo(r.animal.label)} 출전한 사람!", fontSize = 15.sp, color = Pal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // 2명 이상 걸렸으면 몰아주기 (이미 했으면 결과)
            if (miniGame != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Pal.Tomato)
                        .border(2.dp, Pal.Ink, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("💸 몰빵", fontSize = 16.sp, color = Color.White)
                    Text("${miniGame.game.emoji} ${miniGame.game.title} → ${miniGame.loser.label}", fontSize = 16.sp, color = Color.White, maxLines = 1)
                }
            } else if (losers.size >= 2) {
                CartoonButton(onClick = onMiniGame, color = Pal.Sun, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), height = 52.dp, depth = 4.dp) {
                    Text("💸 몰아주기!", fontSize = 21.sp, color = Pal.Ink)
                    Spacer(Modifier.width(8.dp))
                    Text("한 명에게 몰빵", fontSize = 14.sp, color = Pal.InkSoft)
                }
            }
        }
    }
}

/** 몰아주기 게임 고르기 (바텀 시트). 바깥을 누르면 닫힌다. */
@Composable
private fun GamePicker(players: List<RaceResult>, lastGame: MiniGame, onDismiss: () -> Unit, onPick: (MiniGame?) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("누구에게 몰아줄까?", fontSize = 24.sp, color = Pal.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                players.forEach { p ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Pal.Pink)
                            .border(2.dp, Pal.Ink, RoundedCornerShape(12.dp))
                            .padding(start = 3.dp, end = 8.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LaneSticker(p.lane, size = 18.dp)
                        Text(p.animal.label, fontSize = 14.sp, color = Pal.Ink)
                    }
                }
            }
            // 게임들 뒤에 [아무거나]를 붙여 두 칸씩
            (MiniGame.entries + null).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { g ->
                        if (g != null) GameTile(g.emoji, g.title, g.desc, selected = g == lastGame, modifier = Modifier.weight(1f)) { onPick(g) }
                        else GameTile("🎲", "아무거나", "${MiniGame.entries.size}개 중 하나를 랜덤으로", selected = false, modifier = Modifier.weight(1f)) { onPick(null) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameTile(icon: String, title: String, desc: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier) {
        Box(Modifier.matchParentSize().offset(y = 4.dp).clip(RoundedCornerShape(18.dp)).background(Pal.Ink))
        Column(
            Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) Pal.SunPale else Pal.Paper)
                .border(3.dp, Pal.Ink, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(icon, fontSize = 38.sp)
            Text(title, fontSize = 19.sp, color = Pal.Ink)
            Text(desc, fontSize = 13.sp, color = Pal.InkSoft, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
private fun RecordRow(i: Int, r: RaceResult, lane: Int, winnerTime: Double, last: Boolean, hit: Boolean = false) {
    val first = i == 0
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (last || hit) Pal.Pink else if (first) Pal.SunPale else Pal.Paper)
            .border(2.dp, if (first || last || hit) Pal.Ink else Pal.Line, RoundedCornerShape(14.dp))
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "${i + 1}", style = NumberStyle, fontSize = 20.sp, modifier = Modifier.width(30.dp),
            color = if (last || hit) Color(0xFFD23A26) else if (first) Color(0xFFC98A00) else Pal.Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box {
            AnimalSprite(r.animal, 50.dp)
            LaneSticker(lane, Modifier.offset(x = (-4).dp, y = (-4).dp), size = 18.dp)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(r.animal.label, fontSize = 17.sp, color = Pal.Ink)
                if (hit) Tag("당첨", Pal.Tomato, fontSize = 11.sp)
            }
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
