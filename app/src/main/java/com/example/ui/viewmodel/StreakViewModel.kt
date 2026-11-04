package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FreezeShieldAdResult
import com.example.data.StreakRepository
import com.example.data.db.AppDatabase
import com.example.data.db.DailyLogEntity
import com.example.data.db.PlanType
import com.example.data.db.ReadingPlanEntity
import com.example.data.db.StreakCircleEntity
import com.example.data.db.StreakCircleMemberEntity
import com.example.data.db.StreakEntity
import com.example.util.ServerSupportAdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class StreakViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesDataStore = UserPreferencesDataStore.getInstance(application.applicationContext)
    val userProfileName: StateFlow<String> = preferencesDataStore.userSettingsFlow
        .map { it.userProfileName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val streakDao = AppDatabase.getDatabase(application).streakDao()
    val streakRepository = StreakRepository(streakDao)

    val streakState: StateFlow<StreakEntity?> = streakRepository.streakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePlansState: StateFlow<List<ReadingPlanEntity>> = streakRepository.activePlansFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlansState: StateFlow<List<ReadingPlanEntity>> = streakRepository.allPlansFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streakCirclesState: StateFlow<List<StreakCircleEntity>> = streakRepository.streakCirclesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogsState: StateFlow<List<DailyLogEntity>> = streakRepository.recentDailyLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayLog = MutableStateFlow<DailyLogEntity?>(null)
    val todayLog: StateFlow<DailyLogEntity?> = _todayLog.asStateFlow()

    private val _adWatchStatus = MutableStateFlow<String?>(null)
    val adWatchStatus: StateFlow<String?> = _adWatchStatus.asStateFlow()

    init {
        viewModelScope.launch {
            streakRepository.evaluateStreakRollover()
            refreshTodayLog()
        }
    }

    fun refreshTodayLog() {
        viewModelScope.launch {
            val log = streakDao.getDailyLogDirect(streakRepository.getTodayKey())
            _todayLog.value = log
        }
    }

    fun optInToStreak(dailyTargetAyahs: Int = 10) {
        viewModelScope.launch {
            streakRepository.optInToStreak(dailyTargetAyahs)
            refreshTodayLog()
        }
    }

    fun recordAyahRecitation(
        ayahsCount: Int = 1,
        surahNumber: Int,
        ayahNumber: Int,
        paraNumber: Int,
        surahName: String
    ) {
        viewModelScope.launch {
            val currentStreak = streakState.value
            if (currentStreak?.isOptedIn == true) {
                streakRepository.recordAyahRecitation(
                    ayahsIncrement = ayahsCount,
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    paraNumber = paraNumber,
                    surahName = surahName
                )
                refreshTodayLog()
            }
        }
    }

    fun watchRewardedAdForFreeze(
        activity: Activity?,
        onSuccess: (FreezeShieldAdResult) -> Unit,
        onAdNotReady: () -> Unit
    ) {
        val currentStreak = streakState.value ?: return

        // Check if shield was used recently (within 7 days)
        if (!streakRepository.canUseFreezeShieldCooldown(currentStreak.lastFreezeShieldUsedDate)) {
            val remaining = streakRepository.getDaysUntilNextFreezeShieldAvailable(currentStreak.lastFreezeShieldUsedDate)
            onSuccess(FreezeShieldAdResult.CooldownActive(remaining))
            return
        }

        if (activity == null) {
            onAdNotReady()
            return
        }

        _adWatchStatus.value = "Loading ad..."
        ServerSupportAdManager.showRewardedAd(
            activity = activity,
            onUserEarnedReward = { rewardEarned: Boolean ->
                if (rewardEarned) {
                    viewModelScope.launch {
                        val result = streakRepository.recordAdWatchedForFreezeShield()
                        _adWatchStatus.value = when (result) {
                            is FreezeShieldAdResult.Progress -> "Ad watched (${result.adsWatched}/${result.adsNeeded}). Watch ${result.adsNeeded - result.adsWatched} more to unlock a Freeze Pod!"
                            is FreezeShieldAdResult.ShieldEarned -> "Shield Earned! Your 7-day streak protection pod is activated."
                            is FreezeShieldAdResult.CooldownActive -> "Cooldown Active: ${result.daysRemaining} days remaining before next freeze pod."
                        }
                        onSuccess(result)
                    }
                } else {
                    _adWatchStatus.value = "Ad skipped or closed before completion."
                }
            },
            onAdDismissed = {
                // Ad completed
            }
        )
    }

    fun clearAdStatus() {
        _adWatchStatus.value = null
    }

    fun createDailyAyahPlan(title: String, dailyGoal: Int, durationDays: Int) {
        viewModelScope.launch {
            streakRepository.createReadingPlan(
                title = title,
                planType = PlanType.DAILY_AYAH_GOAL,
                dailyTargetAyahs = dailyGoal,
                totalDays = durationDays
            )
        }
    }

    fun createParaRangePlan(title: String, startPara: Int, endPara: Int, durationDays: Int) {
        val parasCsv = (startPara..endPara).joinToString(",")
        viewModelScope.launch {
            streakRepository.createReadingPlan(
                title = title,
                planType = PlanType.PARA_RANGE_TARGET,
                dailyTargetAyahs = 10,
                totalDays = durationDays,
                targetParasCsv = parasCsv
            )
        }
    }

    fun createStreakCircle(
        name: String,
        creatorName: String,
        onCreated: (StreakCircleEntity) -> Unit
    ) {
        viewModelScope.launch {
            val circleId = streakRepository.createStreakCircle(name, creatorName)
            val circle = streakDao.getCircleByIdDirect(circleId)
            if (circle != null) {
                onCreated(circle)
            }
        }
    }

    fun joinStreakCircle(
        passcode: String,
        memberName: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            val circleId = streakRepository.joinStreakCircle(passcode, memberName)
            if (circleId != null) {
                val circle = streakDao.getCircleByIdDirect(circleId)
                onResult(true, "Successfully joined '${circle?.name ?: "Circle"}'!")
            } else {
                onResult(false, "Circle with passcode '$passcode' was not found.")
            }
        }
    }

    fun formatShareCircle(circle: StreakCircleEntity, onFormatted: (String) -> Unit) {
        viewModelScope.launch {
            val members = streakRepository.getMembersForCircle(circle.id).firstOrNull().orEmpty()
            val text = streakRepository.formatStreakCircleShareText(circle, members)
            onFormatted(text)
        }
    }

    fun getCircleMembers(circleId: Long): kotlinx.coroutines.flow.Flow<List<StreakCircleMemberEntity>> {
        return streakRepository.getMembersForCircle(circleId)
    }
}
