package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DistributionRepository
import com.example.data.db.AppDatabase
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.data.db.RoomMode
import com.example.data.firebase.FirebaseConnectionStatus
import com.example.data.firebase.FirebaseCoreManager
import com.example.data.firebase.FirebaseUserProfile
import com.example.data.preferences.UserPreferencesDataStore
import com.example.domain.ParaAssignment
import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import com.example.util.HijriDateUtils
import com.example.util.KhatmNotificationHelper
import com.example.util.MonthDateInfo
import com.example.util.ServerSupportAdManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ActiveDistributionUiState(
    val distribution: DistributionEntity? = null,
    val participants: List<ParticipantEntity> = emptyList(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1, // 1..12
    val assignments: List<ParaAssignment> = emptyList()
) {
    val isAmal: Boolean
        get() = distribution?.type == DistributionType.AMAL

    val isOneTimeKhatm: Boolean
        get() = distribution?.type == DistributionType.ONE_TIME_KHATM

    val isMonthlyKhatm: Boolean
        get() = distribution?.type == DistributionType.KHATM

    val dateInfo: MonthDateInfo
        get() = HijriDateUtils.getMonthInfo(selectedYear, selectedMonth)

    val monthName: String
        get() = dateInfo.gregorianMonthName

    val hijriMonthName: String
        get() = dateInfo.hijriFullText

    val cycleNumber: Int
        get() {
            val dist = distribution ?: return 1
            val offset = (selectedYear - dist.startYear) * 12 + (selectedMonth - dist.startMonth)
            return offset + 1
        }

    val shareText: String
        get() {
            val dist = distribution ?: return ""
            return DistributionViewModel.formatDistributionShareSummary(
                distribution = dist,
                participants = participants,
                assignments = assignments,
                targetYear = selectedYear,
                targetMonth = selectedMonth
            )
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DistributionViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val preferencesDataStore = UserPreferencesDataStore.getInstance(appContext)
    private val repository: DistributionRepository =
        DistributionRepository(AppDatabase.getDatabase(application).distributionDao())

    private val _isLoadingDistributions = MutableStateFlow(true)
    val isLoadingDistributions: StateFlow<Boolean> = _isLoadingDistributions.asStateFlow()

    val userProfileName: StateFlow<String> = preferencesDataStore.userSettingsFlow
        .map { it.userProfileName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val notificationsEnabled: StateFlow<Boolean> = preferencesDataStore.userSettingsFlow
        .map { it.notificationsEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val hasSeenNotificationPrompt: StateFlow<Boolean> = preferencesDataStore.userSettingsFlow
        .map { it.hasSeenNotificationPrompt }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _shouldPromptNotificationOptIn = MutableStateFlow(false)
    val shouldPromptNotificationOptIn: StateFlow<Boolean> = _shouldPromptNotificationOptIn.asStateFlow()

    private val _isSyncingOfflineQueue = MutableStateFlow(false)
    val isSyncingOfflineQueue: StateFlow<Boolean> = _isSyncingOfflineQueue.asStateFlow()

    private val _lastOfflineSyncMessage = MutableStateFlow<String?>(null)
    val lastOfflineSyncMessage: StateFlow<String?> = _lastOfflineSyncMessage.asStateFlow()

    val firebaseConnectionStatus: StateFlow<FirebaseConnectionStatus> =
        FirebaseCoreManager.connectionStatus

    val firebaseUserProfile: StateFlow<FirebaseUserProfile?> =
        FirebaseCoreManager.currentUserProfile

    private val _celebrationMessage = MutableStateFlow<String?>(null)
    val celebrationMessage: StateFlow<String?> = _celebrationMessage.asStateFlow()

    private val _reopenedRoomPasscode = MutableStateFlow<String?>(null)
    val reopenedRoomPasscode: StateFlow<String?> = _reopenedRoomPasscode.asStateFlow()

    val distributions: StateFlow<List<DistributionEntity>> = repository.allDistributions
        .onEach { _isLoadingDistributions.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val participantsByDistribution: StateFlow<Map<Long, List<ParticipantEntity>>> =
        repository.allParticipants
            .map { list -> list.groupBy { it.distributionId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    val pendingOfflineSyncCount: StateFlow<Int> = combine(
        distributions,
        participantsByDistribution
    ) { distList, partsMap ->
        distList.count { dist ->
            dist.isOnline && (dist.hasPendingSync || partsMap[dist.id].orEmpty().any { it.hasPendingSync })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _selectedDistributionId = MutableStateFlow<Long?>(null)
    val selectedDistributionId: StateFlow<Long?> = _selectedDistributionId.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)

    init {
        FirebaseCoreManager.initialize(appContext)
        viewModelScope.launch {
            val currentSettings = preferencesDataStore.userSettingsFlow.first()
            if (currentSettings.userProfileName.isNotBlank()) {
                FirebaseCoreManager.syncUserProfile(currentSettings.userProfileName)
            }
            checkAndTriggerMonthlyRemindersIfNeeded()
        }
        viewModelScope.launch {
            firebaseConnectionStatus.collect { status ->
                if (status == FirebaseConnectionStatus.READY) {
                    repository.flushPendingOfflineChanges()
                }
            }
        }
        viewModelScope.launch {
            _selectedDistributionId
                .flatMapLatest { id ->
                    if (id == null) flowOf(null) else repository.getDistribution(id)
                }
                .flatMapLatest { dist ->
                    val code = dist?.roomPasscode
                    if (dist != null && dist.isOnline && !code.isNullOrBlank()) {
                        FirebaseCoreManager.observeOnlineRoom(code).map { snapshot -> dist.id to snapshot }
                    } else {
                        flowOf(null)
                    }
                }
                .collect { pair ->
                    if (pair != null) {
                        repository.applyFirestoreSnapshotToLocalRoom(pair.first, pair.second)
                    }
                }
        }
    }

    private suspend fun checkAndTriggerMonthlyRemindersIfNeeded() {
        try {
            val settings = preferencesDataStore.userSettingsFlow.first()
            if (!settings.notificationsEnabled) return
            if (!KhatmNotificationHelper.hasNotificationPermission(appContext)) return
            val allDists = repository.allDistributions.first()
            val allParts = repository.allParticipants.first().groupBy { it.distributionId }

            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val stampKey = "$year-$month-NEW-MONTH"

            if (settings.lastMonthlyReminderStamp == stampKey) return

            // For Local Monthly Qur'an Khatms: notify whenever a new month is assigned so user can read & share
            val localMonthlyKhatms = allDists.filter {
                !it.isOnline && it.type == DistributionType.KHATM && !it.isClosedForever
            }
            var sentAny = false
            for (khatm in localMonthlyKhatms) {
                val parts = allParts[khatm.id].orEmpty()
                if (parts.isEmpty()) continue
                val monthInfo = HijriDateUtils.getMonthInfo(year, month)
                KhatmNotificationHelper.notifyLocalNewMonthAssigned(
                    context = appContext,
                    roomId = khatm.id,
                    roomName = khatm.name,
                    monthLabel = monthInfo.gregorianFullText
                )
                KhatmNotificationHelper.scheduleLocalMonthlyAssignmentReminder(
                    context = appContext,
                    distributionId = khatm.id,
                    distributionName = khatm.name
                )
                sentAny = true
            }
            if (sentAny) {
                preferencesDataStore.setLastMonthlyReminderStamp(stampKey)
            }
        } catch (_: Exception) {
        }
    }

    fun setUserProfileName(name: String) {
        viewModelScope.launch {
            preferencesDataStore.setUserProfileName(name)
            if (name.isNotBlank()) {
                FirebaseCoreManager.syncUserProfile(name)
            }
        }
    }

    fun deleteCloudProfileAndResetId(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            preferencesDataStore.setUserProfileName("")
            val deleted = FirebaseCoreManager.deleteCloudProfileAndResetAuth()
            onComplete(deleted)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setNotificationsEnabled(enabled)
        }
    }

    fun triggerNotificationOptInIfNeeded() {
        viewModelScope.launch {
            val settings = preferencesDataStore.userSettingsFlow.first()
            val hasOsPermission = KhatmNotificationHelper.hasNotificationPermission(appContext)
            if (!settings.hasSeenNotificationPrompt || !hasOsPermission) {
                _shouldPromptNotificationOptIn.value = true
            }
        }
    }

    fun completeNotificationOptInPrompt(enabled: Boolean) {
        _shouldPromptNotificationOptIn.value = false
        viewModelScope.launch {
            preferencesDataStore.setHasSeenNotificationPrompt(true)
            preferencesDataStore.setNotificationsEnabled(enabled)
        }
    }

    fun dismissNotificationOptInPrompt() {
        _shouldPromptNotificationOptIn.value = false
        viewModelScope.launch {
            preferencesDataStore.setHasSeenNotificationPrompt(true)
        }
    }

    fun syncOfflineChangesNow() {
        if (_isSyncingOfflineQueue.value) return
        viewModelScope.launch {
            _isSyncingOfflineQueue.value = true
            try {
                FirebaseCoreManager.retryInitialize(appContext)
                val syncedCount = repository.flushPendingOfflineChanges()
                val activeId = _selectedDistributionId.value
                if (activeId != null) {
                    repository.syncDistributionWithFirestore(activeId)
                }
                _lastOfflineSyncMessage.value = if (syncedCount > 0) {
                    "Offline sync complete ($syncedCount room${if (syncedCount == 1) "" else "s"} updated)"
                } else {
                    "All rooms are saved locally & in sync"
                }
            } finally {
                _isSyncingOfflineQueue.value = false
            }
        }
    }

    fun clearOfflineSyncMessage() {
        _lastOfflineSyncMessage.value = null
    }

    fun dismissCelebrationMessage() {
        _celebrationMessage.value = null
    }

    fun dismissReopenedRoomPasscode() {
        _reopenedRoomPasscode.value = null
    }

    fun selectDistribution(id: Long?) {
        _selectedDistributionId.value = id
        if (id != null) {
            _selectedYear.value = Calendar.getInstance().get(Calendar.YEAR)
            _selectedMonth.value = Calendar.getInstance().get(Calendar.MONTH) + 1
        }
    }

    fun navigateMonth(delta: Int) {
        var newMonth = _selectedMonth.value + delta
        var newYear = _selectedYear.value

        while (newMonth > 12) {
            newMonth -= 12
            newYear += 1
        }
        while (newMonth < 1) {
            newMonth += 12
            newYear -= 1
        }

        _selectedMonth.value = newMonth
        _selectedYear.value = newYear
    }

    fun resetToCurrentMonth() {
        _selectedYear.value = Calendar.getInstance().get(Calendar.YEAR)
        _selectedMonth.value = Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    fun computeAssignmentsForDistribution(
        distribution: DistributionEntity,
        participants: List<ParticipantEntity>,
        targetYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        targetMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    ): List<ParaAssignment> {
        if (distribution.type != DistributionType.KHATM || participants.isEmpty()) {
            return emptyList()
        }
        val allocs = participants.map {
            ParticipantAllocation(
                id = it.id,
                name = it.name,
                count = it.allocatedCount,
                displayOrder = it.displayOrder
            )
        }
        return RotationEngine.calculateAssignments(
            participants = allocs,
            startYear = distribution.startYear,
            startMonth = distribution.startMonth,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
    }

    fun generateShareSummaryForDistribution(
        distribution: DistributionEntity,
        participants: List<ParticipantEntity>,
        targetYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        targetMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    ): String {
        val assignments = computeAssignmentsForDistribution(
            distribution = distribution,
            participants = participants,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
        return formatDistributionShareSummary(
            distribution = distribution,
            participants = participants,
            assignments = assignments,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeDistributionState: StateFlow<ActiveDistributionUiState> = _selectedDistributionId
        .flatMapLatest { id ->
            if (id == null) flowOf(ActiveDistributionUiState())
            else {
                combine(
                    repository.getDistribution(id),
                    repository.getParticipants(id),
                    _selectedYear,
                    _selectedMonth
                ) { dist, participants, year, month ->
                    if (dist == null) {
                        ActiveDistributionUiState(null, participants, year, month)
                    } else if (dist.type == DistributionType.AMAL || dist.type == DistributionType.ONE_TIME_KHATM) {
                        val sortedParticipants = if (dist.type == DistributionType.ONE_TIME_KHATM) {
                            participants.sortedBy { it.specificParaNumber ?: it.displayOrder }
                        } else {
                            participants.sortedWith(
                                compareBy<ParticipantEntity> { it.displayOrder }
                                    .thenBy { it.name.lowercase() }
                            )
                        }
                        ActiveDistributionUiState(
                            distribution = dist,
                            participants = sortedParticipants,
                            selectedYear = year,
                            selectedMonth = month,
                            assignments = emptyList()
                        )
                    } else {
                        val allocs = participants.map {
                            ParticipantAllocation(
                                id = it.id,
                                name = it.name,
                                count = it.allocatedCount,
                                displayOrder = it.displayOrder
                            )
                        }
                        val assignments = if (allocs.isNotEmpty()) {
                            RotationEngine.calculateAssignments(
                                participants = allocs,
                                startYear = dist.startYear,
                                startMonth = dist.startMonth,
                                targetYear = year,
                                targetMonth = month
                            )
                        } else emptyList()

                        ActiveDistributionUiState(
                            distribution = dist,
                            participants = participants,
                            selectedYear = year,
                            selectedMonth = month,
                            assignments = assignments
                        )
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActiveDistributionUiState()
        )

    fun saveDistribution(
        name: String,
        type: DistributionType = DistributionType.KHATM,
        roomMode: RoomMode = RoomMode.LOCAL,
        totalTarget: Int = 30,
        allocations: List<ParticipantAllocation>,
        existingId: Long?,
        onSaved: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val newId = repository.saveDistribution(
                name = name,
                type = type,
                roomMode = roomMode,
                totalTarget = totalTarget,
                allocations = allocations,
                existingId = existingId
            )
            // For Local Monthly Khatm rooms, automatically schedule the 1st-of-month rotation reminder if notifications are enabled
            if (roomMode == RoomMode.LOCAL && type == DistributionType.KHATM && notificationsEnabled.value) {
                KhatmNotificationHelper.scheduleLocalMonthlyAssignmentReminder(
                    context = appContext,
                    distributionId = newId,
                    distributionName = name.trim()
                )
            }
            selectDistribution(newId)
            if (existingId == null || existingId == 0L) {
                triggerNotificationOptInIfNeeded()
                if (roomMode == RoomMode.ONLINE) {
                    ServerSupportAdManager.triggerPromptForOnlineRoom(name.trim(), isCreation = true)
                }
            }
            onSaved?.invoke(newId)
        }
    }

    fun claimOneTimePara(
        distributionId: Long,
        paraNumber: Int,
        memberName: String,
        isGuest: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val profile = userProfileName.value.ifBlank { memberName }
            if (!isGuest && userProfileName.value.isBlank() && memberName.isNotBlank()) {
                preferencesDataStore.setUserProfileName(memberName.trim())
            }
            val success = repository.claimOneTimePara(
                distributionId = distributionId,
                paraNumber = paraNumber,
                memberName = memberName,
                isGuest = isGuest,
                addedByProfileName = profile
            )
            onResult(success)
        }
    }

    fun claimMultipleOneTimeParas(
        distributionId: Long,
        paraNumbers: List<Int>,
        memberName: String,
        isGuest: Boolean,
        onResult: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val profile = userProfileName.value.ifBlank { memberName }
            if (!isGuest && userProfileName.value.isBlank() && memberName.isNotBlank()) {
                preferencesDataStore.setUserProfileName(memberName.trim())
            }
            val claimed = repository.claimMultipleOneTimeParas(
                distributionId = distributionId,
                paraNumbers = paraNumbers,
                memberName = memberName,
                isGuest = isGuest,
                addedByProfileName = profile
            )
            onResult(claimed)
        }
    }

    fun joinAndClaimAllocation(
        distributionId: Long,
        memberName: String,
        requestedCount: Int,
        isGuest: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val profile = userProfileName.value.ifBlank { memberName }
            if (!isGuest && userProfileName.value.isBlank() && memberName.isNotBlank()) {
                preferencesDataStore.setUserProfileName(memberName.trim())
            }
            val success = repository.joinAndClaimAllocation(
                distributionId = distributionId,
                memberName = memberName,
                requestedCount = requestedCount,
                isGuest = isGuest,
                addedByProfileName = profile
            )
            onResult(success)
        }
    }

    fun removeParticipantClaim(participantId: Long) {
        viewModelScope.launch {
            repository.removeParticipantClaim(participantId)
        }
    }

    fun toggleParticipantPin(participantId: Long) {
        viewModelScope.launch {
            repository.toggleParticipantPin(participantId)
        }
    }

    fun updateAmalCompletedCount(
        distributionId: Long,
        participantId: Long,
        newCount: Int
    ) {
        viewModelScope.launch {
            val dist = activeDistributionState.value.distribution ?: return@launch
            val result = repository.updateAmalCompletedCount(
                distributionId = distributionId,
                participantId = participantId,
                newCount = newCount
            ) ?: return@launch

            if (result.justCompletedFullDistribution) {
                val celebrationText =
                    "MashaAllah! Round ${dist.roundNumber} of \"${dist.name}\" (${dist.totalTarget} Recitations) is 100% complete! You can reopen this room anytime for a new round."
                _celebrationMessage.value = celebrationText
            }
        }
    }

    fun toggleParticipantCompletion(
        distributionId: Long,
        participantId: Long,
        assignedParaLabel: String? = null
    ) {
        viewModelScope.launch {
            val dist = activeDistributionState.value.distribution ?: return@launch
            val year = _selectedYear.value
            val month = _selectedMonth.value

            val result = repository.toggleParticipantCompletion(
                distributionId = distributionId,
                participantId = participantId,
                targetYear = year,
                targetMonth = month,
                assignedParaLabel = assignedParaLabel
            ) ?: return@launch

            if (result.justCompletedFullDistribution) {
                val celebrationText = when (dist.type) {
                    DistributionType.ONE_TIME_KHATM ->
                        "MashaAllah! All 30 Paras of \"${dist.name}\" have been completed! Detailed claim records have been cleaned up to save space and this room is now closed forever."
                    DistributionType.AMAL ->
                        "MashaAllah! Round ${dist.roundNumber} of \"${dist.name}\" (${dist.totalTarget} Recitations) is 100% complete! You can reopen this room anytime for a new round."
                    DistributionType.KHATM ->
                        "MashaAllah! All 30 Paras of \"${dist.name}\" for ${activeDistributionState.value.dateInfo.gregorianFullText} are 100% completed!"
                }
                _celebrationMessage.value = celebrationText
            }
        }
    }

    fun reopenAmalRoom(
        distributionId: Long,
        keepMembersAndResetTicks: Boolean
    ) {
        viewModelScope.launch {
            val newPasscode = repository.reopenAmalRoom(
                distributionId = distributionId,
                keepMembersAndResetTicks = keepMembersAndResetTicks
            )
            if (newPasscode != null) {
                _reopenedRoomPasscode.value = newPasscode
            }
        }
    }

    fun joinRoomByPasscode(
        rawInput: String,
        customRoomTitle: String? = null,
        userNameToSave: String? = null,
        onResult: (Long?) -> Unit
    ) {
        viewModelScope.launch {
            if (!userNameToSave.isNullOrBlank()) {
                preferencesDataStore.setUserProfileName(userNameToSave.trim())
            }
            val joinedId = repository.joinRoomByPasscodeOrInvite(
                rawInput = rawInput,
                customRoomTitle = customRoomTitle
            )
            if (joinedId != null) {
                selectDistribution(joinedId)
                triggerNotificationOptInIfNeeded()
                ServerSupportAdManager.triggerPromptForOnlineRoom(customRoomTitle ?: "Online Room", isCreation = false)
            }
            onResult(joinedId)
        }
    }

    fun deleteDistribution(id: Long) {
        viewModelScope.launch {
            KhatmNotificationHelper.cancelReminder(appContext, id)
            repository.deleteDistribution(id)
            if (_selectedDistributionId.value == id) {
                _selectedDistributionId.value = null
            }
        }
    }

    fun getScheduledDueReminderLabel(distributionId: Long): String? {
        return KhatmNotificationHelper.getScheduledReminderLabel(appContext, distributionId)
    }

    fun buildOnlineUserRemainingSummary(
        dist: DistributionEntity,
        participants: List<ParticipantEntity>,
        assignments: List<ParaAssignment>,
        year: Int,
        month: Int,
        profileName: String
    ): String {
        val cleanProfile = profileName.trim().lowercase()
        val pinnedIds = participants.filter { it.isPinned }.map { it.id }.toSet()

        fun isUserSlot(p: ParticipantEntity): Boolean {
            if (p.id in pinnedIds) return true
            if (cleanProfile.isBlank()) return false
            return p.name.trim().lowercase() == cleanProfile ||
                p.addedByProfileName.trim().lowercase() == cleanProfile
        }

        return when (dist.type) {
            DistributionType.ONE_TIME_KHATM -> {
                val userRemainingParas = participants
                    .filter { !it.isCompleted && isUserSlot(it) }
                    .mapNotNull { it.specificParaNumber }
                    .sorted()
                val fallbackRemaining = if (userRemainingParas.isEmpty()) {
                    participants.filter { !it.isCompleted }.mapNotNull { it.specificParaNumber }.sorted()
                } else {
                    userRemainingParas
                }
                if (fallbackRemaining.isEmpty()) {
                    "All claimed Paras completed"
                } else {
                    "Para ${fallbackRemaining.joinToString(", ")}"
                }
            }

            DistributionType.AMAL -> {
                val userSlots = participants.filter { !it.isCompleted && isUserSlot(it) }
                val targetSlots = userSlots.ifEmpty { participants.filter { !it.isCompleted } }
                val remainingUnits = targetSlots.sumOf { (it.allocatedCount - it.completedCount).coerceAtLeast(0) }
                if (remainingUnits <= 0) {
                    "All pledged recitations completed"
                } else {
                    "$remainingUnits remaining recitation${if (remainingUnits == 1) "" else "s"}"
                }
            }

            DistributionType.KHATM -> {
                val byId = participants.associateBy { it.id }
                val userRemainingAssignments = assignments.filter { a ->
                    val p = byId[a.participantId]
                    p != null && !p.isCompletedForMonth(year, month) && isUserSlot(p)
                }
                val targetAssignments = userRemainingAssignments.ifEmpty {
                    assignments.filter { a ->
                        val p = byId[a.participantId]
                        p != null && !p.isCompletedForMonth(year, month)
                    }
                }
                val paras = targetAssignments.flatMap { it.assignedParas }.distinct().sorted()
                if (paras.isEmpty()) {
                    "All assigned Paras completed for this month"
                } else {
                    "Para ${paras.joinToString(", ")}"
                }
            }
        }
    }

    fun scheduleOnlineReminderForActiveRoom(preset: KhatmNotificationHelper.ReminderPreset): String {
        val state = activeDistributionState.value
        val dist = state.distribution ?: return "No active room selected"
        val profile = userProfileName.value
        val remainingSummary = buildOnlineUserRemainingSummary(
            dist = dist,
            participants = state.participants,
            assignments = state.assignments,
            year = state.selectedYear,
            month = state.selectedMonth,
            profileName = profile
        )
        val formattedTime = KhatmNotificationHelper.scheduleOnlineRemainingReminder(
            context = appContext,
            distributionId = dist.id,
            distributionName = dist.name,
            memberName = profile.ifBlank { "Reader" },
            remainingAssignmentSummary = remainingSummary,
            preset = preset
        )
        return "Reminder set for $formattedTime ($remainingSummary)"
    }

    fun enableLocalMonthlyNotificationForActiveRoom(): String {
        val state = activeDistributionState.value
        val dist = state.distribution ?: return "No active schedule selected"
        val formattedTime = KhatmNotificationHelper.scheduleLocalMonthlyAssignmentReminder(
            context = appContext,
            distributionId = dist.id,
            distributionName = dist.name
        )
        return "Monthly Qur'an rotation alert set for $formattedTime"
    }

    fun cancelDueReminderForActiveRoom() {
        val dist = activeDistributionState.value.distribution ?: return
        KhatmNotificationHelper.cancelReminder(appContext, dist.id)
    }

    companion object {
        /**
         * Formats the distribution in the clean Name-first, ascending-Para style:
         *
         * Abbas:        1, 2
         * Khadija g.:   3
         * Madiha:       4, 5
         * ...
         * Qurat:        28, 29, 30
         */
        fun formatDistributionShareSummary(
            distribution: DistributionEntity,
            participants: List<ParticipantEntity>,
            assignments: List<ParaAssignment>,
            targetYear: Int,
            targetMonth: Int
        ): String {
            val sb = StringBuilder()

            if (distribution.type == DistributionType.ONE_TIME_KHATM) {
                sb.append("${distribution.name}\n")
                if (distribution.isOnline && !distribution.roomPasscode.isNullOrBlank()) {
                    sb.append("Room Passcode: ${distribution.roomPasscode}\n")
                }
                if (distribution.isClosedForever) {
                    sb.append("${distribution.closedSummaryText ?: "30/30 Paras Completed"}\n")
                    return sb.toString().trimEnd()
                }
                sb.append("\n")

                if (participants.isEmpty()) {
                    sb.append("All 30 Paras are currently open to claim.")
                } else {
                    val sortedClaims = participants.sortedBy { it.specificParaNumber ?: it.displayOrder }
                    // Group by participant name while preserving ascending order of their lowest Para
                    val groupedByMember = linkedMapOf<String, MutableList<ParticipantEntity>>()
                    for (claim in sortedClaims) {
                        val key = "${claim.name.trim()}|${claim.isGuestEntry}"
                        groupedByMember.getOrPut(key) { mutableListOf() }.add(claim)
                    }
                    val maxNameLen = groupedByMember.values.maxOfOrNull { list ->
                        val first = list.first()
                        val guestTag = if (first.isGuestEntry) " (Guest)" else ""
                        (first.name.trim() + guestTag).length
                    } ?: 10
                    val targetColWidth = (maxNameLen + 4).coerceAtLeast(14)

                    groupedByMember.values.forEach { memberClaims ->
                        val first = memberClaims.first()
                        val guestTag = if (first.isGuestEntry) " (Guest)" else ""
                        val parasCsv = memberClaims
                            .map { it.specificParaNumber ?: it.displayOrder }
                            .sorted()
                            .joinToString(", ")
                        val allDone = distribution.isOnline && memberClaims.all { it.isCompleted }
                        val doneMark = if (allDone) " ✓" else ""
                        val rawLabel = "${first.name.trim()}$guestTag:"
                        val paddedLabel = rawLabel.padEnd(targetColWidth, ' ')
                        sb.append("$paddedLabel$parasCsv$doneMark\n")
                    }

                    val takenParas = sortedClaims.map { it.specificParaNumber ?: it.displayOrder }.toSet()
                    val availableParas = (1..30).filter { it !in takenParas }
                    if (availableParas.isNotEmpty()) {
                        sb.append("\nAvailable Paras (${availableParas.size}): ${availableParas.joinToString(", ")}\n")
                    }
                }
                return sb.toString().trimEnd()
            }

            if (distribution.type == DistributionType.AMAL) {
                val roundSuffix = if (distribution.roundNumber > 1) " • Round ${distribution.roundNumber}" else ""
                sb.append("${distribution.name} (Target: ${distribution.totalTarget}$roundSuffix)\n")
                if (distribution.isOnline && !distribution.roomPasscode.isNullOrBlank()) {
                    sb.append("Room Passcode: ${distribution.roomPasscode}\n")
                }
                sb.append("\n")

                if (participants.isEmpty()) {
                    sb.append("Open for members to join and pledge recitations.")
                } else {
                    val sortedMembers = participants.sortedWith(
                        compareBy<ParticipantEntity> { it.displayOrder }
                            .thenBy { it.name.lowercase() }
                    )
                    val maxNameLen = sortedMembers.maxOfOrNull { p ->
                        val guestTag = if (p.isGuestEntry) " (Guest)" else ""
                        (p.name.trim() + guestTag).length
                    } ?: 10
                    val targetColWidth = (maxNameLen + 4).coerceAtLeast(14)

                    sortedMembers.forEach { p ->
                        val guestTag = if (p.isGuestEntry) " (Guest)" else ""
                        val doneMark = if (!distribution.isOnline) {
                            ""
                        } else if (p.isCompleted) {
                            " ✓"
                        } else if (p.completedCount > 0) {
                            " (${p.completedCount}/${p.allocatedCount})"
                        } else {
                            ""
                        }
                        val rawLabel = "${p.name.trim()}$guestTag:"
                        val paddedLabel = rawLabel.padEnd(targetColWidth, ' ')
                        sb.append("$paddedLabel${p.allocatedCount}$doneMark\n")
                    }
                }
                return sb.toString().trimEnd()
            }

            // Monthly Rotating KHATM — simple header + Name-first ascending Para table
            val info = HijriDateUtils.getMonthInfo(targetYear, targetMonth)
            sb.append("${distribution.name} — ${info.gregorianFullText}\n")
            if (distribution.isOnline && !distribution.roomPasscode.isNullOrBlank()) {
                sb.append("Room Passcode: ${distribution.roomPasscode}\n")
            }
            sb.append("\n")

            if (assignments.isEmpty()) {
                sb.append("Open for members to join and claim monthly Paras.")
                return sb.toString().trimEnd()
            }

            val completionByParticipantId = participants.associate {
                it.id to it.isCompletedForMonth(targetYear, targetMonth)
            }

            val sortedByParasAscending = assignments.sortedWith(RotationEngine.paraFirstComparator)

            val maxNameLen = sortedByParasAscending.maxOfOrNull { it.participantName.trim().length } ?: 10
            val targetColWidth = (maxNameLen + 4).coerceAtLeast(14)

            sortedByParasAscending.forEach { assignment ->
                val isDone = distribution.isOnline && completionByParticipantId[assignment.participantId] == true
                val doneMark = if (isDone) " ✓" else ""
                sb.append(
                    assignment.formatAlignedLine(
                        targetNameColumnWidth = targetColWidth,
                        extraSuffix = doneMark
                    )
                )
                sb.append("\n")
            }

            return sb.toString().trimEnd()
        }
    }
}
