package com.example.animalrace.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.example.animalrace.R
import com.example.animalrace.ui.sound.Music
import com.example.animalrace.ui.sound.Sfx
import com.example.animalrace.ui.sound.SoundPlayer

/**
 * 효과음은 SoundPool(앱 시작 때 전부 미리 읽음, 다 읽기 전에 부른 소리는 조용히 건너뜀),
 * 배경음악은 MediaPlayer 반복 재생. 전부 메인 스레드에서 부른다.
 */
class AndroidSound(private val context: Context) : SoundPlayer {
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

    /** 메인의 🔊 버튼 (효과음) */
    var sfxOn = true

    /** 메인의 🎵 버튼. 끄면 음악을 멈췄다가, 켜면 지금 화면의 곡을 다시 튼다 */
    var musicOn = true
        set(value) { field = value; applyMusic() }

    override fun play(sfx: Sfx) {
        if (sfxOn) pool.play(ids.getValue(sfx), 1f, 1f, 1, 0, 1f)
    }

    /** 음량 0으로 한 번 틀어 오디오 출력을 깨운다 */
    override fun warmUp() {
        pool.play(ids.getValue(Sfx.TICK), 0f, 0f, 0, 0, 1f)
    }

    // ---------------------------------------------------------------- 배경음악

    private val main = Handler(Looper.getMainLooper())
    private var wanted: Music? = null
    private var playing: Music? = null
    private var player: MediaPlayer? = null
    private var ducked = false
    private var soft = false
    private var background = false

    override fun music(track: Music?, soft: Boolean) {
        wanted = track
        this.soft = soft
        applyMusic()
        player?.setVolume(volume(), volume())
    }

    override fun duck(on: Boolean) {
        ducked = on
        player?.setVolume(volume(), volume())
    }

    /** 앱이 화면에서 사라지면 멈추고, 돌아오면 이어서 */
    fun onBackground() {
        background = true
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun onForeground() {
        background = false
        applyMusic()
        player?.takeIf { !it.isPlaying }?.start()
    }

    /** 액티비티가 끝날 때 */
    fun release() {
        main.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        pool.release()
    }

    private fun volume() = if (ducked) DUCKED else if (soft) SOFT else VOLUME

    private fun applyMusic() {
        val target = if (musicOn) wanted else null
        if (target == playing) return
        // 🎵 버튼으로 끈 건 바로 조용히, 화면이 바뀌어 곡이 끝나는 건 스르르
        player?.let { if (musicOn) fadeOut(it) else { it.stop(); it.release() } }
        player = null
        playing = target
        ducked = false
        if (target == null) return
        player = MediaPlayer.create(context, if (target == Music.MAIN) R.raw.bgm_main else R.raw.bgm_race)?.apply {
            isLooping = true
            setVolume(volume(), volume())
            if (!background) start()
        }
    }

    /** 0.8초 동안 줄였다가 정리 */
    private fun fadeOut(p: MediaPlayer) {
        val from = volume()
        val steps = 10
        for (i in 1..steps) {
            main.postDelayed({
                val v = from * (1f - i.toFloat() / steps)
                if (i < steps) p.setVolume(v, v) else { p.stop(); p.release() }
            }, i * 80L)
        }
    }

    private fun raw(sfx: Sfx) = when (sfx) {
        Sfx.SELECT -> R.raw.sfx_select
        Sfx.DESELECT -> R.raw.sfx_deselect
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

    private companion object {
        /** 배경음악은 효과음보다 작게 */
        const val VOLUME = 0.4f
        const val SOFT = 0.18f
        const val DUCKED = 0.12f
    }
}
