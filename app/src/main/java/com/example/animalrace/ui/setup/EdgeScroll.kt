package com.example.animalrace.ui.setup

/**
 * 이름표를 끄는 동안 동물 목록 위·아래 가장자리에서 저절로 스크롤할 빠르기 (6.1).
 * 목록 밖(위쪽 이름표 줄)에서 끌고 들어올 때는 가장자리를 지나가도 스크롤하지 않는다:
 * 손가락이 목록 **안쪽**(가장자리 띠를 벗어난 곳)에 한 번 들어온 뒤에야 그 가장자리가 켜진다.
 * 끌기 한 번마다 새로 만든다.
 */
internal class EdgeScroll(private val edge: Float) {
    private var topArmed = false
    private var bottomArmed = false

    /** 손가락 세로 위치 [y], 목록의 위 [top]·아래 [bottom] (같은 좌표계 px) → -1(위로)..1(아래로), 스크롤 안 하면 0 */
    fun speed(y: Float, top: Float, bottom: Float): Float {
        if (y in top..bottom) {
            if (y > top + edge) topArmed = true
            if (y < bottom - edge) bottomArmed = true
        }
        val down = (y - (bottom - edge)) / edge
        val up = ((top + edge) - y) / edge
        return when {
            bottomArmed && down > 0f && y <= bottom + edge -> down.coerceAtMost(1f)
            topArmed && up > 0f && y >= top - edge -> -up.coerceAtMost(1f)
            else -> 0f
        }
    }
}
