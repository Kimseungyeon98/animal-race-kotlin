package com.example.animalrace.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.Venue
import com.example.animalrace.ui.art.AnimalSprite
import com.example.animalrace.ui.components.CartoonButton
import com.example.animalrace.ui.components.OutlinedText
import com.example.animalrace.ui.components.shortBet
import com.example.animalrace.ui.components.withRo
import com.example.animalrace.ui.race.Stadium
import com.example.animalrace.ui.theme.NumberStyle
import com.example.animalrace.ui.theme.Pal
import kotlinx.coroutines.launch
import java.util.Calendar

/** 결과 카드 이미지를 밖으로 내보내는 곳. 앱에서는 MainActivity가 안드로이드 공유 창·갤러리 저장을 넣는다 (기본 null: 미리보기·검증용) */
interface Sharer {
    /** 공유 창(카톡 등)으로 보내기 */
    fun share(image: ImageBitmap, text: String)
    /** 갤러리(사진)에 저장. 못 하면 false */
    fun save(image: ImageBitmap): Boolean
}

val LocalSharer = staticCompositionLocalOf<Sharer?> { null }

/** 카드에 크게 쓸 당첨 내용: 몰아주기를 했으면 그 한 명, 아니면 당첨자들 */
private class Verdict(val people: List<RaceResult>, val headline: String, val detail: String)

private fun verdict(record: RaceRecord): Verdict {
    val n = record.results.size
    val mini = record.miniGame
    val picks = record.picks
    return when {
        mini != null -> {
            val r = record.losers.firstOrNull { it.animal == mini.loser } ?: record.results.first { it.animal == mini.loser }
            Verdict(listOf(r), "${mini.name} 당첨!", "${mini.game.emoji} ${mini.game.title}로 몰빵 · ${record.losers.size}명 중 한 명")
        }
        picks == null -> {
            val r = record.results.last()
            val how = if (r.who != null) "${r.animal.emoji} ${withRo(r.animal.label)} 꼴찌" else "${r.animal.emoji} 꼴찌"
            Verdict(listOf(r), "${r.name} 당첨!", "$how (${count(record)} 중 ${n}등)")
        }
        else -> Verdict(
            record.losers,
            record.losers.joinToString("·") { it.name } + " 당첨!",
            "🎯 랜덤 등수 " + picks.joinToString("·") { "${it}등" } + " · ${count(record)} 중",
        )
    }
}

/** 사람이 달렸으면 "4명", 동물만이면 "4마리" */
private fun count(record: RaceRecord) = "${record.results.size}" + if (record.results.any { it.who != null }) "명" else "마리"

/** 공유 글 (이미지와 같이 보낸다) */
fun shareText(record: RaceRecord): String {
    val v = verdict(record)
    val bet = shortBet(record.bet).ifEmpty { "오늘의 꼴찌" }
    return "🏁 동물 레이스 · $bet → ${v.headline}"
}

/**
 * 결과 공유 카드 창 (4.1). 카드 한 장을 그대로 이미지로 떠서 공유 창에 보내거나 사진으로 저장한다.
 * 시안: 캔버스 "🙋 4.0 사람이 주인공" ⑤
 */
@Composable
fun ShareSheet(record: RaceRecord, onClose: () -> Unit) {
    val sharer = LocalSharer.current
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var toast by remember { mutableStateOf<String?>(null) }
    fun withImage(block: (ImageBitmap) -> Unit) {
        scope.launch { block(layer.toImageBitmap()) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Ink.copy(alpha = 0.9f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .padding(horizontal = 22.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 이 안에 그려진 것이 그대로 이미지가 된다
            Box(
                Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            ) { ShareCard(record) }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartoonButton(
                    onClick = { withImage { toast = if (sharer?.save(it) == true) "사진에 저장했어요" else "공유 창에서 저장해 주세요" } },
                    color = Pal.Paper, modifier = Modifier.weight(1f), height = 58.dp,
                ) { Text("🖼 사진 저장", fontSize = 18.sp, color = Pal.Ink) }
                CartoonButton(
                    onClick = { withImage { sharer?.share(it, shareText(record)) } },
                    color = Pal.Sun, modifier = Modifier.weight(1.3f), height = 58.dp,
                ) { Text("📤 공유하기", fontSize = 20.sp, color = Pal.Ink) }
            }
            Text(
                toast ?: "닫기", fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 14.dp).clickable(onClick = onClose).padding(8.dp),
            )
        }
    }
}

/** 공유 이미지로 나가는 카드 한 장: 날짜·경기장 · 벌칙 · 당첨자 이름 크게 · 1~3등 시상대 · 앱 이름 */
@Composable
fun ShareCard(record: RaceRecord) {
    val v = verdict(record)
    val stadium = Stadium.entries.first { it.venue == (record.venue ?: Venue.MEADOW) }
    val hills = stadium.hills ?: (stadium.verge to stadium.dirt)
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .width(340.dp)
            .clip(shape)
            .background(Pal.Cream)
            .border(4.dp, Pal.Ink, shape),
    ) {
        // 머리: 경기장 하늘·언덕 + 날짜 + 벌칙 + 당첨 동물
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clipToBounds()
                .background(Brush.verticalGradient(listOf(stadium.skyTop, stadium.skyBottom)))
                .drawBehind {
                    val ink = Pal.Ink
                    fun hill(c: Color, x: Float, y: Float, w: Float) {
                        drawOval(c, Offset(x, y), Size(w, w * 0.46f))
                        drawOval(ink, Offset(x, y), Size(w, w * 0.46f), style = Stroke(3.dp.toPx()))
                    }
                    hill(hills.first, -30.dp.toPx(), 92.dp.toPx(), 260.dp.toPx())
                    hill(hills.second, 150.dp.toPx(), 102.dp.toPx(), 240.dp.toPx())
                    drawRect(ink, Offset(0f, size.height - 4.dp.toPx()), Size(size.width, 4.dp.toPx()))
                },
        ) {
            Column(Modifier.padding(start = 16.dp, top = 12.dp).width(200.dp)) {
                Text("${dateLabel(record.at)} · ${(record.venue ?: Venue.MEADOW).label}", fontSize = 14.sp, color = Pal.Ink)
                Text("오늘의 벌칙", fontSize = 13.sp, color = Pal.InkSoft, modifier = Modifier.padding(top = 6.dp))
                OutlinedText(shortBet(record.bet).ifEmpty { "오늘의 꼴찌" }, 26.sp, outlineWidth = 6f)
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy((-34).dp)) {
                v.people.take(3).forEach { AnimalSprite(it.animal, if (v.people.size > 1) 82.dp else 110.dp) }
            }
        }
        Text(
            v.headline, fontSize = if (v.headline.length > 10) 32.sp else 42.sp, color = Pal.TomatoText,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 44.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 14.dp),
        )
        Text(v.detail, fontSize = 15.sp, color = Pal.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        // 1~3등 시상대
        val slots = listOfNotNull(record.results.getOrNull(1)?.let { 1 to it }, 0 to record.results[0], record.results.getOrNull(2)?.let { 2 to it })
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, start = 22.dp, end = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            slots.forEach { (place, r) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimalSprite(r.animal, 58.dp)
                    Text(r.name, fontSize = 16.sp, color = Pal.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(listOf(64.dp, 46.dp, 34.dp)[place])
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(listOf(Pal.Sun, Pal.Silver, Pal.Bronze)[place])
                            .border(3.dp, Pal.Ink, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentAlignment = Alignment.TopCenter,
                    ) { Text("${place + 1}", style = NumberStyle, fontSize = 24.sp, color = Pal.Ink, modifier = Modifier.offset(y = 2.dp)) }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(42.dp).background(Pal.Ink),
            contentAlignment = Alignment.Center,
        ) { Text("🏁 동물 레이스", fontSize = 16.sp, color = Color.White) }
    }
}

private fun dateLabel(at: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = at }
    return "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일"
}
