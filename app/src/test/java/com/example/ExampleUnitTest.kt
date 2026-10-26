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

            // All 30 Paras must be partitioned cleanly across the group
            assertEquals(4, assignments.size)
            assertTrue(assignments.any { it.assignedParas.contains(1) })
            assertTrue(assignments.any { it.assignedParas.contains(30) })

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
        val firstParaLineIdx = lines.indexOfFirst { it.startsWith("XYZ:") }
        val secondParaLineIdx = lines.indexOfFirst { it.startsWith("ABC:") }
        assertTrue("Must contain 'XYZ:          1, 2, ...' line first", firstParaLineIdx >= 0 && lines[firstParaLineIdx].contains("1, 2"))
        assertTrue("Must contain 'ABC:          16, 17, ...' line second", secondParaLineIdx > firstParaLineIdx && lines[secondParaLineIdx].contains("16, 17"))
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
    fun testCircularQueueRotationAndWrappingRule() {
        // Participant list matching user example
        val participants = listOf(
            ParticipantAllocation(1, "Abbas", 2, 1),
            ParticipantAllocation(2, "Khadija g.", 1, 2),
            ParticipantAllocation(3, "Madiha", 2, 3),
            ParticipantAllocation(4, "Khadija", 2, 4),
            ParticipantAllocation(5, "Dadi", 2, 5),
            ParticipantAllocation(6, "Hamza Ali", 2, 6),
            ParticipantAllocation(7, "Muslim", 2, 7),
            ParticipantAllocation(8, "Jawad", 1, 8),
            ParticipantAllocation(9, "M hamza", 2, 9),
            ParticipantAllocation(10, "Sukaina", 3, 10),
            ParticipantAllocation(11, "M. Ali", 2, 11),
            ParticipantAllocation(12, "Kaneez", 3, 12),
            ParticipantAllocation(13, "Tasneem", 3, 13),
            ParticipantAllocation(14, "Qurat", 3, 14)
        )
        assertEquals(30, participants.sumOf { it.count })

        // In Month 1 (m = 1):
        val m1 = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, 1)
        val abbasM1 = m1.first { it.participantName == "Abbas" }
        assertEquals(listOf(1, 2), abbasM1.assignedParas)
        val quratM1 = m1.first { it.participantName == "Qurat" }
        assertEquals(listOf(28, 29, 30), quratM1.assignedParas)

        // In Month 2 (m = 2):
        // Tasneem rotates to front and receives [1, 2, 3]
        // Qurat follows with [4, 5, 6]
        // Abbas follows with [7, 8]
        val m2 = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, 2)
        val tasneemM2 = m2.first { it.participantName == "Tasneem" }
        assertEquals(listOf(1, 2, 3), tasneemM2.assignedParas)
        val quratM2 = m2.first { it.participantName == "Qurat" }
        assertEquals(listOf(4, 5, 6), quratM2.assignedParas)
        val abbasM2 = m2.first { it.participantName == "Abbas" }
        assertEquals(listOf(7, 8), abbasM2.assignedParas)

        // Check across 36 months that for EVERY month:
        // 1. All 30 Paras 1..30 are accounted for exactly once (no duplicates, no gaps).
        // 2. Each participant's assignedParas.size == allocatedCount.
        // 3. No repetition between consecutive months for each participant.
        for (m in 1..36) {
            val cur = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, m)
            val next = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, m + 1)

            val allParas = cur.flatMap { it.assignedParas }
            assertEquals(30, allParas.size)
            assertEquals((1..30).toSet(), allParas.toSet())

            for (assignment in cur) {
                assertTrue("startPara must be in 1..30", assignment.startPara in 1..30)
                assertTrue("endPara must be in 1..30", assignment.endPara in 1..30)
                assertEquals(
                    "Count must match allocatedCount",
                    assignment.allocatedCount,
                    assignment.assignedParas.size
                )
            }

            for (p in participants) {
                val curSet = cur.first { it.participantId == p.id }.assignedParas.toSet()
                val nextSet = next.first { it.participantId == p.id }.assignedParas.toSet()
                val overlap = curSet.intersect(nextSet)
                if (overlap.isNotEmpty()) {
                    println("DEBUG OVERLAP: month $m -> ${m+1}, participant ${p.name}, count ${p.count}, curSet $curSet, nextSet $nextSet, overlap $overlap")
                }
                assertTrue("No repetition for ${p.name} between month $m and ${m + 1} (cur=$curSet, next=$nextSet)", overlap.isEmpty())
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
            // 2. Every member's count is preserved.
            // 3. Wrapping across 30 -> 1 occurs seamlessly without jumping.
            // 4. Zero repetition between consecutive months.
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

                    assertTrue(curAssign.startPara in 1..30)
                    assertTrue(curAssign.endPara in 1..30)
                    assertEquals(p.count, curAssign.assignedParas.size)

                    val overlap = curAssign.assignedParas.toSet().intersect(nextAssign.assignedParas.toSet())
                    assertTrue("No overlap between consecutive months for member ${p.name}", overlap.isEmpty())
                }
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

    @Test
    fun testV3MonthlyCompletionTrackingPerMonth() {
        val p = com.example.data.db.ParticipantEntity(
            id = 10L,
            distributionId = 1L,
            name = "Hamza",
            allocatedCount = 2,
            displayOrder = 1
        )
        assertFalse(p.isCompletedForMonth(2026, 9))

        val completedSep = p.withToggledMonthCompletion(2026, 9)
        assertTrue(completedSep.isCompletedForMonth(2026, 9))
        assertFalse(completedSep.isCompletedForMonth(2026, 10))

        val uncompletedSep = completedSep.withToggledMonthCompletion(2026, 9)
        assertFalse(uncompletedSep.isCompletedForMonth(2026, 9))
    }

    @Test
    fun testV3OneTimeOnlineKhatmShareSummaryWithPasscodeAndGuests() {
        val dist = com.example.data.db.DistributionEntity(
            id = 5L,
            name = "Grandma Esal-e-Sawab Khatm",
            type = com.example.data.db.DistributionType.ONE_TIME_KHATM,
            roomMode = com.example.data.db.RoomMode.ONLINE,
            roomPasscode = "QBO-7482",
            startYear = 2026,
            startMonth = 9
        )
        val claims = listOf(
            com.example.data.db.ParticipantEntity(
                id = 1L,
                distributionId = 5L,
                name = "Hamza",
                allocatedCount = 1,
                displayOrder = 1,
                specificParaNumber = 1,
                isCompleted = true
            ),
            com.example.data.db.ParticipantEntity(
                id = 2L,
                distributionId = 5L,
                name = "Dadi Jaan",
                allocatedCount = 1,
                displayOrder = 2,
                specificParaNumber = 2,
                isGuestEntry = true,
                isCompleted = false
            )
        )

        val summary = com.example.ui.viewmodel.DistributionViewModel.formatDistributionShareSummary(
            distribution = dist,
            participants = claims,
            assignments = emptyList(),
            targetYear = 2026,
            targetMonth = 9
        )

        assertTrue(summary.contains("Room Passcode: QBO-7482"))
        assertTrue(summary.contains("Hamza:") && summary.contains("1 ✓"))
        assertTrue(summary.contains("Dadi Jaan (Guest):") && summary.contains("2"))
        assertTrue(summary.contains("Available Paras (28):"))
    }
}

