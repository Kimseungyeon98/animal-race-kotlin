package com.example.animalrace.data

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.example.animalrace.R
import com.example.animalrace.ui.ads.Ads
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob 광고 (6.0). 광고 단위 ID는 `res/values/ads.xml` (지금은 구글 테스트 ID).
 * 앱을 켜면 [start]: 개인정보 동의가 필요한 지역이면 동의 창 → 광고를 요청해도 되면 SDK 시작.
 * 광고를 못 받아도(인터넷 없음, 동의 안 함, 아직 로딩 중) 게임은 그대로: 배너는 안 보이고 전면 광고는 건너뛴다.
 */
class AndroidAds(private val activity: Activity) : Ads {
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private val started = AtomicBoolean(false)

    /** SDK가 준비돼 광고를 요청할 수 있는지 (배너는 이때부터 그린다) */
    private var ready by mutableStateOf(false)
    private var interstitial: InterstitialAd? = null

    fun start() {
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initIfAllowed() } },
            { initIfAllowed() },
        )
        // 예전에 이미 동의했으면 정보 갱신을 기다리지 않고 바로
        initIfAllowed()
    }

    private fun initIfAllowed() {
        if (!consent.canRequestAds() || !started.compareAndSet(false, true)) return
        MobileAds.initialize(activity) {
            ready = true
            loadInterstitial()
        }
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            activity, activity.getString(R.string.admob_interstitial), AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
            },
        )
    }

    override fun showInterstitial(then: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            // 아직 안 받아 왔으면 기다리게 하지 않고 바로 넘어간다
            if (ready) loadInterstitial()
            then()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { then(); loadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { then(); loadInterstitial() }
        }
        ad.show(activity)
    }

    @Composable
    override fun Banner(modifier: Modifier) {
        if (!ready) return
        val widthDp = LocalConfiguration.current.screenWidthDp
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                    adUnitId = ctx.getString(R.string.admob_banner)
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
