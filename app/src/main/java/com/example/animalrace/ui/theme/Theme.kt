package com.example.animalrace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** B안 카툰 캐주얼 팔레트. 밝은 테마 하나로 고정 (시스템 다크 모드와 무관). */
object Pal {
    val Sky = Color(0xFF8ED6FF)
    val Grass = Color(0xFF7ED37A)
    val HillLight = Color(0xFF6CC46B)
    val HillDark = Color(0xFF5DB65E)
    val Dirt = Color(0xFFF2C98A)
    val DirtAlt = Color(0xFFEDC283)
    val DirtLine = Color(0xFFE0AE6C)
    val Paper = Color(0xFFFFFFFF)
    val Cream = Color(0xFFFFF4D6)
    val Ivory = Color(0xFFFFFDF6)
    val Ink = Color(0xFF2B2140)
    val InkSoft = Color(0xFF5D5272)
    val Line = Color(0xFFE4DEEC)
    val Sun = Color(0xFFFFC93C)
    val SunPale = Color(0xFFFFF1C4)
    val Tomato = Color(0xFFF2543F)
    val TomatoText = Color(0xFFB23A2A)
    val Pink = Color(0xFFFFE1DC)
    val Mint = Color(0xFFEAF7EA)
    val Disabled = Color(0xFFC9C3D6)
    val DisabledInk = Color(0xFF6A5F80)
    val Silver = Color(0xFFC9D2DC)
    val Bronze = Color(0xFFE3A26F)
}

/** 숫자(시간·개수·순위)용. */
val NumberStyle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold)

private fun jua(): Typography {
    val b = Typography()
    fun TextStyle.j() = copy(fontFamily = Jua, fontWeight = FontWeight.Normal)
    return Typography(
        displayLarge = b.displayLarge.j(), displayMedium = b.displayMedium.j(), displaySmall = b.displaySmall.j(),
        headlineLarge = b.headlineLarge.j(), headlineMedium = b.headlineMedium.j(), headlineSmall = b.headlineSmall.j(),
        titleLarge = b.titleLarge.j(), titleMedium = b.titleMedium.j(), titleSmall = b.titleSmall.j(),
        bodyLarge = b.bodyLarge.j(), bodyMedium = b.bodyMedium.j(), bodySmall = b.bodySmall.j(),
        labelLarge = b.labelLarge.j(), labelMedium = b.labelMedium.j(), labelSmall = b.labelSmall.j(),
    )
}

@Composable
fun AnimalRaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pal.Tomato, onPrimary = Color.White,
            secondary = Pal.Sun, onSecondary = Pal.Ink,
            background = Pal.Sky, onBackground = Pal.Ink,
            surface = Pal.Ivory, onSurface = Pal.Ink, onSurfaceVariant = Pal.InkSoft,
            surfaceContainer = Pal.Ivory, surfaceContainerHigh = Pal.Cream,
            outline = Pal.Ink, error = Pal.Tomato,
        ),
        typography = jua(),
        content = content,
    )
}

/** 출전 번호 색 — 실제 경마의 1~10번 표준 색 (배경, 글자). */
val LaneCloth = listOf(
    Color(0xFFD7262B) to Color.White,
    Color(0xFFFFFFFF) to Color(0xFF111111),
    Color(0xFF1F4FB8) to Color.White,
    Color(0xFFF4D000) to Color(0xFF111111),
    Color(0xFF1D8A3A) to Color.White,
    Color(0xFF111111) to Color(0xFFF4D000),
    Color(0xFFF07C1F) to Color(0xFF111111),
    Color(0xFFF39AC0) to Color(0xFF111111),
    Color(0xFF2BB3B1) to Color(0xFF111111),
    Color(0xFF6B2A8F) to Color.White,
)
