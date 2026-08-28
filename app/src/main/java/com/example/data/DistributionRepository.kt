package com.example.data

import com.example.data.db.DistributionDao
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class DistributionRepository(private val dao: DistributionDao) {

    val allDistributions: Flow<List<DistributionEntity>> = dao.getAllDistributions()

    fun getDistribution(id: Long): Flow<DistributionEntity?> = dao.getDistributionByIdFlow(id)

    fun getParticipants(distributionId: Long): Flow<List<ParticipantEntity>> =
        dao.getParticipantsForDistribution(distributionId)

    suspend fun saveDistribution(
        name: String,
        type: DistributionType = DistributionType.KHATM,
        totalTarget: Int = 30,
        allocations: List<ParticipantAllocation>,
        existingId: Long? = null
    ): Long {
        if (type == DistributionType.KHATM) {
            require(RotationEngine.isValid30Allocation(allocations)) {
                "Total allocated Paras must equal exactly 30 Paras. Current count is ${allocations.sumOf { it.count }}."
            }
        } else {
            require(totalTarget > 0) {
                "Total target recitations must be greater than 0."
            }
            require(allocations.isNotEmpty()) {
                "Please add at least one participant."
            }
            val totalAllocated = allocations.sumOf { it.count }
            require(totalAllocated == totalTarget) {
                "Total allocated recitations must equal exactly $totalTarget. Current sum is $totalAllocated."
            }
            require(allocations.all { it.count > 0 && it.name.isNotBlank() }) {
                "All participants must have a non-empty name and at least 1 recitation."
            }
        }

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1..12

        if (existingId == null || existingId == 0L) {
            val entity = DistributionEntity(
                name = name.trim(),
                type = type,
                totalTarget = if (type == DistributionType.KHATM) 30 else totalTarget,
                startYear = currentYear,
                startMonth = currentMonth
            )
            val participants = allocations.mapIndexed { index, alloc ->
                ParticipantEntity(
                    distributionId = 0,
                    name = alloc.name.trim(),
                    allocatedCount = alloc.count,
                    displayOrder = index + 1
                )
            }
            return dao.insertFullDistribution(entity, participants)
        } else {
            val existing = dao.getDistributionById(existingId)
            val entity = DistributionEntity(
                id = existingId,
                name = name.trim(),
                type = type,
                totalTarget = if (type == DistributionType.KHATM) 30 else totalTarget,
                createdTimestamp = existing?.createdTimestamp ?: System.currentTimeMillis(),
                startYear = existing?.startYear ?: currentYear,
                startMonth = existing?.startMonth ?: currentMonth
            )
            val participants = allocations.mapIndexed { index, alloc ->
                ParticipantEntity(
                    distributionId = existingId,
                    name = alloc.name.trim(),
                    allocatedCount = alloc.count,
                    displayOrder = index + 1
                )
            }
            dao.updateFullDistribution(entity, participants)
            return existingId
        }
    }

    suspend fun deleteDistribution(id: Long) {
        dao.deleteDistribution(id)
    }
}


