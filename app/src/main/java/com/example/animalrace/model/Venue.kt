package com.example.animalrace.model

/** 경기장. 그림(색·소품)은 [com.example.animalrace.ui.race.Stadium]에 있고, 기록에는 이 값만 남긴다. */
enum class Venue(val emoji: String, val title: String) {
    MEADOW("🌿", "초원"),
    BEACH("🏖️", "해변"),
    SNOW("❄️", "설원"),
    DESERT("🌵", "사막");

    /** 카운트다운 이름표 등: "🏖️ 해변 경기장" */
    val label get() = "$emoji $title 경기장"
}
