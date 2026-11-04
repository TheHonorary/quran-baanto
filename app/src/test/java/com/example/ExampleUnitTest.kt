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

    @Test
    fun testLocalOneTimeKhatmAll30ParasClaimedSummary() {
        val dist = com.example.data.db.DistributionEntity(
            id = 10L,
            name = "Local One-Time Khatm",
            type = com.example.data.db.DistributionType.ONE_TIME_KHATM,
            roomMode = com.example.data.db.RoomMode.LOCAL,
            startYear = 2026,
            startMonth = 10
        )
        // 30 participants claiming Paras 1..30
        val participants = (1..30).map { para ->
            com.example.data.db.ParticipantEntity(
                id = para.toLong(),
                distributionId = 10L,
                name = "Member $para",
                allocatedCount = 1,
                displayOrder = para,
                specificParaNumber = para,
                isCompleted = para <= 15 // First 15 are completed
            )
        }

        val summary = com.example.ui.viewmodel.DistributionViewModel.formatDistributionShareSummary(
            distribution = dist,
            participants = participants,
            assignments = emptyList(),
            targetYear = 2026,
            targetMonth = 10
        )

        assertTrue(summary.contains("Local One-Time Khatm"))
        assertTrue(summary.contains("Member 1:") && summary.contains("1 ✓"))
        assertTrue(summary.contains("Member 15:") && summary.contains("15 ✓"))
        assertTrue(summary.contains("Member 16:") && summary.contains("16") && !summary.contains("16 ✓"))
        assertFalse("All 30 paras are taken so Available Paras section should not appear", summary.contains("Available Paras"))
    }

    @Test
    fun testAmalProgressCounterAndCompletionThreshold() {
        val participant = com.example.data.db.ParticipantEntity(
            id = 1L,
            distributionId = 20L,
            name = "Ali",
            allocatedCount = 100,
            displayOrder = 1,
            completedCount = 0,
            isCompleted = false
        )

        // Progress counter incrementing
        val step1 = participant.copy(completedCount = 50)
        assertFalse(step1.isCompleted)
        assertEquals(50, step1.completedCount)

        // Progress reaching threshold
        val step2 = step1.copy(completedCount = 100, isCompleted = true)
        assertTrue(step2.isCompleted)
        assertEquals(100, step2.completedCount)

        // Progress clamped beyond target
        val step3 = step2.copy(completedCount = 100.coerceAtMost(100))
        assertEquals(100, step3.completedCount)
    }

    @Test
    fun testPinningToggleAndState() {
        val participant = com.example.data.db.ParticipantEntity(
            id = 7L,
            distributionId = 30L,
            name = "Zaid",
            allocatedCount = 3,
            displayOrder = 1,
            isPinned = false
        )
        assertFalse(participant.isPinned)

        val pinned = participant.copy(isPinned = !participant.isPinned)
        assertTrue(pinned.isPinned)

        val unpinned = pinned.copy(isPinned = !pinned.isPinned)
        assertFalse(unpinned.isPinned)
    }

    @Test
    fun testAmalShareSummaryOnlineAndLocal() {
        val onlineAmal = com.example.data.db.DistributionEntity(
            id = 40L,
            name = "Durood Shareef 1000",
            type = com.example.data.db.DistributionType.AMAL,
            roomMode = com.example.data.db.RoomMode.ONLINE,
            roomPasscode = "DUROOD-100",
            totalTarget = 1000,
            startYear = 2026,
            startMonth = 10,
            roundNumber = 1
        )
        val participants = listOf(
            com.example.data.db.ParticipantEntity(
                id = 1L,
                distributionId = 40L,
                name = "Fatima",
                allocatedCount = 500,
                completedCount = 500,
                isCompleted = true,
                displayOrder = 1
            ),
            com.example.data.db.ParticipantEntity(
                id = 2L,
                distributionId = 40L,
                name = "Hassan",
                allocatedCount = 500,
                completedCount = 250,
                isCompleted = false,
                displayOrder = 2
            )
        )

        val summary = com.example.ui.viewmodel.DistributionViewModel.formatDistributionShareSummary(
            distribution = onlineAmal,
            participants = participants,
            assignments = emptyList(),
            targetYear = 2026,
            targetMonth = 10
        )

        assertTrue(summary.contains("Durood Shareef 1000 (Target: 1000)"))
        assertTrue(summary.contains("Room Passcode: DUROOD-100"))
        assertTrue(summary.contains("Fatima:") && summary.contains("500 ✓"))
        assertTrue(summary.contains("Hassan:") && summary.contains("500 (250/500)"))
    }

    @Test
    fun testAllParasBoundaries1To30() {
        // Ensure that any Para from 1 to 30 maps to valid contiguous ranges and zero index errors
        for (para in 1..30) {
            val single = RotationEngine.calculateAssignments(
                participants = listOf(ParticipantAllocation(id = 1L, name = "Solo", count = 30, displayOrder = 1)),
                startYear = 2026,
                startMonth = 1,
                targetYear = 2026,
                targetMonth = 1
            )
            assertEquals(1, single.first().startPara)
            assertEquals(30, single.first().endPara)
            assertTrue(single.first().assignedParas.contains(para))
        }
    }

    @Test
    fun testStreakFreezeShieldCooldownEnforcement() {
        // Today
        val today = java.time.LocalDate.now()
        val threeDaysAgo = today.minusDays(3).toString()
        val eightDaysAgo = today.minusDays(8).toString()

        // Dummy DAO that won't be called for pure date logic
        val mockDao = object : com.example.data.db.StreakDao {
            override fun getStreak(): kotlinx.coroutines.flow.Flow<com.example.data.db.StreakEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getStreakDirect(): com.example.data.db.StreakEntity? = null
            override suspend fun insertOrUpdateStreak(streak: com.example.data.db.StreakEntity) {}
            override fun getDailyLog(dateKey: String): kotlinx.coroutines.flow.Flow<com.example.data.db.DailyLogEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getDailyLogDirect(dateKey: String): com.example.data.db.DailyLogEntity? = null
            override suspend fun insertOrUpdateDailyLog(log: com.example.data.db.DailyLogEntity) {}
            override fun getRecentLogs(limit: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.db.DailyLogEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getAllLogsDirect(): List<com.example.data.db.DailyLogEntity> = emptyList()
            override fun getActivePlans(): kotlinx.coroutines.flow.Flow<List<com.example.data.db.ReadingPlanEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getAllPlans(): kotlinx.coroutines.flow.Flow<List<com.example.data.db.ReadingPlanEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getPlanById(id: Long): kotlinx.coroutines.flow.Flow<com.example.data.db.ReadingPlanEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun insertPlan(plan: com.example.data.db.ReadingPlanEntity): Long = 1L
            override suspend fun updatePlan(plan: com.example.data.db.ReadingPlanEntity) {}
            override suspend fun deletePlanById(id: Long) {}
            override fun getAllCircles(): kotlinx.coroutines.flow.Flow<List<com.example.data.db.StreakCircleEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getCircleById(id: Long): kotlinx.coroutines.flow.Flow<com.example.data.db.StreakCircleEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getCircleByIdDirect(id: Long): com.example.data.db.StreakCircleEntity? = null
            override suspend fun getCircleByPasscode(code: String): com.example.data.db.StreakCircleEntity? = null
            override suspend fun insertCircle(circle: com.example.data.db.StreakCircleEntity): Long = 1L
            override suspend fun updateCircle(circle: com.example.data.db.StreakCircleEntity) {}
            override suspend fun deleteCircleById(id: Long) {}
            override fun getMembersForCircle(circleId: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.db.StreakCircleMemberEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getMembersForCircleDirect(circleId: Long): List<com.example.data.db.StreakCircleMemberEntity> = emptyList()
            override suspend fun insertMember(member: com.example.data.db.StreakCircleMemberEntity): Long = 1L
            override suspend fun updateMember(member: com.example.data.db.StreakCircleMemberEntity) {}
            override suspend fun deleteMemberById(id: Long) {}
        }
        val repo = com.example.data.StreakRepository(mockDao)

        // Empty last-used date -> cooldown is NOT active (shield can be used)
        assertTrue(repo.canUseFreezeShieldCooldown(""))
        assertEquals(0L, repo.getDaysUntilNextFreezeShieldAvailable(""))

        // Used 3 days ago -> cooldown IS active (cannot use shield yet, 4 days remaining)
        assertFalse(repo.canUseFreezeShieldCooldown(threeDaysAgo))
        assertEquals(4L, repo.getDaysUntilNextFreezeShieldAvailable(threeDaysAgo))

        // Used 8 days ago -> cooldown expired (can use shield again)
        assertTrue(repo.canUseFreezeShieldCooldown(eightDaysAgo))
        assertEquals(0L, repo.getDaysUntilNextFreezeShieldAvailable(eightDaysAgo))
    }

    @Test
    fun testStreakCircleShareTextFormatting() {
        val mockDao = object : com.example.data.db.StreakDao {
            override fun getStreak(): kotlinx.coroutines.flow.Flow<com.example.data.db.StreakEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getStreakDirect(): com.example.data.db.StreakEntity? = null
            override suspend fun insertOrUpdateStreak(streak: com.example.data.db.StreakEntity) {}
            override fun getDailyLog(dateKey: String): kotlinx.coroutines.flow.Flow<com.example.data.db.DailyLogEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getDailyLogDirect(dateKey: String): com.example.data.db.DailyLogEntity? = null
            override suspend fun insertOrUpdateDailyLog(log: com.example.data.db.DailyLogEntity) {}
            override fun getRecentLogs(limit: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.db.DailyLogEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getAllLogsDirect(): List<com.example.data.db.DailyLogEntity> = emptyList()
            override fun getActivePlans(): kotlinx.coroutines.flow.Flow<List<com.example.data.db.ReadingPlanEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getAllPlans(): kotlinx.coroutines.flow.Flow<List<com.example.data.db.ReadingPlanEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getPlanById(id: Long): kotlinx.coroutines.flow.Flow<com.example.data.db.ReadingPlanEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun insertPlan(plan: com.example.data.db.ReadingPlanEntity): Long = 1L
            override suspend fun updatePlan(plan: com.example.data.db.ReadingPlanEntity) {}
            override suspend fun deletePlanById(id: Long) {}
            override fun getAllCircles(): kotlinx.coroutines.flow.Flow<List<com.example.data.db.StreakCircleEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getCircleById(id: Long): kotlinx.coroutines.flow.Flow<com.example.data.db.StreakCircleEntity?> = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getCircleByIdDirect(id: Long): com.example.data.db.StreakCircleEntity? = null
            override suspend fun getCircleByPasscode(code: String): com.example.data.db.StreakCircleEntity? = null
            override suspend fun insertCircle(circle: com.example.data.db.StreakCircleEntity): Long = 1L
            override suspend fun updateCircle(circle: com.example.data.db.StreakCircleEntity) {}
            override suspend fun deleteCircleById(id: Long) {}
            override fun getMembersForCircle(circleId: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.db.StreakCircleMemberEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getMembersForCircleDirect(circleId: Long): List<com.example.data.db.StreakCircleMemberEntity> = emptyList()
            override suspend fun insertMember(member: com.example.data.db.StreakCircleMemberEntity): Long = 1L
            override suspend fun updateMember(member: com.example.data.db.StreakCircleMemberEntity) {}
            override suspend fun deleteMemberById(id: Long) {}
        }
        val repo = com.example.data.StreakRepository(mockDao)

        val circle = com.example.data.db.StreakCircleEntity(
            id = 5L,
            name = "Fajr Noor Circle",
            roomPasscode = "STRK-7788",
            collectiveStreakDays = 14
        )
        val members = listOf(
            com.example.data.db.StreakCircleMemberEntity(
                id = 1L,
                circleId = 5L,
                memberName = "Zaid",
                currentStreakDays = 14,
                hasCompletedToday = true,
                isHost = true
            ),
            com.example.data.db.StreakCircleMemberEntity(
                id = 2L,
                circleId = 5L,
                memberName = "Bilal",
                currentStreakDays = 12,
                hasCompletedToday = false,
                isHost = false
            )
        )

        val text = repo.formatStreakCircleShareText(circle, members)
        assertTrue(text.contains("Fajr Noor Circle"))
        assertTrue(text.contains("14-Day Collective Streak"))
        assertTrue(text.contains("Room Passcode: STRK-7788"))
        assertTrue(text.contains("Zaid (Host): 14 Days (✓ Recited today)"))
        assertTrue(text.contains("Bilal: 12 Days (⏳ Pending today)"))
    }
}


