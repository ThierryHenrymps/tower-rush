package com.henry.towerrush.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    init {
        try {
            MobileAds.initialize(context) {
                loadAd()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            "ca-app-pub-3940256099942544/5224354917", // Test Ad Unit ID for Rewarded Ads
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun isAdLoaded(): Boolean = rewardedAd != null

    fun isLoading(): Boolean = isLoading

    fun showAd(activity: Activity, onRewarded: () -> Unit, onFailedOrClosed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { _ ->
                rewardedAd = null
                loadAd() // Preload next
                onRewarded()
            }
        } else {
            onFailedOrClosed()
            loadAd()
        }
    }
}
