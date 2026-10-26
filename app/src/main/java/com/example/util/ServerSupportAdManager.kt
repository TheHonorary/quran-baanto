package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the permissionary, voluntary server-support ad experience for Quran Baanto.
 *
 * Strict Architecture:
 * - ZERO ads on Quran Reader.
 * - ZERO ads on Home screen.
 * - ZERO banner ads anywhere on distribution pages.
 * - Non-intrusive & permissionary: ONLY asks after creating or joining an online room
 *   if the user would like to watch a brief 30-second sponsored message to help
 *   cover server & database costs.
 * - Plays real Google AdMob Rewarded Ads on physical Android devices.
 * - Falls back to a clean in-app 30-second timer if ads are still loading or network is offline.
 */
object ServerSupportAdManager {

    private const val TAG = "ServerSupportAdManager"
    private const val DEFAULT_TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917"

    private val _showServerSupportPrompt = MutableStateFlow(false)
    val showServerSupportPrompt: StateFlow<Boolean> = _showServerSupportPrompt.asStateFlow()

    private val _isAdPlaying = MutableStateFlow(false)
    val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

    private val _adCountdownSeconds = MutableStateFlow(30)
    val adCountdownSeconds: StateFlow<Int> = _adCountdownSeconds.asStateFlow()

    private val _showGratitudeDialog = MutableStateFlow(false)
    val showGratitudeDialog: StateFlow<Boolean> = _showGratitudeDialog.asStateFlow()

    private val _pendingRoomName = MutableStateFlow("")
    val pendingRoomName: StateFlow<String> = _pendingRoomName.asStateFlow()

    private val _isRoomCreation = MutableStateFlow(true)
    val isRoomCreation: StateFlow<Boolean> = _isRoomCreation.asStateFlow()

    private var countdownJob: Job? = null
    private var loadedRewardedAd: RewardedAd? = null
    private var isLoadingAd: Boolean = false
    private var lastAppContext: Context? = null

    /**
     * Preload Google AdMob Rewarded Ad so it is ready instantly when user accepts.
     */
    fun preloadAd(context: Context) {
        val appContext = context.applicationContext
        lastAppContext = appContext
        if (loadedRewardedAd != null || isLoadingAd) return

        isLoadingAd = true
        val adUnitId = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID.ifBlank { DEFAULT_TEST_REWARDED_AD_UNIT }
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingAd = false
                    loadedRewardedAd = ad
                    Log.d(TAG, "AdMob RewardedAd successfully cached and ready to display.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    loadedRewardedAd = null
                    Log.w(TAG, "AdMob RewardedAd failed to load: ${loadAdError.message} (code ${loadAdError.code})")
                }
            }
        )
    }

    /**
     * Called when an online room is successfully created or joined.
     */
    fun triggerPromptForOnlineRoom(roomName: String, isCreation: Boolean) {
        _pendingRoomName.value = roomName.ifBlank { "Online Khatm Room" }
        _isRoomCreation.value = isCreation
        _showServerSupportPrompt.value = true

        // Try preloading if not yet loaded
        lastAppContext?.let { preloadAd(it) }
    }

    /**
     * User accepted the voluntary prompt: "Yes, I'd love to help support the servers!"
     * Tries to show the real Google AdMob RewardedAd first.
     * If not ready or Activity is not available, falls back to the in-app 30-second player.
     */
    fun acceptVoluntaryAd(context: Context? = null, scope: CoroutineScope) {
        _showServerSupportPrompt.value = false
        val activity = context?.let { findActivity(it) }
        val ad = loadedRewardedAd

        if (ad != null && activity != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AdMob RewardedAd is showing full-screen.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AdMob RewardedAd was dismissed by user.")
                    loadedRewardedAd = null
                    _showGratitudeDialog.value = true
                    // Cache the next ad in the background
                    preloadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AdMob RewardedAd failed to show: ${adError.message}. Falling back to in-app player.")
                    loadedRewardedAd = null
                    playFallbackCountdown(scope)
                    preloadAd(activity)
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User completed voluntary ad reward: ${rewardItem.type} amount: ${rewardItem.amount}")
            }
        } else {
            // Ad is still caching or network unavailable; run the fallback in-app 30-second countdown
            context?.let { preloadAd(it) }
            playFallbackCountdown(scope)
        }
    }

    /**
     * Backwards-compatible overload for callers without context.
     */
    fun acceptVoluntaryAd(scope: CoroutineScope) {
        acceptVoluntaryAd(lastAppContext, scope)
    }

    private fun playFallbackCountdown(scope: CoroutineScope) {
        _isAdPlaying.value = true
        _adCountdownSeconds.value = 30

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (sec in 30 downTo 1) {
                _adCountdownSeconds.value = sec
                delay(1000L)
            }
            _adCountdownSeconds.value = 0
            _isAdPlaying.value = false
            _showGratitudeDialog.value = true
        }
    }

    /**
     * User chose to skip or dismiss the voluntary prompt.
     */
    fun dismissPrompt() {
        _showServerSupportPrompt.value = false
    }

    /**
     * Close the fallback ad player (if user exits early or finishes).
     */
    fun closeAdPlayer() {
        countdownJob?.cancel()
        countdownJob = null
        _isAdPlaying.value = false
    }

    /**
     * Dismiss the thank you / gratitude dialog.
     */
    fun dismissGratitude() {
        _showGratitudeDialog.value = false
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }
}
