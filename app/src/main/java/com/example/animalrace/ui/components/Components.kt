package com.example.animalrace.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.ui.art.svgPath
import com.example.animalrace.ui.theme.Jua
import com.example.animalrace.ui.theme.LaneCloth
import com.example.animalrace.ui.theme.NumberStyle
import com.example.animalrace.ui.theme.Pal

/**
 * 카툰 입체 버튼: 잉크 외곽선 + 바닥 그림자. 누르면 그림자가 줄며 아래로 살짝 내려간다.
 */
@Composable
fun CartoonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Pal.Tomato,
    enabled: Boolean = true,
    height: Dp = 60.dp,
    corner: Dp = 20.dp,
    depth: Dp = 5.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(corner)
    val edge = if (enabled) Pal.Ink else Pal.DisabledInk
    val drop = if (pressed && enabled) depth * 0.6f else 0.dp
    // propagateMinConstraints: 바깥에서 준 폭(fillMaxWidth, weight, width)을 버튼 면이 그대로 채우게 한다
    Box(modifier.padding(bottom = depth), propagateMinConstraints = true) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = depth)
                .clip(shape)
                .background(edge),
        )
        Row(
            Modifier
                .offset(y = drop)
                .defaultMinSize(minHeight = height)
                .clip(shape)
                .background(if (enabled) color else Pal.Disabled)
                .border(3.dp, edge, shape)
                .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/** 잉크 외곽선을 두른 글자 (제목, 버튼 글자, 카운트다운 숫자). */
@Composable
fun OutlinedText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    outline: Color = Pal.Ink,
    outlineWidth: Float = 7f,
    fontFamily: FontFamily = Jua,
) {
    // 외곽선과 글자가 정확히 겹치도록 두 Text가 같은 스타일(자간 포함)을 쓴다
    val base = TextStyle(fontSize = fontSize, fontFamily = fontFamily, letterSpacing = 0.sp)
    Box(modifier) {
        Text(
            text, color = outline, maxLines = 1, softWrap = false,
            style = base.copy(drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round)),
        )
        Text(text, color = color, maxLines = 1, softWrap = false, style = base)
    }
}

/** 출전 번호 스티커 (실제 경마 색). */
@Composable
fun LaneSticker(lane: Int, modifier: Modifier = Modifier, size: Dp = 26.dp, border: Dp = 2.dp) {
    val (bg, fg) = LaneCloth[lane % LaneCloth.size]
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(border, Pal.Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("${lane + 1}", style = NumberStyle, color = fg, fontSize = (size.value * 0.46f).sp)
    }
}

/** 24×24 선 아이콘 (SVG path). */
@Composable
fun InkIcon(paths: List<String>, modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Pal.Ink, stroke: Float = 2.5f, fill: Color? = null) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            paths.forEach { d ->
                val p = svgPath(d)
                if (fill != null) drawPath(p, fill)
                drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

object Icons2 {
    val Book = listOf("M4 5a2 2 0 0 1 2-2h12v16H6a2 2 0 0 0-2 2V5z", "M4 19a2 2 0 0 1 2-2h12", "M9 8h5")
    val Trophy = listOf("M8 21h8", "M12 17v4", "M7 4h10v4a5 5 0 0 1-10 0V4z", "M17 5h3v2a3 3 0 0 1-3 3", "M7 5H4v2a3 3 0 0 0 3 3")
    val Pencil = listOf("M4 20h4L19 9l-4-4L4 16v4z")
    val Play = listOf("M7 4l13 8-13 8V4z")
    val Shuffle = listOf("M16 3h5v5", "M4 20L21 3", "M21 16v5h-5", "M15 15l6 6", "M4 4l5 5")
    val Close = listOf("M6 6l12 12", "M18 6L6 18")
    val Left = listOf("M15 5l-7 7 7 7")
    val Right = listOf("M9 5l7 7-7 7")
    val Clock = listOf("M4 13a8 8 0 1 0 16 0a8 8 0 1 0 -16 0", "M12 9v4l2 2", "M9 2h6")
    val Mic = listOf("M9 5a3 3 0 0 1 6 0v6a3 3 0 0 1 -6 0z", "M5 11a7 7 0 0 0 14 0", "M12 18v4")
    val Home = listOf("M3 11l9-7 9 7", "M5 10v10h14V10")
    val Again = listOf("M21 12a9 9 0 1 1-3-6.7", "M21 4v5h-5")
    val Scale = listOf("M12 3v18", "M5 7h14", "M5 7l-3 7a3 3 0 0 0 6 0z", "M19 7l-3 7a3 3 0 0 0 6 0z")
    val Crown = listOf("M3 19 L5 5 L11 12 L15 3 L19 12 L25 5 L27 19 Z")
}

/** 왕관 (30×22 좌표). */
@Composable
fun Crown(modifier: Modifier = Modifier, width: Dp = 30.dp) {
    Canvas(modifier.size(width, width * 22f / 30f)) {
        scale(this.size.width / 30f, pivot = Offset.Zero) {
            val p = svgPath(Icons2.Crown[0])
            drawPath(p, Pal.Sun)
            drawPath(p, Pal.Ink, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
        }
    }
}

/** 아이콘 위 + 글자 아래 정사각 버튼 (상단 도감·기록). */
@Composable
fun IconTileButton(icon: List<String>, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, badge: String? = null) {
    Box(modifier) {
        CartoonButton(
            onClick = onClick, color = Pal.Paper, height = 56.dp, corner = 16.dp, depth = 4.dp,
            contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.width(52.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                InkIcon(icon, size = 22.dp)
                Text(label, fontSize = 12.sp, color = Pal.Ink)
            }
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .height(22.dp)
                    .defaultMinSize(minWidth = 22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Pal.Tomato)
                    .border(2.dp, Pal.Ink, RoundedCornerShape(11.dp))
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) { Text(badge, style = NumberStyle, fontSize = 12.sp, color = Color.White) }
        }
    }
}

/** 둥근 말풍선 / 칩. */
@Composable
fun Bubble(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = Pal.Paper,
    border: Color = Pal.Ink,
    borderW: Dp = 3.dp,
    fontSize: TextUnit = 16.sp,
    color: Color = Pal.Ink,
    corner: Dp = 16.dp,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .border(borderW, border, RoundedCornerShape(corner))
            .padding(padding),
    ) { Text(text, fontSize = fontSize, color = color, maxLines = 1, softWrap = false) }
}

/** 작은 태그 (꼴찌 벌칙, 1등 등). */
@Composable
fun Tag(text: String, bg: Color, fg: Color = Color.White, modifier: Modifier = Modifier, fontSize: TextUnit = 13.sp, number: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(2.dp, Pal.Ink, RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    ) { Text(text, fontSize = fontSize, color = fg, style = if (number) NumberStyle else LocalTextStyle.current, maxLines = 1, softWrap = false) }
}

/** 벌칙 문구에서 "꼴찌가 " 접두를 뗀 짧은 형태. */
fun shortBet(bet: String) = bet.trim().removePrefix("꼴찌가").trim()

/** 받침에 따라 "로/으로"를 붙인다 (ㄹ받침은 "로"). */
fun withRo(word: String): String {
    val ch = word.last()
    if (ch !in '가'..'힣') return word + "로"
    val jong = (ch - '가') % 28
    return word + if (jong == 0 || jong == 8) "로" else "으로"
}
