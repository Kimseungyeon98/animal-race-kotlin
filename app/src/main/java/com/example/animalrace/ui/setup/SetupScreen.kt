package com.example.animalrace.ui.setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.AppState
import com.example.animalrace.model.Animal
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.ui.components.AnimalBadge
import com.example.animalrace.ui.components.BigButton
import com.example.animalrace.ui.components.Checkers
import com.example.animalrace.ui.components.LaneBadge
import com.example.animalrace.ui.components.Pill
import com.example.animalrace.ui.theme.DisplayFont
import com.example.animalrace.ui.theme.LocalRaceColors
import com.example.animalrace.ui.theme.NumberStyle
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max
import kotlin.math.sin

private val FullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

private val BetPresets = listOf(
    "꼴찌가 커피 쏘기", "꼴찌가 점심 쏘기", "꼴찌가 간식 사기", "꼴찌가 회의록 쓰기",
)

@Composable
fun SetupScreen(state: AppState) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            StartBar(state) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.startRace()
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = FullSpan) { Hero() }
            item(span = FullSpan) { BetCard(state.bet, state::updateBet) }
            item(span = FullSpan) {
                SectionHeader(
                    title = "출전 동물",
                    caption = "각자 응원할 동물을 고르세요. 고른 순서가 출전 번호예요.",
                ) {
                    Pill("${state.selected.size} / ${AppState.MAX_RACERS}",
                        MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, mono = true)
                    IconButton(onClick = { state.pickRandom(max(state.selected.size, 4)) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "랜덤으로 고르기")
                    }
                    IconButton(onClick = { state.clearSelection() }, enabled = state.selected.isNotEmpty()) {
                        Icon(Icons.Default.Close, contentDescription = "선택 해제")
                    }
                }
            }
            items(Animal.entries, key = { it.name }) { animal ->
                AnimalCard(animal, lane = state.selected.indexOf(animal)) {
                    if (state.toggle(animal)) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else {
                        scope.launch { snackbar.showSnackbar("최대 ${AppState.MAX_RACERS}마리까지 출전할 수 있어요") }
                    }
                }
            }
            item(span = FullSpan) {
                SectionHeader(title = "최근 경기", caption = "최근 ${AppState.HISTORY_SIZE}판까지 저장돼요.") {
                    if (state.history.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) { Text("기록 지우기") }
                    }
                }
            }
            if (state.history.isEmpty()) {
                item(span = FullSpan) { EmptyHistory() }
            } else {
                itemsIndexed(state.history, key = { _, r -> r.at }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { i, record ->
                    HistoryCard(number = state.history.size - i, record = record)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("기록을 모두 지울까요?") },
            text = { Text("최근 경기 ${state.history.size}판이 삭제되고 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = { state.clearHistory(); confirmClear = false }) {
                    Text("지우기", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("취소") } },
        )
    }
}

/** 잔디 위를 동물들이 줄지어 달리는 헤더. */
@Composable
private fun Hero() {
    val race = LocalRaceColors.current
    val loop = rememberInfiniteTransition(label = "parade")
    val progress by loop.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "progress",
    )
    val parade = listOf(Animal.HORSE, Animal.SNAIL, Animal.RABBIT, Animal.PENGUIN, Animal.CHEETAH, Animal.TURTLE)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(race.turfLight, race.turfEdge)))
            .drawBehind {
                // 잔디 깎은 대각선 무늬
                val step = 28.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = 0.05f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = step / 2)
                    x += step
                }
                // 바깥 레일
                drawLine(race.rail.copy(alpha = 0.8f), Offset(0f, size.height - 44.dp.toPx()),
                    Offset(size.width, size.height - 44.dp.toPx()), strokeWidth = 2.dp.toPx())
            },
    ) {
        val width = maxWidth
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 64.dp)) {
            Text("BOKBULBOK DERBY", color = Color.White.copy(alpha = 0.7f),
                style = NumberStyle, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text("복불복\n동물 레이스", color = Color.White, fontFamily = DisplayFont, fontSize = 40.sp, lineHeight = 44.sp)
            Spacer(Modifier.height(8.dp))
            Text("누가 꼴찌일지는 결승선까지 아무도 몰라요.", color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium)
        }
        Checkers(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp)
                .width(12.dp)
                .height(44.dp),
        )
        parade.forEachIndexed { i, animal ->
            val p = (progress + i / parade.size.toFloat()) % 1f
            val bob = sin((progress * 60 + i) * 3.0).toFloat() * 3f
            Text(
                animal.emoji, fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (width + 40.dp) * p - 40.dp, y = (-8).dp)
                    .graphicsLayer { scaleX = -1f; translationY = bob * density },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BetCard(bet: String, onChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("꼴찌 벌칙", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = bet,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("예: 꼴찌가 커피 쏘기") },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BetPresets.forEach { preset ->
                    SuggestionChip(onClick = { onChange(preset) }, label = { Text(preset.removePrefix("꼴찌가 ")) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, caption: String, actions: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

@Composable
private fun AnimalCard(animal: Animal, lane: Int, onClick: () -> Unit) {
    val selected = lane >= 0
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerLowest, label = "container")
    val borderColor by animateColorAsState(if (selected) scheme.primary else scheme.outlineVariant, label = "border")
    val elevation by animateDpAsState(if (selected) 6.dp else 0.dp, label = "elevation")
    // 해제 애니메이션 동안에도 번호가 유지되도록 마지막 번호를 기억
    val lastLane = remember { IntArray(1) }
    if (selected) lastLane[0] = lane

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimalBadge(animal, size = 54.dp)
                Text(animal.label, style = MaterialTheme.typography.titleMedium)
                Pill(animal.skill, Color(animal.color).copy(alpha = 0.18f), scheme.onSurface)
                Text(
                    animal.desc, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                    minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            // Card 안은 ColumnScope라서 범위 없는 버전을 명시적으로 호출
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                LaneBadge(lastLane[0], size = 30.dp)
            }
        }
    }
}

@Composable
private fun StartBar(state: AppState, onStart: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 12.dp) {
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.canStart) "${state.selected.size}마리 출전 준비 완료" else "2마리 이상 골라 주세요",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    state.selected.joinToString(" ") { it.emoji }.ifEmpty { "아직 선택한 동물이 없어요" },
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(12.dp))
            BigButton(onClick = onStart, enabled = state.canStart) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("출발!", fontFamily = DisplayFont, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(vertical = 28.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🏟️", fontSize = 28.sp)
            Text("아직 경기 기록이 없어요", style = MaterialTheme.typography.titleSmall)
            Text("동물을 고르고 출발!을 누르면 여기에 쌓여요.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCard(number: Int, record: RaceRecord) {
    val scheme = MaterialTheme.colorScheme
    val race = LocalRaceColors.current
    val loser = record.results.last().animal
    Surface(shape = RoundedCornerShape(18.dp), color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("#$number", scheme.surfaceVariant, scheme.onSurfaceVariant, mono = true)
                Text(
                    "${formatTime(record.at)} · ${record.results.size}마리 · 역전 ${record.leadChanges}회",
                    style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                )
            }
            if (record.bet.isNotEmpty()) {
                Text(
                    buildAnnotatedString {
                        append(record.bet)
                        append("  →  ")
                        withStyle(SpanStyle(color = race.penalty, fontWeight = FontWeight.Bold)) {
                            append("${loser.emoji} ${loser.label}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                record.results.forEachIndexed { i, r ->
                    val last = i == record.results.lastIndex
                    val bg = when {
                        i == 0 -> race.gold.copy(alpha = 0.22f)
                        last -> race.penaltyContainer
                        else -> scheme.surfaceVariant
                    }
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i + 1}", style = NumberStyle, fontSize = 12.sp,
                            color = if (last) race.penalty else scheme.onSurfaceVariant)
                        Spacer(Modifier.size(4.dp))
                        Text(r.animal.emoji, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private fun formatTime(at: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = at }
    return "%d/%d %02d:%02d".format(
        c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
    )
}
