package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DistributionRepository
import com.example.data.db.AppDatabase
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.domain.ParaAssignment
import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import com.example.util.HijriDateUtils
import com.example.util.MonthDateInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

class DistributionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DistributionRepository =
        DistributionRepository(AppDatabase.getDatabase(application).distributionDao())

    private val _isLoadingDistributions = MutableStateFlow(true)
    val isLoadingDistributions: StateFlow<Boolean> = _isLoadingDistributions.asStateFlow()

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

    private val _selectedDistributionId = MutableStateFlow<Long?>(null)
    val selectedDistributionId: StateFlow<Long?> = _selectedDistributionId.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)

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
        if (distribution.type == DistributionType.AMAL || participants.isEmpty()) {
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
                    if (dist == null || participants.isEmpty()) {
                        ActiveDistributionUiState(dist, participants, year, month)
                    } else if (dist.type == DistributionType.AMAL) {
                        ActiveDistributionUiState(
                            distribution = dist,
                            participants = participants.sortedWith(
                                compareBy<ParticipantEntity> { it.displayOrder }
                                    .thenBy { it.name.lowercase() }
                            ),
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
                        val assignments = RotationEngine.calculateAssignments(
                            participants = allocs,
                            startYear = dist.startYear,
                            startMonth = dist.startMonth,
                            targetYear = year,
                            targetMonth = month
                        )
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
        totalTarget: Int = 30,
        allocations: List<ParticipantAllocation>,
        existingId: Long?
    ) {
        viewModelScope.launch {
            val newId = repository.saveDistribution(
                name = name,
                type = type,
                totalTarget = totalTarget,
                allocations = allocations,
                existingId = existingId
            )
            selectDistribution(newId)
        }
    }

    fun deleteDistribution(id: Long) {
        viewModelScope.launch {
            repository.deleteDistribution(id)
            if (_selectedDistributionId.value == id) {
                _selectedDistributionId.value = null
            }
        }
    }

    companion object {
        /**
         * Formats the distribution progress and monthly schedule summary for sharing via messaging apps.
         *
         * Strictly orders all Khatm allocations from Para 1 to Para 30, then by participant name,
         * with Para numbers first on each line:
         * 1, 2: XYZ
         * 3, 4, 5: ABC
         */
        fun formatDistributionShareSummary(
            distribution: DistributionEntity,
            participants: List<ParticipantEntity>,
            assignments: List<ParaAssignment>,
            targetYear: Int,
            targetMonth: Int
        ): String {
            if (participants.isEmpty()) return ""

            if (distribution.type == DistributionType.AMAL) {
                val allocatedSum = participants.sumOf { it.allocatedCount }
                val sb = StringBuilder()
                sb.append("${distribution.name}\n")
                sb.append("Progress: $allocatedSum/${distribution.totalTarget} Recitations (${participants.size} Members)\n\n")
                participants
                    .sortedWith(
                        compareBy<ParticipantEntity> { it.displayOrder }
                            .thenBy { it.name.lowercase() }
                    )
                    .forEach { p ->
                        sb.append("${p.allocatedCount}: ${p.name}\n")
                    }
                return sb.toString().trimEnd()
            }

            if (assignments.isEmpty()) return ""
            val info = HijriDateUtils.getMonthInfo(targetYear, targetMonth)
            val cycleNumber = ((targetYear - distribution.startYear) * 12 + (targetMonth - distribution.startMonth)) + 1
            val totalAssignedParas = assignments.sumOf { it.allocatedCount }

            val sortedByParasFirst = assignments.sortedWith(
                compareBy<ParaAssignment> { it.startPara }
                    .thenBy { it.endPara }
                    .thenBy { it.participantName.lowercase() }
            )

            val sb = StringBuilder()
            sb.append("${distribution.name}\n")
            sb.append("${info.gregorianFullText} • ${info.hijriFullText}\n")
            sb.append("Cycle Month $cycleNumber • Progress: $totalAssignedParas/30 Paras (${sortedByParasFirst.size} Members)\n\n")

            sortedByParasFirst.forEach { assignment ->
                sb.append("${assignment.commaSeparatedParas}: ${assignment.participantName}\n")
            }

            return sb.toString().trimEnd()
        }
    }
}
