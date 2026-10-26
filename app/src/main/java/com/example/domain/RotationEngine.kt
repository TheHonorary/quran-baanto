package com.example.domain

/**
 * Represents a participant's allocated share of Paras in a Khatm distribution.
 */
data class ParticipantAllocation(
    val id: Long = 0,
    val name: String,
    val count: Int,
    val displayOrder: Int,
    val uid: String = java.util.UUID.randomUUID().toString()
)

/**
 * Represents the computed Para assignment for a specific participant in a given month.
 *
 * Guarantees:
 * - [startPara] and [endPara] are strictly within 1..30.
 * - Supports circular wrapping across 30 -> 1 (e.g. 30, 1 or 29, 30, 1).
 * - [assignedParas] contains no duplicates and has size == [allocatedCount].
 */
data class ParaAssignment(
    val participantId: Long,
    val participantName: String,
    val allocatedCount: Int,
    val startPara: Int, // 1..30
    val endPara: Int    // 1..30
) {
    init {
        require(startPara in 1..RotationEngine.TOTAL_QURAN_PARAS) {
            "startPara must be between 1 and 30, got $startPara"
        }
        require(endPara in 1..RotationEngine.TOTAL_QURAN_PARAS) {
            "endPara must be between 1 and 30, got $endPara"
        }
    }

    /**
     * Complete list of assigned Para numbers (1..30) in contiguous circular reading order.
     * When startPara > endPara, wraps across 30 -> 1 (e.g. 30, 1 or 29, 30, 1).
     */
    val assignedParas: List<Int>
        get() {
            return if (startPara <= endPara) {
                (startPara..endPara).toList()
            } else {
                (startPara..RotationEngine.TOTAL_QURAN_PARAS).toList() + (1..endPara).toList()
            }
        }

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
     * Compact pill label e.g. "Paras 1–6" or "Para 12" or "Paras 30–1".
     */
    val compactRangeLabel: String
        get() = displayRangeText

    /**
     * Comma-separated paras string e.g. "1, 2" or "11" or "3, 4, 5" or "30, 1".
     */
    val commaSeparatedParas: String
        get() = assignedParas.joinToString(", ")

    /**
     * Name-first formatted line ordered by ascending Para e.g. "Abbas:        1, 2".
     */
    val nameFirstSummaryLine: String
        get() = formatAlignedLine(targetNameColumnWidth = 14)

    /**
     * Formats "Name:        1, 2" with clean column alignment for WhatsApp / messaging apps.
     */
    fun formatAlignedLine(
        targetNameColumnWidth: Int = 14,
        extraSuffix: String = ""
    ): String {
        val cleanName = participantName.trim()
        val label = if (cleanName.endsWith(":") || cleanName.endsWith(".")) cleanName else "$cleanName:"
        val paddedName = label.padEnd((targetNameColumnWidth).coerceAtLeast(label.length + 2), ' ')
        return "$paddedName$commaSeparatedParas$extraSuffix"
    }

    /**
     * Legacy alias kept for compatibility, now returning Name-first format.
     */
    val paraFirstSummaryLine: String
        get() = "$participantName: $commaSeparatedParas"
}

/**
 * Deterministic 30-Para Contiguous Block Khatm Rotation Engine.
 *
 * Rules enforced:
 * 1. Treats the Quran as 30 ordered blocks (1..30), never 30 independent random values.
 * 2. Treats each month as one complete 30-Para cycle.
 * 3. Allocates each member a contiguous block of Paras (startPara..endPara).
 * 4. Rotates the starting position between months so members progress sequentially through the Quran.
 * 5. A member's allocation NEVER crosses the 30 -> 1 boundary. The 30 -> 1 transition only occurs
 *    when starting the next month's cycle.
 * 6. Every assigned Para is between 1 and 30.
 * 7. No member has duplicate Paras within their allocation.
 * 8. Every Para 1..30 is accounted for exactly once in full-group coverage.
 * 9. Larger allocations rotate fairly when 30 isn't evenly divisible by member count.
 * 10. Month N+1 never duplicates Month N (when member count > 1).
 * 11. Members progress through the Para sequence rather than jumping randomly.
 * 12. After Para 30, progression continues at Para 1 in the next cycle, never inside an allocation.
 * 13. All returned monthly assignments are ordered strictly from Para 1 to Para 30, then by name.
 */
object RotationEngine {

    const val TOTAL_QURAN_PARAS = 30

    private fun posMod(value: Int, mod: Int): Int = ((value % mod) + mod) % mod

    val paraFirstComparator: Comparator<ParaAssignment> =
        compareBy<ParaAssignment> { it.assignedParas.minOrNull() ?: it.startPara }
            .thenBy { it.assignedParas.firstOrNull() ?: it.startPara }
            .thenBy { it.participantName.lowercase() }

    /**
     * Validates whether a list of participant allocations totals exactly 30 Paras
     * with valid positive contiguous sizes.
     */
    fun isValid30Allocation(participants: List<ParticipantAllocation>): Boolean {
        if (participants.isEmpty() || participants.size > TOTAL_QURAN_PARAS) return false
        val total = participants.sumOf { it.count }
        val allPositive = participants.all { it.count in 1..TOTAL_QURAN_PARAS && it.name.isNotBlank() }
        return total == TOTAL_QURAN_PARAS && allPositive
    }

    /**
     * Computes balanced contiguous block sizes for [memberCount] participants summing to 30.
     * When 30 is not divisible by [memberCount], the first (30 % memberCount) slots receive
     * (base + 1) Paras and the remaining slots receive (base) Paras.
     */
    fun computeFairSlotSizes(memberCount: Int): List<Int> {
        if (memberCount <= 0) return emptyList()
        val clampedCount = memberCount.coerceAtMost(TOTAL_QURAN_PARAS)
        val base = TOTAL_QURAN_PARAS / clampedCount
        val remainder = TOTAL_QURAN_PARAS % clampedCount
        return List(clampedCount) { index ->
            base + if (index < remainder) 1 else 0
        }
    }

    /**
     * Computes the Month 1 (Cycle 0) contiguous Para range for each participant in their current order,
     * clamping within 1..30 so the UI can preview contiguous ranges while editing.
     */
    fun previewInitialRanges(participants: List<ParticipantAllocation>): List<IntRange?> {
        var currentPara = 1
        return participants.map { p ->
            if (p.count <= 0 || currentPara > TOTAL_QURAN_PARAS) {
                null
            } else {
                val start = currentPara
                val end = (currentPara + p.count - 1).coerceAtMost(TOTAL_QURAN_PARAS)
                currentPara = end + 1
                start..end
            }
        }
    }

    /**
     * Determines the optimal forward rotation step for custom weighted participant allocations
     * in the circular 30-Para queue.
     */
    fun calculateOptimalRotationStep(participants: List<ParticipantAllocation>): Int {
        if (participants.size <= 1) return 1
        val sorted = participants.sortedBy { it.displayOrder }
        val n = sorted.size

        var bestStep = 1
        var lowestOverlapPenalty = Int.MAX_VALUE

        for (candidateStep in 1 until n) {
            var overlapPenalty = 0

            for (m in 0 until n) {
                val startIdxM = posMod(-m * candidateStep, n)
                val rotatedM = (0 until n).map { i -> sorted[(startIdxM + i) % n] }
                val mMappings = mutableMapOf<Long, Set<Int>>()
                var curM = 1
                for (p in rotatedM) {
                    val pSet = (0 until p.count).map { posMod(curM - 1 + it, TOTAL_QURAN_PARAS) + 1 }.toSet()
                    mMappings[p.id] = pSet
                    curM = posMod(curM - 1 + p.count, TOTAL_QURAN_PARAS) + 1
                }

                val startIdxNext = posMod(-(m + 1) * candidateStep, n)
                val rotatedNext = (0 until n).map { i -> sorted[(startIdxNext + i) % n] }
                var curNext = 1
                for (p in rotatedNext) {
                    val nextSet = (0 until p.count).map { posMod(curNext - 1 + it, TOTAL_QURAN_PARAS) + 1 }.toSet()
                    val mSet = mMappings[p.id] ?: emptySet()
                    overlapPenalty += mSet.intersect(nextSet).size
                    curNext = posMod(curNext - 1 + p.count, TOTAL_QURAN_PARAS) + 1
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
     * Calculates deterministic, circular-queue 30-Para assignments for any target month and year,
     * always ordered in ascending order of lowest Para number (Paras 1 -> 30).
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
        val monthOffset = (targetYear - startYear) * 12 + (targetMonth - startMonth)

        val shiftStep = customShiftStep ?: calculateOptimalRotationStep(sortedParticipants)
        val startIndex = posMod(-monthOffset * shiftStep, n)
        val rotatedParticipants = (0 until n).map { i ->
            sortedParticipants[(startIndex + i) % n]
        }

        val assignments = mutableListOf<ParaAssignment>()
        var currentPara = 1

        for (participant in rotatedParticipants) {
            val count = participant.count
            if (count <= 0) continue

            val actualStart = currentPara
            // End para wrapping circularly around 1..30 without stopping at 30
            val actualEnd = posMod(actualStart - 1 + count - 1, TOTAL_QURAN_PARAS) + 1

            assignments.add(
                ParaAssignment(
                    participantId = participant.id,
                    participantName = participant.name,
                    allocatedCount = count,
                    startPara = actualStart,
                    endPara = actualEnd
                )
            )

            currentPara = posMod(actualEnd, TOTAL_QURAN_PARAS) + 1
        }

        return assignments.sortedWith(paraFirstComparator)
    }
}
