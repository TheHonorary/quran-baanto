package com.example.util

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
 * - If accepted, displays the 30-second server support ad view with countdown.
 * - If skipped or declined, dismissed immediately with zero penalties or restrictions.
 */
object ServerSupportAdManager {

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

    /**
     * Called when an online room is successfully created or joined.
     */
    fun triggerPromptForOnlineRoom(roomName: String, isCreation: Boolean) {
        _pendingRoomName.value = roomName.ifBlank { "Online Khatm Room" }
        _isRoomCreation.value = isCreation
        _showServerSupportPrompt.value = true
    }

    /**
     * User accepted the voluntary prompt: "Yes, I'd love to help support the servers!"
     */
    fun acceptVoluntaryAd(scope: CoroutineScope) {
        _showServerSupportPrompt.value = false
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
     * Close the ad player (if user exits early or finishes).
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
}
