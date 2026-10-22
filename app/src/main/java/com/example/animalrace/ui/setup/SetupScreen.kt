package com.example.animalrace.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.AppState
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Fx
import com.example.animalrace.model.RandomRank
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.CartoonButton
import com.example.animalrace.ui.components.IconTileButton
import com.example.animalrace.ui.components.Icons2
import com.example.animalrace.ui.components.InkIcon
import com.example.animalrace.ui.components.LaneSticker
import com.example.animalrace.ui.components.OutlinedText
import com.example.animalrace.ui.components.Tag
import com.example.animalrace.ui.components.shortBet
import com.example.animalrace.ui.theme.Jua
import com.example.animalrace.ui.theme.NumberStyle
import com.example.animalrace.ui.theme.Pal
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max

private val BetPresets = listOf("꼴찌가 커피 쏘기", "꼴찌가 점심 쏘기", "꼴찌가 간식 사기", "꼴찌가 회의록 쓰기")

/** 메인: 동물 고르기. 14마리를 한 화면 타일로 보여 주고, 도감·기록·벌칙은 위에서 연다. */
@Composable
fun SetupScreen(state: AppState) {
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(state.selected.lastOrNull() ?: Animal.CHEETAH) }
    var dexIndex by remember { mutableStateOf<Int?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var editBet by remember { mutableStateOf(false) }

    fun toggle(animal: Animal) {
        if (state.toggle(animal)) {
            focused = animal
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            scope.launch { snackbar.showSnackbar("최대 ${AppState.MAX_RACERS}마리까지 출전할 수 있어요") }
        }
    }

    Box(Modifier.fillMaxSize().background(Pal.Sky)) {
        Cloud(Modifier.offset(x = (-24).dp, y = 110.dp), 120.dp)
        Cloud(Modifier.offset(x = 270.dp, y = 40.dp), 140.dp)

        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedText("동물 대소동 레이스!", 26.sp)
                        state.history.firstOrNull()?.let { last ->
                            Text(
                                buildAnnotatedString {
                                    val miniGame = last.miniGame
                                    append(if (miniGame != null) "지난 판 몰빵 " else if (last.picks == null) "지난 판 꼴찌 " else "지난 판 당첨 ")
                                    withStyle(SpanStyle(color = Pal.TomatoText)) {
                                        append(miniGame?.loser?.label ?: last.losers.joinToString("·") { it.animal.label })
                                    }
                                },
                                fontSize = 14.sp, color = Pal.Ink,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.75f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    IconTileButton(Icons2.Book, "도감", onClick = { dexIndex = focused.ordinal })
                    IconTileButton(
                        Icons2.Trophy, "기록", onClick = { showHistory = true },
                        badge = state.history.size.takeIf { it > 0 }?.toString(),
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Pal.Cream)
                        .border(3.dp, Pal.Ink, RoundedCornerShape(14.dp))
                        .clickable { editBet = true }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.randomRank) Tag("🎯 랜덤 벌칙", Pal.Sun, Pal.Ink, fontSize = 14.sp)
                    else Tag("꼴찌 벌칙", Pal.Tomato, fontSize = 14.sp)
                    Text(shortBet(state.bet).ifEmpty { "없음" }, fontSize = 17.sp, color = Pal.Ink, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    InkIcon(Icons2.Pencil, size = 18.dp)
                }

                RandomRankRow(state, Modifier.padding(bottom = 4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        buildAnnotatedString {
                            append("출전할 동물 ")
                            withStyle(SpanStyle(fontFamily = NumberStyle.fontFamily)) { append("${state.selected.size}") }
                            withStyle(SpanStyle(fontFamily = NumberStyle.fontFamily, color = Color(0xFF4A5F78))) { append("/${AppState.MAX_RACERS}") }
                        },
                        fontSize = 19.sp, color = Pal.Ink, modifier = Modifier.weight(1f),
                    )
                    SmallButton("랜덤", Icons2.Shuffle) { state.pickRandom(max(state.selected.size, 4)) }
                    SmallButton("비우기", null) { state.clearSelection() }
                }
            }

            AnimalGrid(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                onTap = ::toggle,
            )

            BottomPanel(state, focused) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.startRace()
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp))

        AnimatedVisibility(
            visible = dexIndex != null,
            enter = fadeIn(), exit = fadeOut(),
        ) {
            val idx = remember { mutableIntStateOf(0) }
            dexIndex?.let { idx.intValue = it }
            Dex(
                state = state,
                index = idx.intValue,
                onIndex = { dexIndex = it },
                onToggle = { toggle(it) },
                onClose = { dexIndex = null },
            )
        }

        AnimatedVisibility(visible = showHistory, enter = fadeIn(), exit = fadeOut()) {
            HistorySheet(state) { showHistory = false }
        }
        AnimatedVisibility(visible = editBet, enter = fadeIn(), exit = fadeOut()) {
            BetEditor(state.bet, onDismiss = { editBet = false }) { state.updateBet(it); editBet = false }
        }
    }
}

@Composable
private fun Cloud(modifier: Modifier, width: Dp) {
    Box(
        modifier
            .size(width, width * 0.28f)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.8f)),
    )
}

@Composable
private fun SmallButton(label: String, icon: List<String>?, onClick: () -> Unit) {
    Row(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Pal.Paper)
            .border(2.dp, Pal.Ink, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) InkIcon(icon, size = 14.dp, stroke = 2.8f)
        Text(label, fontSize = 14.sp, color = Pal.Ink)
    }
}

/** 3열 유지. 남은 높이에 5줄이 들어가게 타일 크기를 정하고, 그보다 많으면 스크롤한다. */
@Composable
private fun AnimalGrid(state: AppState, modifier: Modifier, onTap: (Animal) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cols = if (maxWidth > 560.dp) 5 else 3
        val animals = Animal.entries
        val rows = ceil(animals.size / cols.toDouble()).toInt()
        val gap = 10.dp
        val stickerRoom = 10.dp // 첫 줄 번호 스티커가 위로 튀어나올 자리
        // 각 줄 높이 = 타일 + 그림자 5dp
        val visibleRows = minOf(rows, 5)
        val tileH = ((maxHeight - stickerRoom - gap * (visibleRows - 1)) / visibleRows - 5.dp).coerceIn(70.dp, 140.dp)
        val tileW = (maxWidth - gap * (cols - 1)) / cols
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(top = stickerRoom, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            animals.chunked(cols).forEach { rowAnimals ->
                Row(Modifier.fillMaxWidth().height(tileH + 5.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowAnimals.forEach { animal ->
                        AnimalTile(animal, state.selected.indexOf(animal), tileW, tileH, Modifier.weight(1f).fillMaxHeight()) { onTap(animal) }
                    }
                    repeat(cols - rowAnimals.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        // 아래에 동물이 더 있으면 안내 (끝까지 내리면 사라짐)
        AnimatedVisibility(
            visible = scroll.canScrollForward,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 6.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pal.Ink.copy(alpha = 0.82f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                InkIcon(listOf("M5 9l7 7 7-7"), size = 12.dp, color = Color.White, stroke = 3.5f)
                Text("${animals.size}종 · 아래로 더 있어요", fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun AnimalTile(animal: Animal, lane: Int, tileW: Dp, tileH: Dp, modifier: Modifier, onClick: () -> Unit) {
    val selected = lane >= 0
    val shape = RoundedCornerShape(20.dp)
    val lastLane = remember { IntArray(1) }
    if (selected) lastLane[0] = lane
    val sprite = minOf(tileH * 0.72f, tileW * 0.62f)
    Box(modifier) {
        Box(Modifier.fillMaxWidth().height(tileH).offset(y = 5.dp).clip(shape).background(Pal.Ink))
        Column(
            Modifier
                .fillMaxWidth()
                .height(tileH)
                .offset(y = if (selected) 3.dp else 0.dp)
                .clip(shape)
                .background(if (selected) Pal.Sun else Pal.Paper)
                .border(3.dp, Pal.Ink, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimalSprite(animal, sprite, running = selected)
            Text(animal.label, fontSize = 15.sp, color = Pal.Ink, lineHeight = 16.sp)
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-10).dp),
        ) {
            LaneSticker(lastLane[0], Modifier.rotate(-10f), size = 28.dp, border = 3.dp)
        }
    }
}

/** 랜덤 등수 모드 체크박스. 오른쪽에 지금 인원으로 몇 명이 당첨되는지 보여 준다. */
@Composable
private fun RandomRankRow(state: AppState, modifier: Modifier) {
    val on = state.randomRank
    val n = state.selected.size
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Pal.Paper)
            .border(3.dp, Pal.Ink, RoundedCornerShape(14.dp))
            .toggleable(value = on, role = Role.Checkbox, onValueChange = state::updateRandomRank)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (on) Pal.Sun else Pal.Paper)
                .border(3.dp, Pal.Ink, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) { if (on) InkIcon(Icons2.Check, size = 18.dp, stroke = 3.5f) }
        Text("랜덤 등수 모드", fontSize = 17.sp, color = Pal.Ink, modifier = Modifier.weight(1f))
        Text(
            if (n >= AppState.MIN_RACERS) buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = NumberStyle.fontFamily)) { append("$n") }
                append("마리 → ")
                withStyle(SpanStyle(fontFamily = NumberStyle.fontFamily, color = Pal.TomatoText)) { append("${RandomRank.count(n)}") }
                append("명 당첨")
            } else buildAnnotatedString { append("출발 전 룰렛으로 등수 뽑기") },
            fontSize = 14.sp, color = Pal.InkSoft,
        )
    }
}

@Composable
private fun BottomPanel(state: AppState, focused: Animal, onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Pal.Grass)
            .border(width = 0.dp, color = Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(Pal.Ink))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pal.Paper)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(16.dp))
                    .padding(start = 4.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnimalSprite(focused, 52.dp)
                Column(Modifier.weight(1f)) {
                    Text("${focused.label} · ${focused.skill}", fontSize = 15.sp, color = Pal.Ink)
                    Text(focused.desc, fontSize = 13.sp, color = Pal.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            CartoonButton(onClick = onStart, enabled = state.canStart, modifier = Modifier.fillMaxWidth(), height = 62.dp, corner = 22.dp, depth = 6.dp) {
                if (state.canStart) {
                    InkIcon(Icons2.Play, size = 22.dp, fill = Color.White, stroke = 2f)
                    Spacer(Modifier.width(10.dp))
                    OutlinedText(if (state.randomRank) "룰렛 돌리고 출발!" else "${state.selected.size}마리 출발!", 26.sp, outlineWidth = 5f)
                } else {
                    Text("2마리 이상 골라 주세요", fontSize = 21.sp, color = Color.White)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 도감

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Dex(state: AppState, index: Int, onIndex: (Int) -> Unit, onToggle: (Animal) -> Unit, onClose: () -> Unit) {
    val animal = Animal.entries[index]
    val lane = state.selected.indexOf(animal)
    val full = lane < 0 && state.selected.size >= AppState.MAX_RACERS
    Sheet(onClose, fill = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("동물 도감", fontSize = 24.sp, color = Pal.Ink, modifier = Modifier.weight(1f))
            Text("${index + 1} / ${Animal.entries.size}", style = NumberStyle, fontSize = 15.sp, color = Pal.InkSoft)
            Spacer(Modifier.width(10.dp))
            SquareIcon(Icons2.Close, "도감 닫기", onClose)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SquareIcon(Icons2.Left, "이전 동물", { onIndex((index + Animal.entries.size - 1) % Animal.entries.size) }, tall = true)
            Box(
                Modifier
                    .weight(1f)
                    .height(210.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Pal.Sky)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(Modifier.fillMaxWidth().height(52.dp).background(Pal.Dirt))
                Box(Modifier.fillMaxWidth().padding(bottom = 49.dp).height(3.dp).background(Pal.Ink))
                if (lane < 0) {
                    Box(Modifier.align(Alignment.TopStart).padding(18.dp).size(70.dp, 24.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.9f)))
                }
                AnimalSprite(animal, 190.dp, running = true, fx = Fx.SKILL, modifier = Modifier.padding(bottom = 12.dp))
                // 스킬 말풍선이 오른쪽 위에 뜨므로 출전 배지는 왼쪽 위
                if (lane >= 0) {
                    Row(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Pal.Sun)
                            .border(3.dp, Pal.Ink, RoundedCornerShape(15.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        LaneSticker(lane, size = 18.dp)
                        Text("출전", fontSize = 14.sp, color = Pal.Ink)
                    }
                }
            }
            SquareIcon(Icons2.Right, "다음 동물", { onIndex((index + 1) % Animal.entries.size) }, tall = true)
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(animal.label, fontSize = 30.sp, color = Pal.Ink)
                Text(
                    animal.skill, fontSize = 16.sp, color = Pal.Ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Pal.Cream)
                        .border(3.dp, Pal.Ink, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            // 동물마다 설명 길이가 달라도 아래 요소·버튼 위치가 흔들리지 않게 줄 수를 고정
            Text(animal.desc, fontSize = 16.sp, color = Pal.Ink, lineHeight = 24.sp, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(animal.scene, fontSize = 15.sp, color = Pal.InkSoft, lineHeight = 21.sp, minLines = 3, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Pal.Mint)
                .border(2.dp, Pal.Ink, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InkIcon(Icons2.Scale, size = 20.dp)
            Text("스킬은 경기를 재밌게 만드는 연출이에요. 어떤 동물을 골라도 1등 확률은 똑같아요.", fontSize = 14.sp, color = Pal.Ink, lineHeight = 19.sp)
        }
        FlowRow(maxItemsInEachRow = 7, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Animal.entries.forEach { a ->
                val current = a.ordinal == index
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (current) Pal.Sun else if (a in state.selected) Pal.Cream else Pal.Paper)
                        .border(if (current) 3.dp else 2.dp, if (current) Pal.Ink else Pal.Disabled, RoundedCornerShape(12.dp))
                        .clickable { onIndex(a.ordinal) },
                    contentAlignment = Alignment.Center,
                ) { AnimalSprite(a, 40.dp) }
            }
        }
        Spacer(Modifier.height(4.dp))
        CartoonButton(
            onClick = { if (!full) onToggle(animal) },
            enabled = !full,
            color = if (lane >= 0) Pal.Paper else Pal.Sun,
            modifier = Modifier.fillMaxWidth(), height = 58.dp,
        ) {
            Text(
                when {
                    lane >= 0 -> "${lane + 1}번 출전 취소"
                    full -> "이미 ${AppState.MAX_RACERS}마리가 출전해요"
                    else -> "이 동물로 출전하기"
                },
                fontSize = 22.sp, color = Pal.Ink,
            )
        }
    }
}

@Composable
private fun SquareIcon(icon: List<String>, label: String, onClick: () -> Unit, tall: Boolean = false) {
    Box(
        Modifier
            .size(40.dp, if (tall) 56.dp else 40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Pal.Paper)
            .border(3.dp, Pal.Ink, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { InkIcon(icon, size = 18.dp, stroke = 3f) }
}

/**
 * 아래에서 올라오는 시트. 어두운 배경을 누르거나, 위쪽 손잡이를 잡고 아래로 끌어내리면 닫힌다.
 * [fill]이 false면 내용 높이만큼만 올라온다.
 */
@Composable
private fun Sheet(onClose: () -> Unit, fill: Boolean = true, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val drag = remember { Animatable(0f) }
    var sheetPx by remember { mutableFloatStateOf(1f) }
    var closing by remember { mutableStateOf(false) }
    val dragState = rememberDraggableState { delta ->
        scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - (drag.value / sheetPx).coerceIn(0f, 1f) * 0.6f }
            .background(Pal.Ink.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    if (fill) Modifier.fillMaxHeight(0.88f)
                    else Modifier.heightIn(max = maxHeight * 0.92f),
                )
                .onSizeChanged { sheetPx = it.height.toFloat().coerceAtLeast(1f) }
                .offset { IntOffset(0, drag.value.roundToInt()) }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            // 손잡이: 폭 전체를 잡을 수 있게 넉넉한 높이
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val far = drag.value > with(density) { 110.dp.toPx() } || drag.value > sheetPx * 0.3f
                            if (!closing && (far || velocity > 1800f)) {
                                closing = true
                                drag.animateTo(sheetPx, tween(160))
                                onClose()
                            } else {
                                drag.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 600f))
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(48.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(Pal.Disabled))
            }
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                    .then(if (fill) Modifier else Modifier.verticalScroll(rememberScrollState())),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

// ---------------------------------------------------------------- 최근 경기

@Composable
private fun HistorySheet(state: AppState, onClose: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Sheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("최근 경기", fontSize = 24.sp, color = Pal.Ink)
                Text("최근 ${AppState.HISTORY_SIZE}판까지 저장돼요.", fontSize = 13.sp, color = Pal.InkSoft)
            }
            if (state.history.isNotEmpty()) {
                Text(
                    if (confirmClear) "정말 지울까요?" else "기록 지우기", fontSize = 14.sp,
                    color = if (confirmClear) Pal.TomatoText else Pal.InkSoft,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { if (confirmClear) { state.clearHistory(); confirmClear = false } else confirmClear = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            SquareIcon(Icons2.Close, "기록 닫기", onClose)
        }
        if (state.history.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, Pal.Disabled, RoundedCornerShape(18.dp))
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnimalSprite(Animal.TURTLE, 80.dp)
                Text("아직 경기 기록이 없어요", fontSize = 17.sp, color = Pal.Ink)
                Text("동물을 고르고 출발!을 누르면 여기에 쌓여요.", fontSize = 13.sp, color = Pal.InkSoft)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                itemsIndexed(state.history, key = { _, r -> r.at }) { i, record -> HistoryCard(state.history.size - i, record) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCard(number: Int, record: RaceRecord) {
    val picks = record.picks
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Pal.Paper)
            .border(3.dp, Pal.Ink, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("#$number", Pal.Cream, Pal.Ink, number = true)
            val venue = record.venue?.let { "${it.emoji} ${it.title} · " }.orEmpty()
            Text("${formatTime(record.at)} · $venue${record.results.size}마리 · 역전 ${record.leadChanges}회", fontSize = 13.sp, color = Pal.InkSoft)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (picks != null) Tag("🎯 랜덤", Pal.Sun, Pal.Ink, fontSize = 12.sp)
            Text(
                buildAnnotatedString {
                    append(shortBet(record.bet).ifEmpty { if (picks == null) "꼴찌" else "당첨" })
                    append("  →  ")
                    withStyle(SpanStyle(color = Pal.TomatoText)) {
                        append(
                            if (picks == null) record.results.last().animal.label
                            else picks.zip(record.losers).joinToString(" · ") { (p, r) -> "${p}등 ${r.animal.label}" },
                        )
                    }
                },
                fontSize = 16.sp, color = Pal.Ink,
            )
        }
        record.miniGame?.let { m ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag("💸 몰빵", Pal.Tomato, fontSize = 12.sp)
                Text(
                    buildAnnotatedString {
                        append("${m.game.emoji} ${m.game.title}  →  ")
                        withStyle(SpanStyle(color = Pal.TomatoText)) { append(m.loser.label) }
                    },
                    fontSize = 16.sp, color = Pal.Ink,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            record.results.forEachIndexed { i, r ->
                val last = if (picks == null) i == record.results.lastIndex else (i + 1) in picks
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (i == 0) Pal.SunPale else if (last) Pal.Pink else Pal.Ivory)
                        .border(2.dp, if (i == 0 || last) Pal.Ink else Pal.Line, RoundedCornerShape(12.dp))
                        .padding(start = 6.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", style = NumberStyle, fontSize = 12.sp, color = if (last) Pal.TomatoText else Pal.InkSoft)
                    AnimalSprite(r.animal, 32.dp)
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

// ---------------------------------------------------------------- 벌칙 수정

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BetEditor(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Pal.Ivory)
                .border(3.dp, Pal.Ink, RoundedCornerShape(24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("꼴찌 벌칙", fontSize = 22.sp, color = Pal.Ink)
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(30) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = Jua, fontSize = 18.sp, color = Pal.Ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pal.Paper)
                    .border(3.dp, Pal.Ink, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("예: 꼴찌가 커피 쏘기", fontSize = 18.sp, color = Pal.Disabled)
                    inner()
                },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BetPresets.forEach { preset ->
                    Text(
                        shortBet(preset), fontSize = 15.sp, color = Pal.Ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Pal.Cream)
                            .border(2.dp, Pal.Ink, RoundedCornerShape(12.dp))
                            .clickable { text = preset }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartoonButton(onClick = onDismiss, color = Pal.Paper, modifier = Modifier.weight(1f), height = 50.dp, depth = 4.dp) {
                    Text("취소", fontSize = 18.sp, color = Pal.Ink)
                }
                CartoonButton(onClick = { onConfirm(text.trim()) }, color = Pal.Sun, modifier = Modifier.weight(1f), height = 50.dp, depth = 4.dp) {
                    Text("확인", fontSize = 18.sp, color = Pal.Ink)
                }
            }
        }
    }
}
