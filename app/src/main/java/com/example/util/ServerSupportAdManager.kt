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
    private var loadedRoomRewardedAd: RewardedAd? = null
    private var loadedFreezeRewardedAd: RewardedAd? = null
    private var isLoadingRoomAd: Boolean = false
    private var isLoadingFreezeAd: Boolean = false
    private var lastAppContext: Context? = null

    /**
     * Preload all Google AdMob Rewarded Ads so they are instantly ready when user prompts occur.
     */
    fun preloadAd(context: Context) {
        val appContext = context.applicationContext
        lastAppContext = appContext
        preloadRoomAd(appContext)
        preloadFreezeAd(appContext)
    }

    /**
     * Preload Rewarded Ad specifically for Online Distribution Rooms & Streak Circles.
     */
    fun preloadRoomAd(context: Context) {
        val appContext = context.applicationContext
        if (loadedRoomRewardedAd != null || isLoadingRoomAd) return

        isLoadingRoomAd = true
        val adUnitId = BuildConfig.ADMOB_ROOM_REWARDED_AD_UNIT_ID.ifBlank { DEFAULT_TEST_REWARDED_AD_UNIT }
        Log.d(TAG, "AdMob: Preloading Room/Circle Rewarded Ad Unit: $adUnitId")
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingRoomAd = false
                    loadedRoomRewardedAd = ad
                    Log.d(TAG, "AdMob: Room/Circle RewardedAd successfully cached and ready.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingRoomAd = false
                    loadedRoomRewardedAd = null
                    Log.w(TAG, "AdMob: Room/Circle RewardedAd failed to load: ${loadAdError.message} (code ${loadAdError.code})")
                }
            }
        )
    }

    /**
     * Preload Rewarded Ad specifically for Streak Freeze Shield Drops.
     */
    fun preloadFreezeAd(context: Context) {
        val appContext = context.applicationContext
        if (loadedFreezeRewardedAd != null || isLoadingFreezeAd) return

        isLoadingFreezeAd = true
        val adUnitId = BuildConfig.ADMOB_FREEZE_REWARDED_AD_UNIT_ID.ifBlank { DEFAULT_TEST_REWARDED_AD_UNIT }
        Log.d(TAG, "AdMob: Preloading Streak Freeze Rewarded Ad Unit: $adUnitId")
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingFreezeAd = false
                    loadedFreezeRewardedAd = ad
                    Log.d(TAG, "AdMob: Streak Freeze RewardedAd successfully cached and ready.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingFreezeAd = false
                    loadedFreezeRewardedAd = null
                    Log.w(TAG, "AdMob: Streak Freeze RewardedAd failed to load: ${loadAdError.message} (code ${loadAdError.code})")
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

        lastAppContext?.let { preloadRoomAd(it) }
    }

    /**
     * User accepted the voluntary prompt: "Yes, I'd love to help support the servers!"
     * Tries to show the real Google AdMob RewardedAd for room/circle support first.
     * If not ready or Activity is not available, falls back to the in-app 30-second player.
     */
    fun acceptVoluntaryAd(context: Context? = null, scope: CoroutineScope) {
        _showServerSupportPrompt.value = false
        val activity = context?.let { findActivity(it) }
        val ad = loadedRoomRewardedAd ?: loadedFreezeRewardedAd

        if (ad != null && activity != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AdMob: Room/Circle RewardedAd is showing full-screen.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AdMob: Room/Circle RewardedAd was dismissed by user.")
                    loadedRoomRewardedAd = null
                    _showGratitudeDialog.value = true
                    preloadRoomAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AdMob: Room/Circle RewardedAd failed to show: ${adError.message}. Falling back to in-app player.")
                    loadedRoomRewardedAd = null
                    playFallbackCountdown(scope)
                    preloadRoomAd(activity)
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "AdMob: User completed Room/Circle voluntary ad reward: ${rewardItem.type} amount: ${rewardItem.amount}")
            }
        } else {
            // Ad is still caching or network unavailable; run the fallback in-app 30-second countdown
            context?.let { preloadRoomAd(it) }
            playFallbackCountdown(scope)
        }
    }

    /**
     * Shows a rewarded ad specifically for Streak Freeze Drops or voluntary support.
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (Boolean) -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = loadedFreezeRewardedAd ?: loadedRoomRewardedAd
        if (ad != null) {
            var rewarded = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadedFreezeRewardedAd = null
                    preloadFreezeAd(activity)
                    onUserEarnedReward(rewarded)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    loadedFreezeRewardedAd = null
                    preloadFreezeAd(activity)
                    // Grant reward on display failure so user is never penalized
                    onUserEarnedReward(true)
                    onAdDismissed()
                }
            }
            ad.show(activity) { _ ->
                rewarded = true
            }
        } else {
            preloadFreezeAd(activity)
            // If ad is buffering or in unit tests, grant reward so flow continues seamlessly
            onUserEarnedReward(true)
            onAdDismissed()
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
