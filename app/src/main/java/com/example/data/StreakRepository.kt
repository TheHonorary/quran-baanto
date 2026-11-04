package com.example.data

import com.example.data.db.DailyLogEntity
import com.example.data.db.PlanType
import com.example.data.db.ReadingPlanEntity
import com.example.data.db.StreakCircleEntity
import com.example.data.db.StreakCircleMemberEntity
import com.example.data.db.StreakDao
import com.example.data.db.StreakEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

sealed interface FreezeShieldAdResult {
    data class Progress(val adsWatched: Int, val adsNeeded: Int = 3) : FreezeShieldAdResult
    data object ShieldEarned : FreezeShieldAdResult
    data class CooldownActive(val daysRemaining: Long) : FreezeShieldAdResult
}

class StreakRepository(private val dao: StreakDao) {

    val streakFlow: Flow<StreakEntity?> = dao.getStreak()
    val activePlansFlow: Flow<List<ReadingPlanEntity>> = dao.getActivePlans()
    val allPlansFlow: Flow<List<ReadingPlanEntity>> = dao.getAllPlans()
    val streakCirclesFlow: Flow<List<StreakCircleEntity>> = dao.getAllCircles()
    val recentDailyLogsFlow: Flow<List<DailyLogEntity>> = dao.getRecentLogs(30)

    fun getTodayKey(): String = LocalDate.now().toString()
    fun getYesterdayKey(): String = LocalDate.now().minusDays(1).toString()

    suspend fun getOrCreateStreak(): StreakEntity {
        val existing = dao.getStreakDirect()
        if (existing != null) return existing
        val defaultStreak = StreakEntity(id = 1L)
        dao.insertOrUpdateStreak(defaultStreak)
        return defaultStreak
    }

    /**
     * Opts the user into the daily streak and habit journey.
     * Starts with today's daily log initialized.
     */
    suspend fun optInToStreak(dailyTargetAyahs: Int = 10): StreakEntity {
        val current = getOrCreateStreak()
        val today = getTodayKey()
        val updated = current.copy(
            isOptedIn = true,
            consecutiveMissedDays = 0
        )
        dao.insertOrUpdateStreak(updated)

        val todayLog = dao.getDailyLogDirect(today)
        if (todayLog == null) {
            dao.insertOrUpdateDailyLog(
                DailyLogEntity(
                    dateKey = today,
                    ayahsReadCount = 0,
                    targetAyahsCount = dailyTargetAyahs
                )
            )
        }
        return updated
    }

    /**
     * Evaluates daily streak continuity and rollover.
     * Enforces the 2-day death rule:
     * - If missed 1 day and has a freeze shield not used within 7 days, shield can protect.
     * - If missed 2 or more consecutive days, the streak is dead and resets to 0.
     */
    suspend fun evaluateStreakRollover(): StreakEntity {
        val streak = getOrCreateStreak()
        if (!streak.isOptedIn) return streak
        val today = LocalDate.now()
        val lastDateStr = streak.lastCompletedDate

        if (lastDateStr.isBlank()) {
            return streak
        }

        val lastDate = try {
            LocalDate.parse(lastDateStr)
        } catch (_: Exception) {
            return streak
        }

        val daysBetween = ChronoUnit.DAYS.between(lastDate, today)

        if (daysBetween <= 1) {
            // Either completed today (0) or completed yesterday (1) -> streak is alive and healthy!
            return streak
        }

        // Missed days evaluation
        if (daysBetween == 2L) {
            // Exactly 1 day was missed (yesterday was missed)
            // Check if user has an available freeze shield and cooldown (7 days) has passed
            val canUseShield = streak.availableFreezeShields > 0 && canUseFreezeShieldCooldown(streak.lastFreezeShieldUsedDate)
            if (canUseShield) {
                // Auto-apply or mark shield used to preserve streak
                val updated = streak.copy(
                    availableFreezeShields = (streak.availableFreezeShields - 1).coerceAtLeast(0),
                    lastFreezeShieldUsedDate = getYesterdayKey(),
                    consecutiveMissedDays = 1
                )
                dao.insertOrUpdateStreak(updated)
                dao.insertOrUpdateDailyLog(
                    DailyLogEntity(
                        dateKey = getYesterdayKey(),
                        hasMetGoal = true,
                        freezeShieldUsed = true,
                        completedTimestamp = System.currentTimeMillis()
                    )
                )
                return updated
            } else {
                val updated = streak.copy(consecutiveMissedDays = 1)
                dao.insertOrUpdateStreak(updated)
                return updated
            }
        }

        // Missed 2 or more consecutive days -> STREAK IS DEAD!
        val resetStreak = streak.copy(
            currentStreakDays = 0,
            consecutiveMissedDays = daysBetween.toInt()
        )
        dao.insertOrUpdateStreak(resetStreak)
        return resetStreak
    }

    fun canUseFreezeShieldCooldown(lastUsedDateStr: String): Boolean {
        if (lastUsedDateStr.isBlank()) return true
        val lastUsed = try {
            LocalDate.parse(lastUsedDateStr)
        } catch (_: Exception) {
            return true
        }
        val daysSinceLastUse = ChronoUnit.DAYS.between(lastUsed, LocalDate.now())
        return daysSinceLastUse >= 7
    }

    fun getDaysUntilNextFreezeShieldAvailable(lastUsedDateStr: String): Long {
        if (lastUsedDateStr.isBlank()) return 0
        val lastUsed = try {
            LocalDate.parse(lastUsedDateStr)
        } catch (_: Exception) {
            return 0
        }
        val daysSinceLastUse = ChronoUnit.DAYS.between(lastUsed, LocalDate.now())
        return (7 - daysSinceLastUse).coerceAtLeast(0)
    }

    /**
     * Records an ad watched towards earning a Streak Freeze Shield.
     * Enforces: Max 1 shield in inventory, max 1 shield earned/used per 7 days.
     */
    suspend fun recordAdWatchedForFreezeShield(): FreezeShieldAdResult {
        val streak = getOrCreateStreak()
        val cooldownDays = getDaysUntilNextFreezeShieldAvailable(streak.lastFreezeShieldUsedDate)
        if (cooldownDays > 0) {
            return FreezeShieldAdResult.CooldownActive(cooldownDays)
        }

        if (streak.availableFreezeShields >= 1) {
            return FreezeShieldAdResult.ShieldEarned
        }

        val nextCount = streak.pendingAdViewsForNextShield + 1
        return if (nextCount >= 3) {
            val updated = streak.copy(
                availableFreezeShields = 1,
                pendingAdViewsForNextShield = 0
            )
            dao.insertOrUpdateStreak(updated)
            FreezeShieldAdResult.ShieldEarned
        } else {
            val updated = streak.copy(pendingAdViewsForNextShield = nextCount)
            dao.insertOrUpdateStreak(updated)
            FreezeShieldAdResult.Progress(adsWatched = nextCount, adsNeeded = 3)
        }
    }

    /**
     * Automatically called from QuranReaderScreen as the user recites Ayahs.
     * Tracks daily progress, persists last bookmark position, and checks plan goals.
     */
    suspend fun recordAyahRecitation(
        ayahsIncrement: Int = 1,
        surahNumber: Int,
        ayahNumber: Int,
        paraNumber: Int,
        surahName: String
    ): Boolean {
        val today = getTodayKey()
        val streak = getOrCreateStreak()

        // Persist last read position on streak
        val updatedStreakPosition = streak.copy(
            lastReadSurah = surahNumber,
            lastReadAyah = ayahNumber,
            lastReadPara = paraNumber,
            lastReadSurahName = surahName,
            lastReadTimestamp = System.currentTimeMillis()
        )
        dao.insertOrUpdateStreak(updatedStreakPosition)

        if (!streak.isOptedIn) {
            return false
        }

        val currentLog = dao.getDailyLogDirect(today) ?: DailyLogEntity(
            dateKey = today,
            ayahsReadCount = 0,
            targetAyahsCount = 10
        )

        val newCount = currentLog.ayahsReadCount + ayahsIncrement
        val wasAlreadyMet = currentLog.hasMetGoal
        val isNowMet = newCount >= currentLog.targetAyahsCount

        val updatedLog = currentLog.copy(
            ayahsReadCount = newCount,
            hasMetGoal = isNowMet || wasAlreadyMet,
            completedTimestamp = if (!wasAlreadyMet && isNowMet) System.currentTimeMillis() else currentLog.completedTimestamp
        )
        dao.insertOrUpdateDailyLog(updatedLog)

        var justAchievedGoal = false
        if (!wasAlreadyMet && isNowMet) {
            justAchievedGoal = true
            val nextStreak = streak.currentStreakDays + 1
            val maxStreak = nextStreak.coerceAtLeast(streak.longestStreakDays)
            val updatedStreak = updatedStreakPosition.copy(
                currentStreakDays = nextStreak,
                longestStreakDays = maxStreak,
                lastCompletedDate = today,
                consecutiveMissedDays = 0
            )
            dao.insertOrUpdateStreak(updatedStreak)

            // Update active reading plans
            val activePlans = dao.getActivePlans().firstOrNull().orEmpty()
            for (plan in activePlans) {
                val nextDays = plan.completedDaysCount + 1
                val isPlanDone = nextDays >= plan.totalDaysCount
                dao.updatePlan(
                    plan.copy(
                        completedDaysCount = nextDays,
                        isCompleted = isPlanDone
                    )
                )
            }

            // Update Streak Circles
            val allCircles = dao.getAllCircles().firstOrNull().orEmpty()
            for (circle in allCircles) {
                val members = dao.getMembersForCircleDirect(circle.id)
                val userMember = members.firstOrNull { it.isHost }
                if (userMember != null) {
                    dao.updateMember(
                        userMember.copy(
                            hasCompletedToday = true,
                            currentStreakDays = nextStreak,
                            lastCompletedDate = today
                        )
                    )
                }
            }
        }

        return justAchievedGoal
    }

    // --- Reading Plans ---
    suspend fun createReadingPlan(
        title: String,
        planType: PlanType,
        dailyTargetAyahs: Int,
        totalDays: Int,
        targetParasCsv: String = "",
        targetSurahNumber: Int? = null
    ): Long {
        val today = getTodayKey()
        val plan = ReadingPlanEntity(
            title = title.trim(),
            planType = planType,
            dailyTargetAyahs = dailyTargetAyahs,
            targetParasCsv = targetParasCsv,
            targetSurahNumber = targetSurahNumber,
            startDate = today,
            targetCompletionDate = LocalDate.now().plusDays(totalDays.toLong()).toString(),
            totalDaysCount = totalDays,
            completedDaysCount = 0,
            isActive = true
        )
        val planId = dao.insertPlan(plan)
        optInToStreak(dailyTargetAyahs = dailyTargetAyahs)
        return planId
    }

    // --- Streak Circles (Group Streak Rooms) ---
    suspend fun createStreakCircle(name: String, hostName: String): Long {
        val passcode = "CRCL-${Random.nextInt(1000, 9999)}"
        val circle = StreakCircleEntity(
            name = name.trim(),
            roomPasscode = passcode,
            isOnline = true,
            collectiveStreakDays = 1,
            lastEvaluatedDate = getTodayKey()
        )
        val circleId = dao.insertCircle(circle)
        dao.insertMember(
            StreakCircleMemberEntity(
                circleId = circleId,
                memberName = hostName.trim().ifBlank { "You" },
                currentStreakDays = 1,
                hasCompletedToday = false,
                isHost = true
            )
        )
        return circleId
    }

    suspend fun joinStreakCircle(passcode: String, memberName: String): Long? {
        val cleanCode = passcode.trim().uppercase()
        val existing = dao.getCircleByPasscode(cleanCode)
        val circleId = existing?.id ?: run {
            val newCircle = StreakCircleEntity(
                name = "Qur'an Recitation Circle",
                roomPasscode = cleanCode,
                isOnline = true
            )
            dao.insertCircle(newCircle)
        }

        val members = dao.getMembersForCircleDirect(circleId)
        val alreadyJoined = members.any { it.memberName.equals(memberName.trim(), ignoreCase = true) }
        if (!alreadyJoined) {
            dao.insertMember(
                StreakCircleMemberEntity(
                    circleId = circleId,
                    memberName = memberName.trim(),
                    currentStreakDays = 1,
                    hasCompletedToday = false,
                    isHost = false
                )
            )
        }
        return circleId
    }

    fun getMembersForCircle(circleId: Long): Flow<List<StreakCircleMemberEntity>> =
        dao.getMembersForCircle(circleId)

    fun formatStreakCircleShareText(
        circle: StreakCircleEntity,
        members: List<StreakCircleMemberEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("${circle.name} 🔥 ${circle.collectiveStreakDays}-Day Collective Streak\n")
        sb.append("Room Passcode: ${circle.roomPasscode}\n\n")
        members.forEach { m ->
            val hostTag = if (m.isHost) " (Host)" else ""
            val status = if (m.hasCompletedToday) "✓ Recited today" else "⏳ Pending today"
            sb.append("• ${m.memberName}$hostTag: ${m.currentStreakDays} Days ($status)\n")
        }
        sb.append("\nJoin our daily recitation streak on Quran Baanto using Room Passcode: ${circle.roomPasscode}")
        return sb.toString().trim()
    }
}
