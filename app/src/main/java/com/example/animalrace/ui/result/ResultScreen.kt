package com.example.animalrace.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.race.withObject
import com.example.animalrace.ui.components.AnimalBadge
import com.example.animalrace.ui.components.BigButton
import com.example.animalrace.ui.components.Pill
import com.example.animalrace.ui.theme.DisplayFont
import com.example.animalrace.ui.theme.LocalRaceColors
import com.example.animalrace.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ResultScreen(record: RaceRecord, onAgain: () -> Unit, onHome: () -> Unit) {
    val results = record.results
    val winner = results.first()
    val loser = results.last()
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(record) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    Scaffold(
        containerColor = scheme.background,
        bottomBar = {
            Surface(color = scheme.surfaceContainer, shadowElevation = 12.dp) {
                Row(
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onHome, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("처음으로")
                    }
                    BigButton(onClick = onAgain, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("같은 동물로 한 판 더", fontFamily = DisplayFont, fontSize = 17.sp)
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("최종 순위", style = MaterialTheme.typography.headlineMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("역전 ${record.leadChanges}회", scheme.surfaceContainerHigh, scheme.onSurface)
                            Pill("1착 %.2f초".format(winner.time), scheme.surfaceContainerHigh, scheme.onSurface)
                            Pill("격차 %.2f초".format(loser.time - winner.time), scheme.surfaceContainerHigh, scheme.onSurface)
                        }
                    }
                }
                item { PenaltyCard(record.bet, loser) }
                item { Podium(results) }
                item {
                    Text("전체 기록", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                itemsIndexed(results, key = { _, r -> r.animal.name }) { i, r ->
                    ResultRow(place = i, result = r, winnerTime = winner.time, isLast = i == results.lastIndex)
                }
            }
            Confetti(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PenaltyCard(bet: String, loser: RaceResult) {
    val race = LocalRaceColors.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(loser) {
        delay(900)
        repeat(3) {
            shake.animateTo(10f, tween(55))
            shake.animateTo(-10f, tween(55))
        }
        shake.animateTo(0f, spring())
    }
    Surface(
        shape = RoundedCornerShape(24.dp), color = race.penaltyContainer,
        border = BorderStroke(1.5.dp, race.penalty.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(loser.animal.emoji, fontSize = 48.sp, modifier = Modifier.graphicsLayer {
                translationX = shake.value.dp.toPx(); rotationZ = shake.value; scaleX = -1f
            })
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("꼴찌 벌칙 당첨", style = NumberStyle, fontSize = 12.sp, color = race.penalty, letterSpacing = 1.sp)
                Text(bet.ifEmpty { "오늘의 꼴찌" }, style = MaterialTheme.typography.headlineSmall)
                Text("${withObject(loser.animal.label)} 응원한 사람!", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 2-1-3 배치의 시상대. 3위부터 차례로 솟아오른다. */
@Composable
private fun Podium(results: List<RaceResult>) {
    val slots = listOfNotNull(
        results.getOrNull(1)?.let { 1 to it },
        0 to results[0],
        results.getOrNull(2)?.let { 2 to it },
    )
    Row(
        Modifier.fillMaxWidth().height(260.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        slots.forEach { (place, r) -> PodiumColumn(place, r, Modifier.weight(1f)) }
    }
}

@Composable
private fun PodiumColumn(place: Int, result: RaceResult, modifier: Modifier) {
    val race = LocalRaceColors.current
    val medal = listOf(race.gold, race.silver, race.bronze)[place]
    val blockH = listOf(132.dp, 98.dp, 74.dp)[place]
    val rise = remember { Animatable(0f) }
    LaunchedEffect(result) {
        delay(listOf(700L, 350L, 0L)[place])
        rise.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 180f))
    }
    val bounce = rememberInfiniteTransition(label = "winner")
    val hop by bounce.animateFloat(0f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "hop")

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            result.animal.emoji,
            fontSize = if (place == 0) 52.sp else 40.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = -1f
                alpha = rise.value.coerceIn(0f, 1f)
                translationY = if (place == 0) -hop * 10.dp.toPx() else 0f
            },
        )
        Text(result.animal.label, style = MaterialTheme.typography.titleSmall)
        Text("%.2f초".format(result.time), style = NumberStyle, fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(blockH * rise.value.coerceAtLeast(0f))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Brush.verticalGradient(listOf(medal, medal.copy(alpha = 0.7f)))),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text("${place + 1}", fontFamily = DisplayFont, fontSize = 34.sp, color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ResultRow(place: Int, result: RaceResult, winnerTime: Double, isLast: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val race = LocalRaceColors.current
    val bg = when {
        place == 0 -> race.gold.copy(alpha = 0.16f)
        isLast -> race.penaltyContainer
        else -> scheme.surfaceContainerLowest
    }
    val accent = when {
        place == 0 -> race.gold
        isLast -> race.penalty
        else -> scheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${place + 1}", fontFamily = DisplayFont, fontSize = 22.sp, color = accent, modifier = Modifier.width(34.dp))
        AnimalBadge(result.animal, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(result.animal.label, style = MaterialTheme.typography.titleSmall)
            Text("${result.animal.skill} ${result.skillUses}회", style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("%.2f초".format(result.time), style = NumberStyle, fontSize = 15.sp)
            Text(
                if (place == 0) "우승" else "+%.2f".format(result.time - winnerTime),
                style = NumberStyle, fontSize = 11.sp, color = accent,
            )
        }
    }
}

private class ConfettiPiece(
    val x: Float, val delay: Float, val fall: Float, val sway: Float,
    val w: Float, val h: Float, val spin: Float, val color: Color,
)

/** 우승 축하 꽃가루. 4초 정도 내리고 사라진다. */
@Composable
private fun Confetti(modifier: Modifier) {
    val race = LocalRaceColors.current
    val palette = listOf(race.gold, Color(0xFF4CAF50), Color(0xFF2F80ED), Color(0xFFE94E77), Color.White, race.bronze)
    val pieces = remember {
        List(110) {
            ConfettiPiece(
                x = Random.nextFloat(), delay = Random.nextFloat() * 0.9f, fall = 0.35f + Random.nextFloat() * 0.35f,
                sway = Random.nextFloat() * 6f, w = 5f + Random.nextFloat() * 5f, h = 8f + Random.nextFloat() * 8f,
                spin = 180f + Random.nextFloat() * 540f, color = palette.random(),
            )
        }
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (time < 4.5f) {
            val now = withFrameNanos { it }
            time = (now - start) / 1e9f
        }
    }
    if (time >= 4.5f) return
    Canvas(modifier) {
        pieces.forEach { p ->
            val tt = time - p.delay
            if (tt < 0) return@forEach
            val y = -20f + tt * p.fall * size.height
            val x = p.x * size.width + sin(tt * 2.5f + p.sway) * 18.dp.toPx()
            val w = p.w.dp.toPx() * 0.6f
            val h = p.h.dp.toPx() * 0.6f
            val fade = (1f - ((time - 3.5f) / 1f).coerceIn(0f, 1f))
            rotate(degrees = tt * p.spin, pivot = Offset(x, y)) {
                drawRect(p.color.copy(alpha = fade), Offset(x - w / 2, y - h / 2), Size(w, h))
            }
        }
    }
}
