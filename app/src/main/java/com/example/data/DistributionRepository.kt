package com.example.data

import com.example.data.db.DistributionDao
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.data.db.RoomMode
import com.example.data.firebase.FirebaseCoreManager
import com.example.data.firebase.FirestoreRoomSnapshot
import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import java.util.Calendar

data class CompletionUpdateResult(
    val wasMarkedComplete: Boolean,
    val memberName: String,
    val itemLabel: String,
    val remainingPendingCount: Int,
    val remainingUnitLabel: String,
    val justCompletedFullDistribution: Boolean,
    val autoCleanedUpOneTimeKhatm: Boolean = false
)

class DistributionRepository(private val dao: DistributionDao) {

    val allDistributions: Flow<List<DistributionEntity>> = dao.getAllDistributions()

    val allParticipants: Flow<List<ParticipantEntity>> = dao.getAllParticipants()

    fun getDistribution(id: Long): Flow<DistributionEntity?> = dao.getDistributionByIdFlow(id)

    fun getParticipants(distributionId: Long): Flow<List<ParticipantEntity>> =
        dao.getParticipantsForDistribution(distributionId)

    private val secureRandom = SecureRandom()

    suspend fun generateUniquePasscode(type: DistributionType): String {
        val prefix = when (type) {
            DistributionType.KHATM -> "QBM"
            DistributionType.ONE_TIME_KHATM -> "QBO"
            DistributionType.AMAL -> "QBA"
        }
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        repeat(20) {
            val suffix = (1..8)
                .map { chars[secureRandom.nextInt(chars.length)] }
                .joinToString("")
            val candidate = "$prefix-$suffix"
            if (dao.getDistributionByPasscode(candidate) == null) {
                return candidate
            }
        }
        val fallback = (1..8)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
        return "$prefix-$fallback"
    }

    suspend fun saveDistribution(
        name: String,
        type: DistributionType = DistributionType.KHATM,
        roomMode: RoomMode = RoomMode.LOCAL,
        totalTarget: Int = 30,
        allocations: List<ParticipantAllocation>,
        existingId: Long? = null
    ): Long {
        val normalizedTarget = if (type == DistributionType.AMAL) totalTarget.coerceAtLeast(1) else 30

        // Strict validation for Local Rooms where all participants are named and assigned upfront at setup
        if (roomMode == RoomMode.LOCAL && (type == DistributionType.KHATM || type == DistributionType.ONE_TIME_KHATM)) {
            require(RotationEngine.isValid30Allocation(allocations)) {
                "Total allocated Paras must equal exactly 30 Paras. Current count is ${allocations.sumOf { it.count }}."
            }
        } else if (roomMode == RoomMode.LOCAL && type == DistributionType.AMAL) {
            require(normalizedTarget > 0) {
                "Total target recitations must be greater than 0."
            }
            require(allocations.isNotEmpty()) {
                "Please add at least one participant."
            }
            val totalAllocated = allocations.sumOf { it.count }
            require(totalAllocated == normalizedTarget) {
                "Total allocated recitations must equal exactly $normalizedTarget. Current sum is $totalAllocated."
            }
        }

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1..12

        if (existingId == null || existingId == 0L) {
            val passcode = if (roomMode == RoomMode.ONLINE) generateUniquePasscode(type) else null
            val entity = DistributionEntity(
                name = name.trim(),
                type = type,
                roomMode = roomMode,
                roomPasscode = passcode,
                totalTarget = normalizedTarget,
                startYear = currentYear,
                startMonth = currentMonth,
                roundNumber = 1
            )

            val validAllocations = allocations.filter { it.name.isNotBlank() && it.count > 0 }
            val participants = if (type == DistributionType.ONE_TIME_KHATM) {
                // In One-Time Khatm, each initial allocation claims sequential Paras 1..N
                var nextPara = 1
                val list = mutableListOf<ParticipantEntity>()
                for (alloc in validAllocations) {
                    repeat(alloc.count) {
                        if (nextPara <= 30) {
                            list.add(
                                ParticipantEntity(
                                    distributionId = 0,
                                    name = alloc.name.trim(),
                                    allocatedCount = 1,
                                    displayOrder = nextPara,
                                    specificParaNumber = nextPara
                                )
                            )
                            nextPara++
                        }
                    }
                }
                list
            } else {
                validAllocations.mapIndexed { index, alloc ->
                    ParticipantEntity(
                        distributionId = 0,
                        name = alloc.name.trim(),
                        allocatedCount = alloc.count,
                        displayOrder = index + 1
                    )
                }
            }
            val newId = dao.insertFullDistribution(entity, participants)
            syncDistributionWithFirestore(newId)
            return newId
        } else {
            val existing = dao.getDistributionById(existingId)
            val existingParticipants = dao.getParticipantsForDistributionList(existingId)
            val passcode = when {
                roomMode == RoomMode.ONLINE && !existing?.roomPasscode.isNullOrBlank() -> existing?.roomPasscode
                roomMode == RoomMode.ONLINE -> generateUniquePasscode(type)
                else -> null
            }

            val entity = DistributionEntity(
                id = existingId,
                name = name.trim(),
                type = type,
                roomMode = roomMode,
                roomPasscode = passcode,
                totalTarget = normalizedTarget,
                createdTimestamp = existing?.createdTimestamp ?: System.currentTimeMillis(),
                startYear = existing?.startYear ?: currentYear,
                startMonth = existing?.startMonth ?: currentMonth,
                roundNumber = existing?.roundNumber ?: 1,
                isClosedForever = existing?.isClosedForever ?: false,
                closedSummaryText = existing?.closedSummaryText,
                completedTimestamp = existing?.completedTimestamp,
                hasPendingSync = existing?.hasPendingSync ?: false,
                lastModifiedTimestamp = System.currentTimeMillis(),
                lastSyncedTimestamp = existing?.lastSyncedTimestamp ?: 0L
            )

            if (type == DistributionType.ONE_TIME_KHATM && allocations.isEmpty()) {
                dao.updateDistribution(entity)
                syncDistributionWithFirestore(existingId)
                return existingId
            }

            val validAllocations = allocations.filter { it.name.isNotBlank() && it.count > 0 }
            val participants = if (type == DistributionType.ONE_TIME_KHATM && roomMode == RoomMode.LOCAL) {
                var nextPara = 1
                val list = mutableListOf<ParticipantEntity>()
                for (alloc in validAllocations) {
                    repeat(alloc.count) {
                        if (nextPara <= 30) {
                            val prev = existingParticipants.find { it.specificParaNumber == nextPara }
                            list.add(
                                ParticipantEntity(
                                    id = prev?.id ?: 0,
                                    distributionId = existingId,
                                    name = alloc.name.trim(),
                                    allocatedCount = 1,
                                    displayOrder = nextPara,
                                    specificParaNumber = nextPara,
                                    isCompleted = prev?.isCompleted ?: false,
                                    isPinned = prev?.isPinned ?: false
                                )
                            )
                            nextPara++
                        }
                    }
                }
                list
            } else {
                validAllocations.mapIndexed { index, alloc ->
                    val prev = existingParticipants.find { it.id == alloc.id }
                    ParticipantEntity(
                        id = if (alloc.id > 0) alloc.id else 0,
                        distributionId = existingId,
                        name = alloc.name.trim(),
                        allocatedCount = alloc.count,
                        displayOrder = index + 1,
                        specificParaNumber = prev?.specificParaNumber,
                        completedCount = (prev?.completedCount ?: 0).coerceAtMost(alloc.count),
                        isCompleted = prev?.isCompleted ?: false,
                        completedCycleMonthsCsv = prev?.completedCycleMonthsCsv ?: "",
                        isGuestEntry = prev?.isGuestEntry ?: false,
                        addedByProfileName = prev?.addedByProfileName ?: "",
                        isPinned = prev?.isPinned ?: false
                    )
                }
            }
            dao.updateFullDistribution(entity, participants)
            syncDistributionWithFirestore(existingId)
            return existingId
        }
    }

    suspend fun syncDistributionWithFirestore(distributionId: Long): Boolean {
        val dist = dao.getDistributionById(distributionId) ?: return false
        if (!dist.isOnline || dist.roomPasscode.isNullOrBlank()) return false
        val participants = dao.getParticipantsForDistributionList(distributionId)
        val ok = FirebaseCoreManager.syncOnlineRoomToFirestore(dist, participants)
        val now = System.currentTimeMillis()
        if (ok) {
            dao.updateDistribution(
                dist.copy(
                    hasPendingSync = false,
                    lastSyncedTimestamp = now
                )
            )
            participants.filter { it.hasPendingSync }.forEach { p ->
                dao.updateParticipant(p.copy(hasPendingSync = false, lastModifiedTimestamp = now))
            }
        } else {
            dao.updateDistribution(
                dist.copy(
                    hasPendingSync = true,
                    lastModifiedTimestamp = now
                )
            )
        }
        return ok
    }

    /**
     * Flushes all pending offline edits across all Online Rooms to Firestore,
     * and pulls fresh remote snapshots for clean rooms when online.
     */
    suspend fun flushPendingOfflineChanges(): Int {
        val pendingDistributions = dao.getPendingSyncDistributionsList()
        val allOnline = dao.getAllOnlineDistributionsList()
        var syncedCount = 0

        val pendingIds = pendingDistributions.map { it.id }.toSet()
        for (dist in allOnline) {
            val participants = dao.getParticipantsForDistributionList(dist.id)
            val hasPendingParticipant = participants.any { it.hasPendingSync }
            if (dist.id in pendingIds || hasPendingParticipant) {
                if (syncDistributionWithFirestore(dist.id)) {
                    syncedCount++
                }
            } else if (!dist.roomPasscode.isNullOrBlank()) {
                val remote = FirebaseCoreManager.fetchOnlineRoomByPasscode(dist.roomPasscode)
                if (remote != null) {
                    applyFirestoreSnapshotToLocalRoom(dist.id, remote)
                    syncedCount++
                }
            }
        }
        return syncedCount
    }

    suspend fun applyFirestoreSnapshotToLocalRoom(
        distributionId: Long,
        snapshot: FirestoreRoomSnapshot
    ) {
        val existing = dao.getDistributionById(distributionId) ?: return
        val existingParts = dao.getParticipantsForDistributionList(distributionId)

        // Offline Sync Safeguard: If this room has unsynced local changes, push them first!
        val unsyncedParts = existingParts.filter { it.hasPendingSync }
        if (existing.hasPendingSync || unsyncedParts.isNotEmpty()) {
            val pushed = FirebaseCoreManager.syncOnlineRoomToFirestore(existing, existingParts)
            if (pushed) {
                val now = System.currentTimeMillis()
                dao.updateDistribution(existing.copy(hasPendingSync = false, lastSyncedTimestamp = now))
                unsyncedParts.forEach { p ->
                    dao.updateParticipant(p.copy(hasPendingSync = false, lastModifiedTimestamp = now))
                }
            }
            return
        }

        val now = System.currentTimeMillis()
        val updatedEntity = existing.copy(
            name = snapshot.name,
            totalTarget = snapshot.totalTarget,
            roundNumber = snapshot.roundNumber,
            isClosedForever = snapshot.isClosedForever,
            closedSummaryText = snapshot.closedSummaryText ?: existing.closedSummaryText,
            hasPendingSync = false,
            lastSyncedTimestamp = now
        )
        if (snapshot.isClosedForever && snapshot.type == DistributionType.ONE_TIME_KHATM) {
            dao.deleteParticipantsByDistribution(distributionId)
            dao.updateDistribution(updatedEntity)
            return
        }
        val mappedParticipants = snapshot.participants
            .sortedBy { it.displayOrder }
            .map { dto ->
                val localMatch = existingParts.firstOrNull {
                    (dto.specificParaNumber != null && it.specificParaNumber == dto.specificParaNumber) ||
                        (it.displayOrder == dto.displayOrder && it.name.equals(dto.name, ignoreCase = true))
                }
                ParticipantEntity(
                    distributionId = distributionId,
                    name = dto.name,
                    allocatedCount = dto.allocatedCount,
                    completedCount = if (dto.isCompleted && dto.completedCount == 0) dto.allocatedCount else dto.completedCount.coerceIn(0, dto.allocatedCount),
                    displayOrder = dto.displayOrder,
                    specificParaNumber = dto.specificParaNumber,
                    isCompleted = dto.isCompleted,
                    completedCycleMonthsCsv = dto.completedMonthsCsv,
                    isGuestEntry = dto.isGuestEntry,
                    addedByProfileName = localMatch?.addedByProfileName ?: "",
                    isPinned = localMatch?.isPinned ?: false,
                    hasPendingSync = false,
                    lastModifiedTimestamp = now
                )
            }
        dao.updateFullDistribution(updatedEntity, mappedParticipants)
    }

    /**
     * First-come, first-served Para claim for One-Time Qur'an Khatm (Paras 1..30).
     * Returns true if claimed successfully, or false if that Para was already taken.
     */
    suspend fun claimOneTimePara(
        distributionId: Long,
        paraNumber: Int,
        memberName: String,
        isGuest: Boolean,
        addedByProfileName: String
    ): Boolean {
        if (paraNumber !in 1..30 || memberName.isBlank()) return false
        val dist = dao.getDistributionById(distributionId) ?: return false
        if (dist.isClosedForever) return false

        val existing = dao.getParticipantsForDistributionList(distributionId)
        val alreadyTaken = existing.any {
            (it.specificParaNumber ?: it.displayOrder) == paraNumber
        }
        if (alreadyTaken) return false

        // If Online Room, execute atomic Firestore Transaction first to guarantee first-come, first-served
        if (dist.isOnline && !dist.roomPasscode.isNullOrBlank()) {
            val cloudSuccess = FirebaseCoreManager.claimOneTimeParaTransactionInFirestore(
                passcode = dist.roomPasscode,
                paraNumber = paraNumber,
                memberName = memberName,
                isGuest = isGuest
            )
            if (!cloudSuccess) return false
        }

        dao.insertParticipant(
            ParticipantEntity(
                distributionId = distributionId,
                name = FirebaseCoreManager.sanitizeInput(memberName),
                allocatedCount = 1,
                displayOrder = paraNumber,
                specificParaNumber = paraNumber,
                isCompleted = false,
                isGuestEntry = isGuest,
                addedByProfileName = FirebaseCoreManager.sanitizeInput(addedByProfileName)
            )
        )
        return true
    }

    /**
     * Claims multiple available Paras at once in a One-Time Khatm (first-come, first-served).
     */
    suspend fun claimMultipleOneTimeParas(
        distributionId: Long,
        paraNumbers: List<Int>,
        memberName: String,
        isGuest: Boolean,
        addedByProfileName: String
    ): Int {
        var claimedCount = 0
        for (p in paraNumbers.distinct().sorted()) {
            if (claimOneTimePara(distributionId, p, memberName, isGuest, addedByProfileName)) {
                claimedCount++
            }
        }
        return claimedCount
    }

    /**
     * Joins an Online Monthly Khatm or Online Amal room and claims a count of Paras/Recitations
     * (either for the user themselves or for a Guest like Grandma).
     */
    suspend fun joinAndClaimAllocation(
        distributionId: Long,
        memberName: String,
        requestedCount: Int,
        isGuest: Boolean,
        addedByProfileName: String
    ): Boolean {
        if (memberName.isBlank() || requestedCount <= 0) return false
        val dist = dao.getDistributionById(distributionId) ?: return false
        if (dist.isClosedForever) return false

        val existing = dao.getParticipantsForDistributionList(distributionId)
        val currentAllocated = existing.sumOf { it.allocatedCount }
        val remaining = (dist.totalTarget - currentAllocated).coerceAtLeast(0)
        val actualCount = requestedCount.coerceAtMost(remaining)
        if (actualCount <= 0) return false

        val nextOrder = (existing.maxOfOrNull { it.displayOrder } ?: 0) + 1
        val newParticipant = ParticipantEntity(
            distributionId = distributionId,
            name = FirebaseCoreManager.sanitizeInput(memberName),
            allocatedCount = actualCount,
            displayOrder = nextOrder,
            isCompleted = false,
            isGuestEntry = isGuest,
            addedByProfileName = FirebaseCoreManager.sanitizeInput(addedByProfileName)
        )
        dao.insertParticipant(newParticipant)
        if (dist.isOnline) {
            FirebaseCoreManager.upsertSingleParticipantInFirestore(dist, newParticipant)
        }
        return true
    }

    suspend fun removeParticipantClaim(participantId: Long) {
        val existing = dao.getParticipantById(participantId)
        dao.deleteParticipantById(participantId)
        if (existing != null) {
            val dist = dao.getDistributionById(existing.distributionId)
            if (dist != null && dist.isOnline) {
                FirebaseCoreManager.deleteParticipantSlotFromFirestore(dist, existing)
            }
        }
    }

    suspend fun toggleParticipantPin(participantId: Long) {
        val existing = dao.getParticipantById(participantId) ?: return
        dao.updateParticipant(existing.copy(isPinned = !existing.isPinned))
    }

    /**
     * Updates the tactile recitation counter for a participant in a Custom Amal room.
     * Automatically marks the participant's pledge as completed when [newCount] reaches [allocatedCount].
     */
    suspend fun updateAmalCompletedCount(
        distributionId: Long,
        participantId: Long,
        newCount: Int
    ): CompletionUpdateResult? {
        val dist = dao.getDistributionById(distributionId) ?: return null
        if (dist.type != DistributionType.AMAL || dist.isClosedForever) return null
        val participant = dao.getParticipantById(participantId) ?: return null

        val clampedCount = newCount.coerceIn(0, participant.allocatedCount)
        val isNowComplete = clampedCount >= participant.allocatedCount
        val wasCompleteBefore = participant.isCompleted

        val updated = participant.copy(
            completedCount = clampedCount,
            isCompleted = isNowComplete
        )
        dao.updateParticipant(updated)
        if (dist.isOnline) {
            val synced = FirebaseCoreManager.upsertSingleParticipantInFirestore(dist, updated)
            if (!synced) {
                dao.updateParticipant(updated.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
                dao.updateDistribution(dist.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
            } else {
                dao.updateDistribution(dist.copy(hasPendingSync = false, lastSyncedTimestamp = System.currentTimeMillis()))
            }
        }

        if (!isNowComplete || wasCompleteBefore) {
            return null
        }

        val allAfter = dao.getParticipantsForDistributionList(distributionId)
        val completedSum = allAfter.sumOf {
            if (it.isCompleted) it.allocatedCount else it.completedCount.coerceIn(0, it.allocatedCount)
        }
        val pendingMembers = allAfter.count { !it.isCompleted }
        val isFullComplete = completedSum >= dist.totalTarget && pendingMembers == 0 && allAfter.isNotEmpty()

        return CompletionUpdateResult(
            wasMarkedComplete = true,
            memberName = updated.name,
            itemLabel = "${updated.allocatedCount} recitations",
            remainingPendingCount = pendingMembers,
            remainingUnitLabel = if (pendingMembers == 1) "member" else "members",
            justCompletedFullDistribution = isFullComplete
        )
    }

    /**
     * Toggles completion tickmark for a participant's Para or Amal.
     * If a One-Time Qur'an Khatm reaches 30/30 completed Paras, automatically cleans up
     * detailed participant records to save space and closes the room forever.
     */
    suspend fun toggleParticipantCompletion(
        distributionId: Long,
        participantId: Long,
        targetYear: Int,
        targetMonth: Int,
        assignedParaLabel: String? = null
    ): CompletionUpdateResult? {
        val dist = dao.getDistributionById(distributionId) ?: return null
        if (dist.isClosedForever) return null
        val participant = dao.getParticipantById(participantId) ?: return null

        return when (dist.type) {
            DistributionType.ONE_TIME_KHATM -> {
                val updated = participant.copy(isCompleted = !participant.isCompleted)
                dao.updateParticipant(updated)

                val allAfter = dao.getParticipantsForDistributionList(distributionId)
                val completedParas = allAfter.count { it.isCompleted }
                val remainingParas = (30 - completedParas).coerceAtLeast(0)
                val paraNum = updated.specificParaNumber ?: updated.displayOrder

                if (updated.isCompleted && completedParas >= 30 && allAfter.size >= 30) {
                    // All 30 Paras are completed! Perform space-saving cleanup & close room forever
                    val uniqueMembers = allAfter.map { it.name.trim().lowercase() }.distinct().size
                    val summary = "30/30 Paras Completed • $uniqueMembers ${if (uniqueMembers == 1) "Member" else "Members"} • Closed"
                    dao.deleteParticipantsByDistribution(distributionId)
                    dao.updateDistribution(
                        dist.copy(
                            isClosedForever = true,
                            closedSummaryText = summary,
                            completedTimestamp = System.currentTimeMillis()
                        )
                    )
                    if (dist.isOnline && !dist.roomPasscode.isNullOrBlank()) {
                        FirebaseCoreManager.clearAllParticipantSlotsInFirestore(dist.roomPasscode)
                    }
                    syncDistributionWithFirestore(distributionId)
                    CompletionUpdateResult(
                        wasMarkedComplete = true,
                        memberName = updated.name,
                        itemLabel = "Para $paraNum",
                        remainingPendingCount = 0,
                        remainingUnitLabel = "Paras",
                        justCompletedFullDistribution = true,
                        autoCleanedUpOneTimeKhatm = true
                    )
                } else {
                    if (dist.isOnline) {
                        val synced = FirebaseCoreManager.upsertSingleParticipantInFirestore(dist, updated)
                        if (!synced) {
                            dao.updateParticipant(updated.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
                            dao.updateDistribution(dist.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
                        } else {
                            dao.updateDistribution(dist.copy(hasPendingSync = false, lastSyncedTimestamp = System.currentTimeMillis()))
                        }
                    }
                    CompletionUpdateResult(
                        wasMarkedComplete = updated.isCompleted,
                        memberName = updated.name,
                        itemLabel = "Para $paraNum",
                        remainingPendingCount = remainingParas,
                        remainingUnitLabel = if (remainingParas == 1) "Para" else "Paras",
                        justCompletedFullDistribution = false
                    )
                }
            }

            DistributionType.AMAL -> {
                val nextCompleted = !participant.isCompleted
                val updated = participant.copy(
                    isCompleted = nextCompleted,
                    completedCount = if (nextCompleted) participant.allocatedCount else 0
                )
                dao.updateParticipant(updated)
                if (dist.isOnline) {
                    val synced = FirebaseCoreManager.upsertSingleParticipantInFirestore(dist, updated)
                    if (!synced) {
                        dao.updateParticipant(updated.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
                        dao.updateDistribution(dist.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
                    } else {
                        dao.updateDistribution(dist.copy(hasPendingSync = false, lastSyncedTimestamp = System.currentTimeMillis()))
                    }
                }

                val allAfter = dao.getParticipantsForDistributionList(distributionId)
                val completedSum = allAfter.filter { it.isCompleted }.sumOf { it.allocatedCount }
                val pendingMembers = allAfter.count { !it.isCompleted }
                val isFullComplete = updated.isCompleted && completedSum >= dist.totalTarget && allAfter.isNotEmpty()

                CompletionUpdateResult(
                    wasMarkedComplete = updated.isCompleted,
                    memberName = updated.name,
                    itemLabel = "${updated.allocatedCount} recitations",
                    remainingPendingCount = pendingMembers,
                    remainingUnitLabel = if (pendingMembers == 1) "member" else "members",
                    justCompletedFullDistribution = isFullComplete
                )
            }

            DistributionType.KHATM -> {
                val updated = participant.withToggledMonthCompletion(targetYear, targetMonth)
                val isNowDone = updated.isCompletedForMonth(targetYear, targetMonth)
                dao.updateParticipant(updated)
                if (dist.isOnline) {
                    val synced = FirebaseCoreManager.upsertSingleParticipantInFirestore(dist, updated)
                    if (!synced) {
                        dao.updateParticipant(updated.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
                        dao.updateDistribution(dist.copy(hasPendingSync = true, lastModifiedTimestamp = System.currentTimeMillis()))
                    } else {
                        dao.updateDistribution(dist.copy(hasPendingSync = false, lastSyncedTimestamp = System.currentTimeMillis()))
                    }
                }

                val allAfter = dao.getParticipantsForDistributionList(distributionId)
                val pendingMembers = allAfter.count { !it.isCompletedForMonth(targetYear, targetMonth) }
                val completedParasSum = allAfter
                    .filter { it.isCompletedForMonth(targetYear, targetMonth) }
                    .sumOf { it.allocatedCount }
                val isFullComplete = isNowDone && completedParasSum >= 30 && pendingMembers == 0

                CompletionUpdateResult(
                    wasMarkedComplete = isNowDone,
                    memberName = updated.name,
                    itemLabel = assignedParaLabel ?: "${updated.allocatedCount} Para(s)",
                    remainingPendingCount = pendingMembers,
                    remainingUnitLabel = if (pendingMembers == 1) "member" else "members",
                    justCompletedFullDistribution = isFullComplete
                )
            }
        }
    }

    /**
     * Reopens an Amal distribution for a fresh round with the same Amal settings.
     * If it is an Online Room, generates a brand-new unique passcode so the host can share it
     * with the same or new people from the start without recreating the distribution.
     */
    suspend fun reopenAmalRoom(
        distributionId: Long,
        keepMembersAndResetTicks: Boolean
    ): String? {
        val dist = dao.getDistributionById(distributionId) ?: return null
        val newPasscode = if (dist.isOnline) {
            generateUniquePasscode(DistributionType.AMAL)
        } else {
            null
        }

        val updatedDist = dist.copy(
            roundNumber = dist.roundNumber + 1,
            roomPasscode = newPasscode,
            isClosedForever = false,
            completedTimestamp = null
        )
        dao.updateDistribution(updatedDist)

        if (keepMembersAndResetTicks) {
            val currentMembers = dao.getParticipantsForDistributionList(distributionId)
            val resetMembers = currentMembers.map { it.copy(isCompleted = false, completedCount = 0) }
            dao.updateFullDistribution(updatedDist, resetMembers)
        } else {
            // Clear old claims so old or new members can join and pledge from the start
            dao.deleteParticipantsByDistribution(distributionId)
        }
        syncDistributionWithFirestore(distributionId)
        return newPasscode
    }

    /**
     * Joins an Online Room by Passcode or pasted Share Invite text.
     * If the room already exists locally, returns its ID immediately.
     * If shared from another device, reconstructs the Online Room with that passcode so the user can join seamlessly.
     */
    suspend fun joinRoomByPasscodeOrInvite(
        rawInput: String,
        customRoomTitle: String? = null
    ): Long? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        // Extract QBM-XXXX..., QBO-XXXX..., QBA-XXXX... or QB-XXXX... code if pasted inside an invite message
        val codeRegex = Regex("\\b(QB[MOA]?-[A-Z0-9]{4,10})\\b", RegexOption.IGNORE_CASE)
        val matchedCode = codeRegex.find(trimmed)?.value?.uppercase()
            ?: trimmed.uppercase().replace(" ", "").take(14)

        if (matchedCode.length < 4) return null

        // 1. Check if room already exists on this device
        val existing = dao.getDistributionByPasscode(matchedCode)
        if (existing != null) {
            val cloudSnapshot = FirebaseCoreManager.fetchOnlineRoomByPasscode(matchedCode)
            if (cloudSnapshot != null) {
                applyFirestoreSnapshotToLocalRoom(existing.id, cloudSnapshot)
            }
            return existing.id
        }

        // 2. Check Firestore cloud for a live Online Room with this passcode
        val cloudRoom = FirebaseCoreManager.fetchOnlineRoomByPasscode(matchedCode)
        if (cloudRoom != null) {
            val cloudEntity = DistributionEntity(
                name = if (!customRoomTitle.isNullOrBlank()) customRoomTitle.trim() else cloudRoom.name,
                type = cloudRoom.type,
                roomMode = RoomMode.ONLINE,
                roomPasscode = matchedCode,
                totalTarget = cloudRoom.totalTarget,
                startYear = cloudRoom.startYear,
                startMonth = cloudRoom.startMonth,
                roundNumber = cloudRoom.roundNumber,
                isClosedForever = cloudRoom.isClosedForever,
                closedSummaryText = cloudRoom.closedSummaryText
            )
            val cloudParticipants = cloudRoom.participants
                .sortedBy { it.displayOrder }
                .map { dto ->
                    ParticipantEntity(
                        distributionId = 0,
                        name = dto.name,
                        allocatedCount = dto.allocatedCount,
                        completedCount = if (dto.isCompleted && dto.completedCount == 0) dto.allocatedCount else dto.completedCount.coerceIn(0, dto.allocatedCount),
                        displayOrder = dto.displayOrder,
                        specificParaNumber = dto.specificParaNumber,
                        isCompleted = dto.isCompleted,
                        completedCycleMonthsCsv = dto.completedMonthsCsv,
                        isGuestEntry = dto.isGuestEntry
                    )
                }
            return dao.insertFullDistribution(cloudEntity, cloudParticipants)
        }

        // 3. Reconstruct Online Room from shared passcode / invite text if offline/local
        val inferredType = when {
            matchedCode.startsWith("QBO-") -> DistributionType.ONE_TIME_KHATM
            matchedCode.startsWith("QBA-") -> DistributionType.AMAL
            matchedCode.startsWith("QBM-") -> DistributionType.KHATM
            else -> DistributionType.ONE_TIME_KHATM
        }

        // Try to parse title or target if user pasted full invite message
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        val inferredTitle = when {
            !customRoomTitle.isNullOrBlank() -> customRoomTitle.trim()
            lines.size > 1 && !lines.first().equals(matchedCode, ignoreCase = true) -> {
                lines.first().removePrefix("🕌").removePrefix("📖").trim()
            }
            inferredType == DistributionType.ONE_TIME_KHATM -> "Shared Khatm Room ($matchedCode)"
            inferredType == DistributionType.AMAL -> "Shared Amal Room ($matchedCode)"
            else -> "Monthly Khatm Room ($matchedCode)"
        }

        val targetRegex = Regex("Target:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val parsedTarget = targetRegex.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val targetCount = if (inferredType == DistributionType.AMAL) (parsedTarget ?: 110) else 30

        val cal = Calendar.getInstance()
        val newEntity = DistributionEntity(
            name = inferredTitle,
            type = inferredType,
            roomMode = RoomMode.ONLINE,
            roomPasscode = matchedCode,
            totalTarget = targetCount,
            startYear = cal.get(Calendar.YEAR),
            startMonth = cal.get(Calendar.MONTH) + 1,
            roundNumber = 1
        )
        return dao.insertDistribution(newEntity)
    }

    suspend fun deleteDistribution(id: Long) {
        dao.deleteDistribution(id)
    }
}
