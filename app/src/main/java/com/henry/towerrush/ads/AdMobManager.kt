package com.henry.towerrush.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isRewardedLoading = false
    private var isInterstitialLoading = false

    companion object {
        // Mude para false ao publicar em produção
        var useTestAds: Boolean = true

        // IDs Reais de Produção
        private const val PROD_REWARDED_ID = "ca-app-pub-9916078396917448/1540163900"
        private const val PROD_INTERSTITIAL_ID = "ca-app-pub-9916078396917448/4904693848"

        // IDs Oficiais de Teste do Google
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    }

    init {
        try {
            MobileAds.initialize(context) {
                loadRewardedAd()
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadRewardedAd() {
        if (isRewardedLoading || rewardedAd != null) return
        isRewardedLoading = true
        val adUnitId = if (useTestAds) TEST_REWARDED_ID else PROD_REWARDED_ID
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                }
            }
        )
    }

    fun loadInterstitialAd() {
        if (isInterstitialLoading || interstitialAd != null) return
        isInterstitialLoading = true
        val adUnitId = if (useTestAds) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun isRewardedLoaded(): Boolean = rewardedAd != null
    fun isRewardedLoading(): Boolean = isRewardedLoading

    fun isInterstitialLoaded(): Boolean = interstitialAd != null

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onFailedOrClosed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { _ ->
                rewardedAd = null
                loadRewardedAd()
                onRewarded()
            }
        } else {
            onFailedOrClosed()
            loadRewardedAd()
        }
    }

    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdClosed()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd()
            onAdClosed()
        }
    }
}
