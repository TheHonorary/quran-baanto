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
 * - [startPara] <= [endPara] (contiguous block, never crosses the 30 -> 1 boundary).
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
        require(startPara <= endPara) {
            "Allocation must be contiguous and never cross 30->1 boundary (start=$startPara, end=$endPara)"
        }
    }

    /**
     * Complete list of assigned Para numbers (1..30) in contiguous reading order.
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
     * Compact pill label e.g. "Paras 1–6" or "Para 12".
     */
    val compactRangeLabel: String
        get() = when {
            allocatedCount == 1 || startPara == endPara -> "Para $startPara"
            else -> "Paras $startPara–$endPara"
        }

    /**
     * Comma-separated paras string e.g. "1, 2" or "11" or "3, 4, 5".
     */
    val commaSeparatedParas: String
        get() = assignedParas.joinToString(", ")

    /**
     * Para-first formatted line e.g. "1, 2: XYZ" or "3, 4, 5: ABC".
     */
    val paraFirstSummaryLine: String
        get() = "$commaSeparatedParas: $participantName"
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

    private val paraFirstComparator: Comparator<ParaAssignment> =
        compareBy<ParaAssignment> { it.startPara }
            .thenBy { it.endPara }
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
     * Determines the optimal forward rotation step for custom weighted participant allocations.
     */
    fun calculateOptimalRotationStep(participants: List<ParticipantAllocation>): Int {
        if (participants.size <= 1) return 1
        val sorted = participants.sortedBy { it.displayOrder }
        val n = sorted.size

        val minCount = sorted.minOf { it.count }
        val maxCount = sorted.maxOf { it.count }
        if (maxCount - minCount <= 1) {
            return 1
        }

        var bestStep = 1
        var lowestOverlapPenalty = Int.MAX_VALUE

        for (candidateStep in 1 until n) {
            var overlapPenalty = 0

            for (m in 0 until n) {
                val startIdxM = (((-m * candidateStep) % n) + n) % n
                val rotatedM = (0 until n).map { i -> sorted[(startIdxM + i) % n] }
                val mMappings = mutableMapOf<Long, Set<Int>>()
                var curM = 1
                for (p in rotatedM) {
                    val end = (curM + p.count - 1).coerceAtMost(TOTAL_QURAN_PARAS)
                    mMappings[p.id] = (curM..end).toSet()
                    curM = end + 1
                }

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
     * Calculates deterministic, contiguous 30-Para assignments for any target month and year,
     * always ordered from Para 1 to Para 30, then by participant name.
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

        val minCount = sortedParticipants.minOf { it.count }
        val maxCount = sortedParticipants.maxOf { it.count }
        val totalParas = sortedParticipants.sumOf { it.count }

        // Balanced / Fair-Remainder Mode (e.g. equal shares or 30 divided across N members where max - min <= 1)
        if (totalParas == TOTAL_QURAN_PARAS && maxCount - minCount <= 1 && customShiftStep == null) {
            val slotBlocks = ArrayList<IntRange>(n)
            var cur = 1
            for (i in 0 until n) {
                val slotSize = sortedParticipants[i].count
                val end = (cur + slotSize - 1).coerceAtMost(TOTAL_QURAN_PARAS)
                slotBlocks.add(cur..end)
                cur = end + 1
            }

            // In month m, member i progresses to slot (i + m) mod N, then sort strictly 1 -> 30
            return sortedParticipants.mapIndexed { memberIdx, participant ->
                val slotIdx = (((memberIdx + monthOffset) % n) + n) % n
                val range = slotBlocks[slotIdx]
                val blockCount = range.last - range.first + 1
                ParaAssignment(
                    participantId = participant.id,
                    participantName = participant.name,
                    allocatedCount = blockCount,
                    startPara = range.first,
                    endPara = range.last
                )
            }.sortedWith(paraFirstComparator)
        }

        // Custom Weighted Mode: each participant retains their specific custom count
        val shiftStep = customShiftStep ?: calculateOptimalRotationStep(sortedParticipants)
        val startIndex = (((-monthOffset * shiftStep) % n) + n) % n
        val rotatedParticipants = (0 until n).map { i ->
            sortedParticipants[(startIndex + i) % n]
        }

        val assignments = mutableListOf<ParaAssignment>()
        var currentPara = 1

        for (participant in rotatedParticipants) {
            val count = participant.count
            if (count <= 0 || currentPara > TOTAL_QURAN_PARAS) continue

            val actualStart = currentPara
            val actualEnd = (currentPara + count - 1).coerceAtMost(TOTAL_QURAN_PARAS)

            assignments.add(
                ParaAssignment(
                    participantId = participant.id,
                    participantName = participant.name,
                    allocatedCount = actualEnd - actualStart + 1,
                    startPara = actualStart,
                    endPara = actualEnd
                )
            )

            currentPara = actualEnd + 1
        }

        return assignments.sortedWith(paraFirstComparator)
    }
}
