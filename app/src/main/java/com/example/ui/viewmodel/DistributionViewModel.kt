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

    val shareText: String
        get() {
            if (distribution == null || participants.isEmpty()) return ""

            if (isAmal) {
                val sb = StringBuilder()
                sb.append("${distribution.name} — ${distribution.totalTarget} Recitations\n\n")
                sb.append("🤲 Amal Distribution\n\n")
                participants.sortedBy { it.displayOrder }.forEach { p ->
                    sb.append("${p.name} — ${p.allocatedCount} recitations\n")
                }
                sb.append("\nMay Allah accept everyone's recitation. 🤲\n")
                return sb.toString().trimEnd()
            }

            // Khatm Share format: Organized based on the Paras (1..30)
            if (assignments.isEmpty()) return ""
            val info = dateInfo
            val sb = StringBuilder()
            sb.append("✨ ${distribution.name}\n")
            sb.append("📅 ${info.gregorianFullText} • ${info.hijriFullText}\n")
            sb.append("📖 Qur'an Monthly Para Distribution (30 Paras)\n")
            sb.append("─────────────────────────\n\n")

            // Organized based on the paras
            val sortedByParas = assignments.sortedWith(
                compareBy<ParaAssignment> { it.startPara }.thenBy { it.endPara }
            )

            sortedByParas.forEach { assignment ->
                sb.append("${assignment.participantName}: ${assignment.commaSeparatedParas}\n")
            }

            sb.append("\n─────────────────────────\n")
            sb.append("May Allah accept everyone's recitation. 🤲\n")
            sb.append("Organized via Quran Baanto")
            return sb.toString()
        }
}

class DistributionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DistributionRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DistributionRepository(database.distributionDao())
    }

    val distributions: StateFlow<List<DistributionEntity>> = repository.allDistributions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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
                        // Amal: No monthly rotation, fixed configured recitation distribution
                        ActiveDistributionUiState(
                            distribution = dist,
                            participants = participants.sortedBy { it.displayOrder },
                            selectedYear = year,
                            selectedMonth = month,
                            assignments = emptyList()
                        )
                    } else {
                        // Khatm: 30-Para fair rotation algorithm
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
}


