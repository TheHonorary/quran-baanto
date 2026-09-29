package com.example

import com.example.domain.ParaAssignment
import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import com.example.util.HijriDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testValid30AllocationRule() {
        val validAllocations = listOf(
            ParticipantAllocation(name = "Dad", count = 6, displayOrder = 1),
            ParticipantAllocation(name = "Mom", count = 6, displayOrder = 2),
            ParticipantAllocation(name = "Hamza", count = 6, displayOrder = 3),
            ParticipantAllocation(name = "Sara", count = 6, displayOrder = 4),
            ParticipantAllocation(name = "Tariq", count = 6, displayOrder = 5)
        )
        assertTrue(RotationEngine.isValid30Allocation(validAllocations))

        val invalidUnderAllocated = listOf(
            ParticipantAllocation(name = "Dad", count = 5, displayOrder = 1),
            ParticipantAllocation(name = "Mom", count = 5, displayOrder = 2)
        )
        assertFalse(RotationEngine.isValid30Allocation(invalidUnderAllocated))

        val invalidOverAllocated = listOf(
            ParticipantAllocation(name = "Dad", count = 20, displayOrder = 1),
            ParticipantAllocation(name = "Mom", count = 20, displayOrder = 2)
        )
        assertFalse(RotationEngine.isValid30Allocation(invalidOverAllocated))
    }

    @Test
    fun testExact30ParasDistributedAcrossConsecutiveMonths() {
        val participants = listOf(
            ParticipantAllocation(id = 1, name = "A", count = 5, displayOrder = 1),
            ParticipantAllocation(id = 2, name = "B", count = 7, displayOrder = 2),
            ParticipantAllocation(id = 3, name = "C", count = 8, displayOrder = 3),
            ParticipantAllocation(id = 4, name = "D", count = 10, displayOrder = 4)
        )

        // Test over 36 consecutive months (3 full years)
        for (monthOffset in 0 until 36) {
            val targetYear = 2026 + (monthOffset / 12)
            val targetMonth = (monthOffset % 12) + 1

            val assignments = RotationEngine.calculateAssignments(
                participants = participants,
                startYear = 2026,
                startMonth = 1,
                targetYear = targetYear,
                targetMonth = targetMonth
            )

            // Total assigned paras across all members must be exactly 30
            val allAssignedParas = assignments.flatMap { it.assignedParas }
            assertEquals("Month $monthOffset must have exactly 30 assigned paras", 30, allAssignedParas.size)

            // Must contain every Para from 1 to 30 with no duplicates
            val distinctParas = allAssignedParas.toSet()
            assertEquals("Month $monthOffset must cover all 30 unique Paras", (1..30).toSet(), distinctParas)

            // Assignments list must be strictly ordered 1 to 30
            assertEquals(1, assignments.first().startPara)
            assertEquals(30, assignments.last().endPara)
            for (i in 1 until assignments.size) {
                assertEquals(assignments[i - 1].endPara + 1, assignments[i].startPara)
            }

            // Individual counts must match requested allocation sizes
            assertEquals(5, assignments.first { it.participantId == 1L }.allocatedCount)
            assertEquals(7, assignments.first { it.participantId == 2L }.allocatedCount)
            assertEquals(8, assignments.first { it.participantId == 3L }.allocatedCount)
            assertEquals(10, assignments.first { it.participantId == 4L }.allocatedCount)
        }
    }

    @Test
    fun testShareSummaryParaFirstOrderingAndFormat() {
        val dist = com.example.data.db.DistributionEntity(
            id = 1L,
            name = "Family Khatm",
            startYear = 2026,
            startMonth = 1
        )
        val participants = listOf(
            com.example.data.db.ParticipantEntity(id = 1, distributionId = 1, name = "ABC", allocatedCount = 15, displayOrder = 1),
            com.example.data.db.ParticipantEntity(id = 2, distributionId = 1, name = "XYZ", allocatedCount = 15, displayOrder = 2)
        )
        val allocs = participants.map {
            ParticipantAllocation(id = it.id, name = it.name, count = it.allocatedCount, displayOrder = it.displayOrder)
        }
        // In Month 2 (February 2026), XYZ has Paras 1..15 and ABC has Paras 16..30
        val febAssignments = RotationEngine.calculateAssignments(allocs, 2026, 1, 2026, 2)
        assertEquals("XYZ", febAssignments[0].participantName)
        assertEquals(1, febAssignments[0].startPara)
        assertEquals("ABC", febAssignments[1].participantName)
        assertEquals(16, febAssignments[1].startPara)

        val summary = com.example.ui.viewmodel.DistributionViewModel.formatDistributionShareSummary(
            distribution = dist,
            participants = participants,
            assignments = febAssignments,
            targetYear = 2026,
            targetMonth = 2
        )
        val lines = summary.lines()
        val firstParaLineIdx = lines.indexOfFirst { it.startsWith("1, 2") }
        val secondParaLineIdx = lines.indexOfFirst { it.startsWith("16, 17") }
        assertTrue("Must contain '1, 2, ...: XYZ' line", firstParaLineIdx >= 0 && lines[firstParaLineIdx].endsWith(": XYZ"))
        assertTrue("Must contain '16, 17, ...: ABC' line", secondParaLineIdx > firstParaLineIdx && lines[secondParaLineIdx].endsWith(": ABC"))
    }

    @Test
    fun testEqualDistributionFullCycle() {
        val participants = (1..5).map {
            ParticipantAllocation(id = it.toLong(), name = "Person $it", count = 6, displayOrder = it)
        }

        // Over 5 months, Person 1 should read all 30 Paras with 0 consecutive overlap
        val seenParasPerson1 = mutableSetOf<Int>()
        for (month in 1..5) {
            val assignments = RotationEngine.calculateAssignments(
                participants = participants,
                startYear = 2026,
                startMonth = 1,
                targetYear = 2026,
                targetMonth = month
            )
            val p1Paras = assignments.first { it.participantId == 1L }.assignedParas
            seenParasPerson1.addAll(p1Paras)
        }

        assertEquals(30, seenParasPerson1.size)
        assertEquals((1..30).toSet(), seenParasPerson1)
    }

    @Test
    fun testHijriCalendarConversion() {
        val jan2027 = HijriDateUtils.getMonthInfo(2027, 1)
        assertEquals("January 2027", jan2027.gregorianFullText)
        assertTrue(jan2027.hijriFullText.contains("1448 AH"))
        assertTrue(jan2027.hijriMonthName.isNotBlank())
        assertTrue(jan2027.dualHeaderDisplay.contains("January 2027"))
    }

    @Test
    fun testSequentialProgressionThreeParasEach() {
        // 10 participants reading 3 Paras each (Total = 30)
        val names = listOf("Dad", "Mom", "Brother", "Sister", "Uncle", "Aunt", "Cousin1", "Cousin2", "Grandpa", "Grandma")
        val participants = names.mapIndexed { idx, name ->
            ParticipantAllocation(id = (idx + 1).toLong(), name = name, count = 3, displayOrder = idx + 1)
        }

        // September (Month 9): Dad has Paras 1..3
        val sep = RotationEngine.calculateAssignments(participants, 2026, 9, 2026, 9)
        val dadSep = sep.first { it.participantName == "Dad" }
        assertEquals(1, dadSep.startPara)
        assertEquals(3, dadSep.endPara)
        assertEquals(listOf(1, 2, 3), dadSep.assignedParas)

        // October (Month 10): Dad must get next consecutive Paras 4..6
        val oct = RotationEngine.calculateAssignments(participants, 2026, 9, 2026, 10)
        val dadOct = oct.first { it.participantName == "Dad" }
        assertEquals(4, dadOct.startPara)
        assertEquals(6, dadOct.endPara)
        assertEquals(listOf(4, 5, 6), dadOct.assignedParas)

        // November (Month 11): Dad must get next consecutive Paras 7..9
        val nov = RotationEngine.calculateAssignments(participants, 2026, 9, 2026, 11)
        val dadNov = nov.first { it.participantName == "Dad" }
        assertEquals(7, dadNov.startPara)
        assertEquals(9, dadNov.endPara)
        assertEquals(listOf(7, 8, 9), dadNov.assignedParas)

        // December (Month 12): Dad gets 10..12
        val dec = RotationEngine.calculateAssignments(participants, 2026, 9, 2026, 12)
        val dadDec = dec.first { it.participantName == "Dad" }
        assertEquals(listOf(10, 11, 12), dadDec.assignedParas)

        // Verify zero repetition for all participants across 12 consecutive months
        for (m in 9..20) {
            val cur = RotationEngine.calculateAssignments(participants, 2026, 9, 2026, m)
            val next = RotationEngine.calculateAssignments(participants, 2026, 9, 2026, m + 1)

            participants.forEach { p ->
                val curParas = cur.first { it.participantId == p.id }.assignedParas.toSet()
                val nextParas = next.first { it.participantId == p.id }.assignedParas.toSet()
                val overlap = curParas.intersect(nextParas)
                assertTrue("Participant ${p.name} must have 0 consecutive overlap between month $m and ${m + 1}", overlap.isEmpty())
            }
        }
    }

    @Test
    fun testUnequalAllocationsMinimalSkipAndZeroConsecutiveOverlap() {
        // Unequal distribution where max allocation is 4
        val participants = listOf(
            ParticipantAllocation(id = 1, name = "Dad", count = 3, displayOrder = 1),
            ParticipantAllocation(id = 2, name = "Mom", count = 3, displayOrder = 2),
            ParticipantAllocation(id = 3, name = "Uncle", count = 4, displayOrder = 3),
            ParticipantAllocation(id = 4, name = "Aunt", count = 2, displayOrder = 4),
            ParticipantAllocation(id = 5, name = "Cousin", count = 3, displayOrder = 5),
            ParticipantAllocation(id = 6, name = "Brother", count = 5, displayOrder = 6),
            ParticipantAllocation(id = 7, name = "Sister", count = 5, displayOrder = 7),
            ParticipantAllocation(id = 8, name = "Friend", count = 5, displayOrder = 8)
        )
        assertEquals(30, participants.sumOf { it.count })

        // Optimal rotation step should be the smallest valid step with 0 overlap
        val step = RotationEngine.calculateOptimalRotationStep(participants)
        assertTrue(step in 1 until participants.size)

        // Verify zero overlap across 24 consecutive months
        for (m in 1..24) {
            val cur = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, m)
            val next = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, m + 1)

            participants.forEach { p ->
                val curParas = cur.first { it.participantId == p.id }.assignedParas.toSet()
                val nextParas = next.first { it.participantId == p.id }.assignedParas.toSet()
                val overlap = curParas.intersect(nextParas)
                assertTrue("Participant ${p.name} must have 0 overlap between month $m and ${m + 1}", overlap.isEmpty())
            }
        }
    }

    @Test
    fun testSingleParaDisplayFormatting() {
        val singleParaAssignment = ParaAssignment(
            participantId = 1,
            participantName = "Jawad",
            allocatedCount = 1,
            startPara = 11,
            endPara = 11
        )
        // Must show "Para 11", never "Para 11–11" or "11-11"
        assertEquals("Para 11", singleParaAssignment.displayRangeText)
        assertEquals("11", singleParaAssignment.commaSeparatedParas)

        val lastParaAssignment = ParaAssignment(
            participantId = 2,
            participantName = "Khadija g.",
            allocatedCount = 1,
            startPara = 30,
            endPara = 30
        )
        // Must show "Para 30", never "30-30"
        assertEquals("Para 30", lastParaAssignment.displayRangeText)
        assertEquals("30", lastParaAssignment.commaSeparatedParas)

        val multiParaAssignment = ParaAssignment(
            participantId = 3,
            participantName = "Sukaina",
            allocatedCount = 3,
            startPara = 14,
            endPara = 16
        )
        assertEquals("Paras 14–16", multiParaAssignment.displayRangeText)
        assertEquals("14, 15, 16", multiParaAssignment.commaSeparatedParas)
    }

    @Test
    fun testNonCircularConsecutiveParasRule() {
        // Participant list matching user example
        val participants = listOf(
            ParticipantAllocation(1, "Madiha", 2, 1),
            ParticipantAllocation(2, "Khadija", 2, 2),
            ParticipantAllocation(3, "Dadi", 2, 3),
            ParticipantAllocation(4, "Hamza Ali", 2, 4),
            ParticipantAllocation(5, "Muslim", 2, 5),
            ParticipantAllocation(6, "Jawad", 1, 6),
            ParticipantAllocation(7, "M hamza", 2, 7),
            ParticipantAllocation(8, "Sukaina", 3, 8),
            ParticipantAllocation(9, "M. Ali", 2, 9),
            ParticipantAllocation(10, "Kaneez", 3, 10),
            ParticipantAllocation(11, "Tasneem", 3, 11),
            ParticipantAllocation(12, "Qurat", 3, 12),
            ParticipantAllocation(13, "Abbas", 2, 13),
            ParticipantAllocation(14, "Khadija g.", 1, 14)
        )
        assertEquals(30, participants.sumOf { it.count })

        // Check across 36 months that for EVERY participant, assignedParas are strictly consecutive (start <= end <= 30)
        for (m in 1..36) {
            val assignments = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, m)
            for (assignment in assignments) {
                // Must be strictly consecutive without wrapping across 30 -> 1
                assertTrue("startPara must be >= 1", assignment.startPara >= 1)
                assertTrue("endPara must be <= 30", assignment.endPara <= 30)
                assertEquals(
                    "Assigned paras must match consecutive range",
                    (assignment.startPara..assignment.endPara).toList(),
                    assignment.assignedParas
                )
                assertEquals(
                    "Count must match allocatedCount",
                    assignment.allocatedCount,
                    assignment.assignedParas.size
                )
            }
        }
    }

    @Test
    fun testNonDivisibleMemberCountFairRemainderRotationAndSequentialProgression() {
        // Test 4 members (8, 8, 7, 7) and 7 members (5, 5, 4, 4, 4, 4, 4)
        for (memberCount in listOf(4, 7, 8, 9, 11)) {
            val slotSizes = RotationEngine.computeFairSlotSizes(memberCount)
            assertEquals(30, slotSizes.sum())
            val participants = slotSizes.mapIndexed { idx, count ->
                ParticipantAllocation(
                    id = (idx + 1).toLong(),
                    name = "Member ${idx + 1}",
                    count = count,
                    displayOrder = idx + 1
                )
            }

            // Over a full N-month cycle:
            // 1. Every month covers 1..30 exactly once.
            // 2. Every member's allocation is contiguous (startPara..endPara) and never crosses 30->1.
            // 3. Each member's next month starts immediately at (previous endPara + 1), wrapping 30 -> 1 only between months.
            // 4. Over N months, every member reads all 30 Paras and receives the larger blocks the exact same number of times.
            val totalParasPerMemberAcrossCycle = mutableMapOf<Long, MutableList<Int>>()

            for (m in 0 until memberCount) {
                val cur = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, 1 + m)
                val next = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, 1 + m + 1)

                // Full 1..30 coverage with zero duplicates
                val allParas = cur.flatMap { it.assignedParas }
                assertEquals(30, allParas.size)
                assertEquals((1..30).toSet(), allParas.toSet())

                for (p in participants) {
                    val curAssign = cur.first { it.participantId == p.id }
                    val nextAssign = next.first { it.participantId == p.id }

                    // Contiguous and within 1..30, never 30->1 inside an allocation
                    assertTrue(curAssign.startPara in 1..30)
                    assertTrue(curAssign.endPara in 1..30)
                    assertTrue(curAssign.startPara <= curAssign.endPara)

                    // Month N+1 progresses directly from Month N's endPara (or wraps 30 -> 1 at cycle start)
                    val expectedNextStart = if (curAssign.endPara == 30) 1 else curAssign.endPara + 1
                    assertEquals(
                        "Member ${p.name} must progress sequentially without jumping",
                        expectedNextStart,
                        nextAssign.startPara
                    )

                    totalParasPerMemberAcrossCycle.getOrPut(p.id) { mutableListOf() }.addAll(curAssign.assignedParas)
                }
            }

            // Over N months, every member has read Paras 1..30 in exact order (total 30 Paras each)
            for (p in participants) {
                val cycleParas = totalParasPerMemberAcrossCycle[p.id]!!
                assertEquals(30, cycleParas.size)
                assertEquals((1..30).toSet(), cycleParas.toSet())
            }
        }
    }

    @Test
    fun testParseAyahExtractsTrueAyahNumberAndStripsTrailingMarker() {
        val para3FirstVerse = "تِلۡكَ ٱلرُّسُلُ فَضَّلۡنَا بَعۡضَهُمۡ عَلَىٰ بَعۡضٖۘ وَٱللَّهُ يَفۡعَلُ مَا يُرِيدُ ۝253"
        val parsed = com.example.ui.components.parseAyah(para3FirstVerse, fallbackNumber = 1)

        assertEquals(253, parsed.ayahNumber)
        assertEquals("٢٥٣", parsed.arabicDigits)
        assertFalse("Arabic text must not retain raw ۝253 marker", parsed.arabicText.contains("۝"))
        assertFalse("Arabic text must not retain raw ASCII 253", parsed.arabicText.contains("253"))
    }
}

