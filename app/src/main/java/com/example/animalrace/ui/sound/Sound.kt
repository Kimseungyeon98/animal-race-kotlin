package com.example.animalrace.ui.sound

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 효과음 (시안: "효과음 키트"). 파일은 `res/raw/sfx_<이름>.ogg` — `tools/sound/make_sfx.sh`가 보드와 같은 레시피로 만든다.
 * 이벤트마다 한 번씩 울리는 소리(틱·드르륵·심장·슥슥)는 반복 한 번 분량이라 화면 쪽에서 박자를 맞춰 부른다.
 */
enum class Sfx {
    BEEP, GO, FANFARE, DRUM,
    TICK, CLACK, SAFE, BUST,
    PLUCK, SCRATCH, POP, CLICK,
    HEART, SNAP, PHEW, RATCHET, ROLL, TAP, POOF,
}

fun interface SoundPlayer {
    fun play(sfx: Sfx)

    companion object {
        val Silent = SoundPlayer { }
    }
}

/** 화면에서 `LocalSound.current.play(Sfx.GO)`. 기본은 무음(미리보기·검증용), 앱에서는 MainActivity가 실제 재생기를 넣는다 */
val LocalSound = staticCompositionLocalOf { SoundPlayer.Silent }
