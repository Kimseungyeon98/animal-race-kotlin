package com.example.animalrace.ui.sound

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 효과음 (시안: "효과음 키트"). 파일은 `res/raw/sfx_<이름>.ogg` — `tools/sound/make_sfx.sh`가 보드와 같은 레시피로 만든다.
 * 이벤트마다 한 번씩 울리는 소리(틱·드르륵·심장·슥슥)는 반복 한 번 분량이라 화면 쪽에서 박자를 맞춰 부른다.
 */
enum class Sfx {
    SELECT, DESELECT,
    BEEP, GO, FANFARE, DRUM,
    TICK, CLACK, SAFE, BUST,
    PLUCK, SCRATCH, POP, CLICK,
    HEART, SNAP, PHEW, RATCHET, ROLL, TAP, POOF,
}

/** 배경음악 (시안: "배경음악 시안" 메인 B·경기 A). 파일은 `res/raw/bgm_*.ogg`, 끊김 없이 반복 */
enum class Music { MAIN, RACE }

fun interface SoundPlayer {
    fun play(sfx: Sfx)

    /**
     * 소리 장치 깨우기. 폰은 몇 초 조용하면 오디오 출력이 잠들어서, 깨어난 뒤 첫 소리가 늦게 나온다
     * (카운트다운 "3"이 늦게 나와 "2"와 겹침). 박자가 중요한 연출 직전에 부르고 잠깐 기다린다.
     */
    fun warmUp() {}

    /** 배경음악 바꾸기. null이면 스르르 끝. 같은 곡이면 그대로 이어서. [soft]면 더 작게 (미니게임) */
    fun music(track: Music?, soft: Boolean = false) {}

    /** 팡파레처럼 큰 소리가 날 때 배경음악을 잠깐 작게 */
    fun duck(on: Boolean) {}

    companion object {
        val Silent = SoundPlayer { }
    }
}

/** 화면에서 `LocalSound.current.play(Sfx.GO)`. 기본은 무음(미리보기·검증용), 앱에서는 MainActivity가 실제 재생기를 넣는다 */
val LocalSound = staticCompositionLocalOf { SoundPlayer.Silent }
