package com.example.animalrace.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import java.io.File

// 데스크톱 검증용 대체 (Android에선 res/font). 스크린샷 때는 앱의 진짜 글꼴 파일을 읽는다
private fun appFont(name: String, weight: FontWeight = FontWeight.Normal): FontFamily =
    System.getProperty("animalrace.fonts")?.let { File(it, name) }?.takeIf { it.exists() }
        ?.let { FontFamily(Font(it, weight)) } ?: FontFamily.Default

val Jua: FontFamily = appFont("jua.ttf")
val Fredoka: FontFamily = appFont("fredoka_bold.ttf", FontWeight.Bold)
