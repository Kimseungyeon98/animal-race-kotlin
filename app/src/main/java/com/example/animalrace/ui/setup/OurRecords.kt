package com.example.animalrace.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.AppState
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Period
import com.example.animalrace.model.ourStats
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.shortBet
import com.example.animalrace.ui.theme.Fredoka
import com.example.animalrace.ui.theme.Pal

/**
 * 우리 기록 (4.2): 기간·멤버 묶음별 벌칙 왕·1등 왕·연속 당첨·최근 벌칙.
 * 시안: 캔버스 "🙋 4.0 사람이 주인공" ⑥
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OurRecords(state: AppState, now: Long = System.currentTimeMillis()) {
    var period by remember { mutableStateOf(Period.MONTH) }
    // 👥 칩을 누를 때마다 모두 → 묶음1 → 묶음2 … → 모두
    var groupIndex by remember { mutableStateOf(-1) }
    val group = state.groups.getOrNull(groupIndex)
    val stats = ourStats(state.history, period, group, now)
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Period.entries.forEach { p -> SmallChip(p.label, p == period) { period = p } }
            if (state.groups.isNotEmpty()) SmallChip(
                "👥 " + (group?.joinToString("·")?.let { if (it.length > 12) it.take(11) + "…" else it } ?: "모든 멤버"),
                group != null,
            ) { groupIndex = if (groupIndex + 1 >= state.groups.size) -1 else groupIndex + 1 }
        }
        if (stats.games == 0) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, Pal.Disabled, RoundedCornerShape(18.dp))
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnimalSprite(Animal.PANDA, 80.dp)
                Text(if (state.history.isEmpty()) "아직 경기 기록이 없어요" else "${period.label}엔 이름 붙인 판이 없어요", fontSize = 17.sp, color = Pal.Ink)
                Text("🙋 참가자에서 이름을 붙이고 달리면\n여기에 우리끼리 순위가 쌓여요.", fontSize = 13.sp, color = Pal.InkSoft, textAlign = TextAlign.Center)
            }
            return@Column
        }
        // ☕ 벌칙 왕: 사람마다 걸린 횟수 막대
        Card(Pal.Paper) {
            Text(
                buildAnnotatedString {
                    append("☕ 벌칙 왕 ")
                    withStyle(SpanStyle(fontSize = 14.sp, color = Pal.InkSoft)) { append("· 걸린 횟수 / 달린 판") }
                },
                fontSize = 20.sp, color = Pal.Ink,
            )
            val max = stats.people.maxOf { it.penalties }.coerceAtLeast(1)
            val colors = listOf(Pal.Tomato, Pal.Sun, Pal.Sky, Pal.Grass, Pal.Bronze, Pal.Silver)
            stats.people.take(8).forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.who, fontSize = 17.sp, color = Pal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(64.dp))
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val w = (maxWidth - 64.dp) * (p.penalties.toFloat() / max)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                Modifier
                                    .width(w.coerceAtLeast(10.dp))
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (p.penalties == 0) Pal.Line else colors[i % colors.size])
                                    .border(2.5.dp, Pal.Ink, RoundedCornerShape(10.dp)),
                            )
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontFamily = Fredoka, fontSize = 17.sp)) { append("${p.penalties}") }
                                    withStyle(SpanStyle(fontSize = 12.sp, color = Pal.InkSoft)) { append("/${p.games}") }
                                    if (p == stats.penaltyKing) append(" 👑")
                                },
                                fontSize = 15.sp, color = Pal.Ink, maxLines = 1,
                            )
                        }
                    }
                }
            }
            Text(
                buildAnnotatedString {
                    append("총 ")
                    withStyle(SpanStyle(fontFamily = Fredoka)) { append("${stats.games}") }
                    append("판")
                    stats.lucky?.let { append(" · 가장 운 좋은 사람: ${it.who} (${it.penalties}/${it.games})") }
                },
                fontSize = 14.sp, color = Pal.InkSoft,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 🔥 연속 당첨 (없으면 가장 많이 걸린 사람)
            Card(Pal.Pink, Modifier.weight(1f)) {
                val streak = stats.streak
                Text("🔥 연속 당첨", fontSize = 16.sp, color = Pal.Ink)
                Text(streak?.first ?: "없음", fontSize = 28.sp, color = if (streak != null) Pal.TomatoText else Pal.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (streak != null) buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = Fredoka)) { append("${streak.second}") }
                        append("판 연속 당첨")
                    } else buildAnnotatedString { append("2판 연속부터 나와요") },
                    fontSize = 14.sp, color = Pal.Ink,
                )
            }
            // 🏆 1등 왕
            Card(Pal.SunPale, Modifier.weight(1f)) {
                val king = stats.winKing
                Text("🏆 1등 왕", fontSize = 16.sp, color = Pal.Ink)
                Text(king?.who ?: "없음", fontSize = 28.sp, color = if (king != null) Pal.Ink else Pal.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (king != null) buildAnnotatedString {
                        append("1등 ")
                        withStyle(SpanStyle(fontFamily = Fredoka)) { append("${king.wins}") }
                        append("번")
                    } else buildAnnotatedString { append("이름 붙인 1등이 아직 없어요") },
                    fontSize = 14.sp, color = Pal.Ink,
                )
            }
        }
        // 🧾 최근 5판 벌칙
        Card(Pal.Paper) {
            Text("🧾 최근 ${stats.recent.size}판 벌칙", fontSize = 16.sp, color = Pal.Ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val king = stats.penaltyKing?.who
                stats.recent.forEach { (bet, who) ->
                    val names = who.joinToString("·").ifEmpty { "동물" }
                    Text(
                        buildAnnotatedString {
                            append(names)
                            shortBet(bet).takeIf { it.isNotEmpty() }?.let {
                                withStyle(SpanStyle(fontSize = 12.sp, color = Pal.InkSoft)) { append(" " + if (it.length > 6) it.take(5) + "…" else it) }
                            }
                        },
                        fontSize = 15.sp, color = Pal.Ink, maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (king != null && king in who) Pal.Pink else Pal.Ivory)
                            .border(2.dp, Pal.Ink, RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Text(
            "결과는 매판 공평한 무작위예요 · 순위는 재미로만!", fontSize = 13.sp, color = Pal.InkSoft,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Card(bg: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(3.dp, Pal.Ink, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
