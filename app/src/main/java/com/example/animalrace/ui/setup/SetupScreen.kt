package com.example.animalrace.ui.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.example.animalrace.ui.components.BigButton
import com.example.animalrace.ui.components.LaneBadge
import com.example.animalrace.ui.components.Pill
import com.example.animalrace.ui.theme.DisplayFont
import com.example.animalrace.ui.theme.LocalRaceColors
import com.example.animalrace.ui.theme.NumberStyle
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max

private val BetPresets = listOf(
    "꼴찌가 커피 쏘기", "꼴찌가 점심 쏘기", "꼴찌가 간식 사기", "꼴찌가 회의록 쓰기",
)

/**
 * 메인 화면. 동물 고르기가 주인공이라 14마리를 스크롤 없이 한 화면에 큰 타일로 보여 준다.
 * 벌칙은 작은 칩, 최근 기록은 아래에서 올라오는 시트로 뺐다.
 */
@Composable
fun SetupScreen(state: AppState) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var showHistory by remember { mutableStateOf(false) }
    var editBet by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(state.selected.lastOrNull()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            StartBar(state, focused) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.startRace()
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopRow(state, onHistory = { showHistory = true })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BetChip(state.bet, Modifier.weight(1f)) { editBet = true }
                Pill("${state.selected.size}/${AppState.MAX_RACERS}",
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, mono = true)
                IconButton(onClick = { state.pickRandom(max(state.selected.size, 4)) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "랜덤으로 고르기")
                }
                IconButton(onClick = { state.clearSelection() }, enabled = state.selected.isNotEmpty(), modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "선택 해제")
                }
            }
            AnimalGrid(state, Modifier.weight(1f).padding(bottom = 10.dp)) { animal ->
                if (state.toggle(animal)) {
                    focused = animal
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else {
                    scope.launch { snackbar.showSnackbar("최대 ${AppState.MAX_RACERS}마리까지 출전할 수 있어요") }
                }
            }
        }
    }

    if (editBet) BetDialog(state.bet, onDismiss = { editBet = false }) { state.updateBet(it); editBet = false }
    if (showHistory) HistorySheet(state) { showHistory = false }
}

@Composable
private fun TopRow(state: AppState, onHistory: () -> Unit) {
    val race = LocalRaceColors.current
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("BOKBULBOK DERBY", style = NumberStyle, fontSize = 10.sp, letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary)
            Text("복불복 동물 레이스", fontFamily = DisplayFont, fontSize = 26.sp)
            val last = state.history.firstOrNull()?.results?.lastOrNull()
            if (last != null) {
                Text(
                    buildAnnotatedString {
                        append("지난 판 꼴찌 ")
                        withStyle(SpanStyle(color = race.penalty, fontWeight = FontWeight.Bold)) {
                            append("${last.animal.emoji} ${last.animal.label}")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onHistory) {
            BadgedBox(badge = { if (state.history.isNotEmpty()) Badge { Text("${state.history.size}") } }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "최근 경기")
            }
        }
    }
}

@Composable
private fun BetChip(bet: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("벌칙", style = MaterialTheme.typography.labelMedium, color = LocalRaceColors.current.penalty,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text(bet.ifEmpty { "없음" }, style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Edit, contentDescription = "벌칙 수정", modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 남은 높이에 맞춰 타일 크기를 계산해서 14마리가 한 화면에 들어오게 한다. */
@Composable
private fun AnimalGrid(state: AppState, modifier: Modifier, onTap: (Animal) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cols = if (maxWidth > 560.dp) 5 else 3
        val animals = Animal.entries
        val rows = ceil(animals.size / cols.toDouble()).toInt()
        val gap = 8.dp
        val tileH = ((maxHeight - gap * (rows - 1)) / rows).coerceIn(78.dp, 150.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            animals.chunked(cols).forEach { rowAnimals ->
                Row(Modifier.fillMaxWidth().height(tileH), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowAnimals.forEach { animal ->
                        AnimalTile(
                            animal, lane = state.selected.indexOf(animal), height = tileH,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { onTap(animal) }
                    }
                    repeat(cols - rowAnimals.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun AnimalTile(animal: Animal, lane: Int, height: androidx.compose.ui.unit.Dp, modifier: Modifier, onClick: () -> Unit) {
    val selected = lane >= 0
    val scheme = MaterialTheme.colorScheme
    val tint = Color(animal.color)
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerLowest, label = "container")
    val borderColor by animateColorAsState(if (selected) scheme.primary else scheme.outlineVariant, label = "border")
    val pop by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.4f, stiffness = 400f), label = "pop")
    val lastLane = remember { IntArray(1) }
    if (selected) lastLane[0] = lane
    val emojiSize = (height.value * 0.36f).coerceIn(28f, 48f).sp

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Box(Modifier.fillMaxSize()) {
            // 동물 대표색 바닥 광택
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size((emojiSize.value * 1.5f).dp)
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = if (selected) 0.28f else 0.14f)),
            )
            Column(
                Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(animal.emoji, fontSize = emojiSize, modifier = Modifier.graphicsLayer { scaleX = pop; scaleY = pop })
                Spacer(Modifier.height(2.dp))
                Text(animal.label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(animal.skill, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Card 안은 ColumnScope라서 범위 없는 버전을 명시적으로 호출
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
            ) {
                LaneBadge(lastLane[0], size = 24.dp)
            }
        }
    }
}

@Composable
private fun StartBar(state: AppState, focused: Animal?, onStart: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surfaceContainer, shadowElevation = 12.dp) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 마지막으로 누른 동물의 스킬 설명
            AnimatedContent(
                targetState = focused,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "skill",
            ) { animal ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (animal == null) {
                        Text("동물을 누르면 스킬 설명이 여기에 나와요.", style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant)
                    } else {
                        Text(animal.emoji, fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${animal.label} · ${animal.skill}", style = MaterialTheme.typography.titleSmall)
                            Text(animal.desc, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.canStart) "${state.selected.size}마리 출전" else "2마리 이상 골라 주세요",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        state.selected.joinToString(" ") { it.emoji }.ifEmpty { "-" },
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 18.sp,
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BetDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("꼴찌 벌칙") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(30) },
                    singleLine = true, placeholder = { Text("예: 꼴찌가 커피 쏘기") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BetPresets.forEach { preset ->
                        SuggestionChip(onClick = { text = preset }, label = { Text(preset.removePrefix("꼴찌가 ")) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(state: AppState, onDismiss: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("최근 경기", style = MaterialTheme.typography.headlineSmall)
                Text("최근 ${AppState.HISTORY_SIZE}판까지 저장돼요.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.history.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("기록 지우기") }
        }
        Spacer(Modifier.height(12.dp))
        if (state.history.isEmpty()) {
            EmptyHistory(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Spacer(Modifier.height(32.dp))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.history, key = { _, r -> r.at }) { i, record ->
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

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(
        modifier
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
            Text(
                buildAnnotatedString {
                    append(record.bet.ifEmpty { "꼴찌" })
                    append("  →  ")
                    withStyle(SpanStyle(color = race.penalty, fontWeight = FontWeight.Bold)) {
                        append("${loser.emoji} ${loser.label}")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
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
