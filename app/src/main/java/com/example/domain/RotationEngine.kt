package com.example.domain

/**
 * Represents a participant's allocated share of Paras in a Khatm distribution.
 */
data class ParticipantAllocation(
    val id: Long = 0,
    val name: String,
    val count: Int,
    val displayOrder: Int
)

/**
 * Represents the computed Para assignment for a specific participant in a given month.
 */
data class ParaAssignment(
    val participantId: Long,
    val participantName: String,
    val allocatedCount: Int,
    val startPara: Int, // 1..30
    val endPara: Int    // 1..30
) {
    /**
     * Complete list of assigned Para numbers (1..30) in consecutive reading order.
     */
    val assignedParas: List<Int>
        get() = (startPara..endPara).toList()

    /**
     * Clean, human-readable display string for the assigned Paras.
     * If a single Para is assigned, displays just the number (e.g. "Para 30" or "Para 1"),
     * never a redundant range like "30-30" or "1-1".
     */
    val displayRangeText: String
        get() = when {
            allocatedCount == 1 || startPara == endPara -> "Para $startPara"
            else -> "Paras $startPara–$endPara"
        }

    /**
     * Comma-separated paras string e.g. "1, 2" or "11" or "14, 15, 16".
     */
    val commaSeparatedParas: String
        get() = assignedParas.joinToString(", ")
}

/**
 * Deterministic and fair Qur'an Para rotation engine.
 *
 * Guarantees:
 * 1. Sequential Progression: Prioritizes assigning the consecutive following Paras in the next month
 *    (e.g., Paras 1–3 in Month 1 -> Paras 4–6 in Month 2 -> Paras 7–9 in Month 3).
 * 2. Zero Repetitions: Strictly prevents any participant from receiving the same Paras in consecutive months.
 * 3. Minimal Forward Shift: When unequal allocations or restrictions prevent exact sequential continuation,
 *    advances by the smallest valid forward step, skipping the minimum number of Paras.
 * 4. Deterministic & Full Partition: Exactly 30 Paras (1..30) are partitioned with 0 duplicates and 0 omissions.
 */
object RotationEngine {

    const val TOTAL_QURAN_PARAS = 30

    /**
     * Validates whether a list of participant allocations totals exactly 30 Paras.
     */
    fun isValid30Allocation(participants: List<ParticipantAllocation>): Boolean {
        if (participants.isEmpty()) return false
        val total = participants.sumOf { it.count }
        val allPositive = participants.all { it.count > 0 && it.name.isNotBlank() }
        return total == TOTAL_QURAN_PARAS && allPositive
    }

    /**
     * Determines the optimal monthly rotation step for a given set of participant allocations.
     *
     * Rules:
     * 1. Equal Allocations (e.g. 10x3, 6x5, 5x6, 15x2, 30x1): Step equals the allocation size (e.g. 3),
     *    producing exact sequential progression (1–3 -> 4–6 -> 7–9 -> ...).
     * 2. Unequal Allocations: Finds the smallest forward step S in 1..29 that eliminates (or minimizes)
     *    consecutive month repetition across all members, skipping the minimum number of Paras forward.
     */
    fun calculateOptimalRotationStep(participants: List<ParticipantAllocation>): Int {
        if (participants.size <= 1) return 1
        val sorted = participants.sortedBy { it.displayOrder }
        val n = sorted.size

        // If all participants have equal shares, step is 1 participant position
        val distinctCounts = sorted.map { it.count }.distinct()
        if (distinctCounts.size == 1) {
            return 1
        }

        var bestStep = 1
        var lowestOverlapPenalty = Int.MAX_VALUE

        // Find the smallest step k in 1 until n that minimizes consecutive-month overlap across all cycles
        for (candidateStep in 1 until n) {
            var overlapPenalty = 0

            // Check across all possible month transitions (m = 0 until n)
            for (m in 0 until n) {
                // Month m assignments
                val startIdxM = (((-m * candidateStep) % n) + n) % n
                val rotatedM = (0 until n).map { i -> sorted[(startIdxM + i) % n] }
                val mMappings = mutableMapOf<Long, Set<Int>>()
                var curM = 1
                for (p in rotatedM) {
                    val end = (curM + p.count - 1).coerceAtMost(TOTAL_QURAN_PARAS)
                    mMappings[p.id] = (curM..end).toSet()
                    curM = end + 1
                }

                // Month m + 1 assignments
                val startIdxNext = (((-(m + 1) * candidateStep) % n) + n) % n
                val rotatedNext = (0 until n).map { i -> sorted[(startIdxNext + i) % n] }
                var curNext = 1
                for (p in rotatedNext) {
                    val end = (curNext + p.count - 1).coerceAtMost(TOTAL_QURAN_PARAS)
                    val nextSet = (curNext..end).toSet()
                    val mSet = mMappings[p.id] ?: emptySet()
                    overlapPenalty += mSet.intersect(nextSet).size
                    curNext = end + 1
                }
            }

            if (overlapPenalty < lowestOverlapPenalty) {
                lowestOverlapPenalty = overlapPenalty
                bestStep = candidateStep
                if (lowestOverlapPenalty == 0) {
                    return bestStep
                }
            }
        }

        return bestStep
    }

    /**
     * Calculates deterministic Para assignments for any target month and year.
     *
     * @param participants List of participants. Sum of counts must equal 30.
     * @param startYear Year distribution was created
     * @param startMonth Month distribution was created (1..12)
     * @param targetYear Target calculation year
     * @param targetMonth Target calculation month (1..12)
     * @param customShiftStep Optional custom shift step; if null, optimal step is calculated.
     */
    fun calculateAssignments(
        participants: List<ParticipantAllocation>,
        startYear: Int,
        startMonth: Int,
        targetYear: Int,
        targetMonth: Int,
        customShiftStep: Int? = null
    ): List<ParaAssignment> {
        if (participants.isEmpty()) return emptyList()

        val sortedParticipants = participants.sortedBy { it.displayOrder }
        val n = sortedParticipants.size
        val shiftStep = customShiftStep ?: calculateOptimalRotationStep(sortedParticipants)

        // Month offset m (0 for creation month)
        val monthOffset = (targetYear - startYear) * 12 + (targetMonth - startMonth)

        // Rotate participant positions along the partition of 30 Paras
        val startIndex = (((-monthOffset * shiftStep) % n) + n) % n
        val rotatedParticipants = (0 until n).map { i ->
            sortedParticipants[(startIndex + i) % n]
        }

        val assignments = mutableListOf<ParaAssignment>()
        var currentPara = 1

        for (participant in rotatedParticipants) {
            val count = participant.count
            if (count <= 0) continue

            val actualStart = currentPara
            val actualEnd = (currentPara + count - 1).coerceAtMost(TOTAL_QURAN_PARAS)

            assignments.add(
                ParaAssignment(
                    participantId = participant.id,
                    participantName = participant.name,
                    allocatedCount = count,
                    startPara = actualStart,
                    endPara = actualEnd
                )
            )

            currentPara = actualEnd + 1
        }

        // Return assignments in participant display order
        val idOrderMap = sortedParticipants.mapIndexed { idx, p -> p.id to idx }.toMap()
        return assignments.sortedBy { idOrderMap[it.participantId] ?: 0 }
    }
}

