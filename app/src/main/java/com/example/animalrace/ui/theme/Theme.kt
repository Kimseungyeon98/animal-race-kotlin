package com.example.animalrace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Material 색 외에 경주장 전용 색 (잔디·레일·금은동). */
@Immutable
data class RaceColors(
    val turfLight: Color,
    val turfDark: Color,
    val turfEdge: Color,
    val rail: Color,
    val gold: Color,
    val silver: Color,
    val bronze: Color,
    val penalty: Color,
    val penaltyContainer: Color,
)

private val LightRace = RaceColors(
    turfLight = Color(0xFF4A9A55), turfDark = Color(0xFF428C4C), turfEdge = Color(0xFF2B5F33),
    rail = Color(0xFFF7F5EC), gold = Color(0xFFE2A400), silver = Color(0xFF9AA5B1), bronze = Color(0xFFC07A45),
    penalty = Color(0xFFC0352A), penaltyContainer = Color(0xFFFBE3DF),
)
private val DarkRace = RaceColors(
    turfLight = Color(0xFF337A3F), turfDark = Color(0xFF2D6E38), turfEdge = Color(0xFF173D1E),
    rail = Color(0xFFD9D7CC), gold = Color(0xFFF3C440), silver = Color(0xFFB7C1CC), bronze = Color(0xFFD9925C),
    penalty = Color(0xFFFF7B6B), penaltyContainer = Color(0xFF3D1C18),
)

val LocalRaceColors = staticCompositionLocalOf { LightRace }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF17633A), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EBDB), onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFFB98500), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7A8), onSecondaryContainer = Color(0xFF2A1F00),
    background = Color(0xFFF0F3ED), onBackground = Color(0xFF131D17),
    surface = Color(0xFFF0F3ED), onSurface = Color(0xFF131D17),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8FAF6),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFE6ECE4),
    surfaceVariant = Color(0xFFE1E8DF), onSurfaceVariant = Color(0xFF56645A),
    outline = Color(0xFFBCC8BD), outlineVariant = Color(0xFFD7DFD5),
    error = Color(0xFFC0352A),
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6CCB8F), onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF1F4A2E), onPrimaryContainer = Color(0xFFC8F0D4),
    secondary = Color(0xFFF3C440), onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF5A4500), onSecondaryContainer = Color(0xFFFFE7A8),
    background = Color(0xFF0B120E), onBackground = Color(0xFFE2EAE3),
    surface = Color(0xFF0B120E), onSurface = Color(0xFFE2EAE3),
    surfaceContainerLowest = Color(0xFF070C09), surfaceContainerLow = Color(0xFF111A14),
    surfaceContainer = Color(0xFF152019), surfaceContainerHigh = Color(0xFF1D2A21),
    surfaceVariant = Color(0xFF223027), onSurfaceVariant = Color(0xFFA2B2A6),
    outline = Color(0xFF3E5045), outlineVariant = Color(0xFF2A382F),
    error = Color(0xFFFF7B6B),
)

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.display() = copy(fontFamily = DisplayFont, fontWeight = FontWeight.Normal)
    return base.copy(
        displayLarge = base.displayLarge.display(),
        displayMedium = base.displayMedium.display(),
        displaySmall = base.displaySmall.display(),
        headlineLarge = base.headlineLarge.display(),
        headlineMedium = base.headlineMedium.display(),
        headlineSmall = base.headlineSmall.display(),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.4.sp),
    )
}

/** 숫자(기록·시간)용 고정폭 스타일. */
val NumberStyle = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold)

@Composable
fun AnimalRaceTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRaceColors provides if (dark) DarkRace else LightRace) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = typography(),
            content = content,
        )
    }
}

/** 출전 번호(새들클로스) 색 — 실제 경마의 1~10번 표준 색 */
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
