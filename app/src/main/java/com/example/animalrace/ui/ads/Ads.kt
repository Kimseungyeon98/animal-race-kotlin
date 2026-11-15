package com.example.animalrace.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * 광고 (6.0, 캔버스 "📢 광고 위치 시안" A + C).
 * 결과 화면 맨 아래 배너, 결과에서 다음으로 넘어갈 때 몇 판마다 전면 광고.
 * 경주·룰렛·미니게임 중에는 띄우지 않고, 보상형 광고도 없다 (광고가 순위·확률에 영향을 주면 안 된다).
 * 실제 구현은 안드로이드 전용 [com.example.animalrace.data.AndroidAds]. 검증 도구·테스트에서는 [NoAds].
 */
interface Ads {
    /** 결과 화면 맨 아래 배너 (못 띄우면 아무것도 그리지 않는다) */
    @Composable
    fun Banner(modifier: Modifier)

    /** 전면 광고를 띄우고, 닫히면(못 띄우면 바로) [then] */
    fun showInterstitial(then: () -> Unit)

    /** 광고 개인정보 동의를 다시 고를 수 있어야 하는 지역(유럽 등)인지. 맞을 때만 정보 창에 "광고 개인정보 설정"이 보인다 (6.1) */
    val privacyOptionsRequired: Boolean get() = false

    /** 광고 개인정보 동의 창을 다시 연다 */
    fun showPrivacyOptions() {}
}

object NoAds : Ads {
    @Composable
    override fun Banner(modifier: Modifier) {}
    override fun showInterstitial(then: () -> Unit) = then()
}

val LocalAds = staticCompositionLocalOf<Ads> { NoAds }

/** 전면 광고 간격: 결과 화면에서 [한 판 더]·[처음으로]로 넘어가는 [EVERY]번째마다 한 번 */
object AdPacing {
    const val EVERY = 3
    fun due(leaves: Int): Boolean = leaves > 0 && leaves % EVERY == 0
}
