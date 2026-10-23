package com.example.animalrace.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.animalrace.R
import com.example.animalrace.ui.sound.Sfx
import com.example.animalrace.ui.sound.SoundPlayer

/** SoundPool로 짧은 효과음 재생. 앱 시작 때 전부 미리 읽어 두고(약 200KB), 다 읽기 전에 부른 소리는 조용히 건너뛴다 */
class AndroidSound(context: Context) : SoundPlayer {
    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = Sfx.entries.associateWith { pool.load(context, raw(it), 1) }

    override fun play(sfx: Sfx) {
        pool.play(ids.getValue(sfx), 1f, 1f, 1, 0, 1f)
    }

    private fun raw(sfx: Sfx) = when (sfx) {
        Sfx.BEEP -> R.raw.sfx_beep
        Sfx.GO -> R.raw.sfx_go
        Sfx.FANFARE -> R.raw.sfx_fanfare
        Sfx.DRUM -> R.raw.sfx_drum
        Sfx.TICK -> R.raw.sfx_tick
        Sfx.CLACK -> R.raw.sfx_clack
        Sfx.SAFE -> R.raw.sfx_safe
        Sfx.BUST -> R.raw.sfx_bust
        Sfx.PLUCK -> R.raw.sfx_pluck
        Sfx.SCRATCH -> R.raw.sfx_scratch
        Sfx.POP -> R.raw.sfx_pop
        Sfx.CLICK -> R.raw.sfx_click
        Sfx.HEART -> R.raw.sfx_heart
        Sfx.SNAP -> R.raw.sfx_snap
        Sfx.PHEW -> R.raw.sfx_phew
        Sfx.RATCHET -> R.raw.sfx_ratchet
        Sfx.ROLL -> R.raw.sfx_roll
        Sfx.TAP -> R.raw.sfx_tap
        Sfx.POOF -> R.raw.sfx_poof
    }
}
