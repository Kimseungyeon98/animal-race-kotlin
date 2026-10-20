package com.example.animalrace.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.model.Animal
import com.example.animalrace.ui.theme.LaneCloth
import com.example.animalrace.ui.theme.NumberStyle

/** 출전 번호판. 실제 경마 새들클로스 색을 쓴다. */
@Composable
fun LaneBadge(lane: Int, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val (bg, fg) = LaneCloth[lane % LaneCloth.size]
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(bg)
            .border(1.dp, Color.Black.copy(alpha = 0.18f), RoundedCornerShape(size * 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("${lane + 1}", color = fg, style = NumberStyle, fontSize = (size.value * 0.5f).sp)
    }
}

/** 동물 대표색을 은은하게 깐 원형 배지 + 이모지. */
@Composable
fun AnimalBadge(animal: Animal, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val tint = Color(animal.color)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.32f), tint.copy(alpha = 0.12f))))
            .border(1.5.dp, tint.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(animal.emoji, fontSize = (size.value * 0.52f).sp)
    }
}

/** 작은 알약 모양 라벨. */
@Composable
fun Pill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text, color = content,
            style = if (mono) NumberStyle.copy(fontSize = 12.sp) else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 눌렀을 때 살짝 들어가는 큰 실행 버튼. */
@Composable
fun BigButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    label: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f), label = "press")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = modifier.scale(scale).defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, content = label)
    }
}

/** 결승선 체커 무늬. */
@Composable
fun Checkers(modifier: Modifier = Modifier, cell: Dp = 6.dp) {
    Canvas(modifier) {
        val c = cell.toPx()
        clipRect {
            var y = 0f
            var row = 0
            while (y < size.height) {
                var x = 0f
                var col = 0
                while (x < size.width) {
                    drawRect(
                        if ((row + col) % 2 == 0) Color(0xFF111111) else Color.White,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(c, c),
                    )
                    x += c; col++
                }
                y += c; row++
            }
        }
    }
}
