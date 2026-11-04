package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlanType {
    DAILY_AYAH_GOAL,
    PARA_RANGE_TARGET,
    CUSTOM_SURAH_TARGET
}

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey val id: Long = 1L,
    val isOptedIn: Boolean = false,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastCompletedDate: String = "", // YYYY-MM-DD
    val consecutiveMissedDays: Int = 0,
    val lastFreezeShieldUsedDate: String = "", // YYYY-MM-DD to enforce 7-day cooldown
    val availableFreezeShields: Int = 0, // Max 1 in reserve
    val pendingAdViewsForNextShield: Int = 0, // 0..3 (3 views grant 1 shield)
    val lastReadSurah: Int? = null,
    val lastReadAyah: Int? = null,
    val lastReadPara: Int? = null,
    val lastReadSurahName: String? = null,
    val lastReadTimestamp: Long = 0L
)

@Entity(tableName = "reading_plans")
data class ReadingPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val planType: PlanType = PlanType.DAILY_AYAH_GOAL,
    val dailyTargetAyahs: Int = 10,
    val targetParasCsv: String = "",
    val targetSurahNumber: Int? = null,
    val targetCompletionDate: String = "",
    val startDate: String = "",
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val completedDaysCount: Int = 0,
    val totalDaysCount: Int = 30,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val ayahsReadCount: Int = 0,
    val targetAyahsCount: Int = 10,
    val hasMetGoal: Boolean = false,
    val freezeShieldUsed: Boolean = false,
    val completedTimestamp: Long? = null
)

@Entity(tableName = "streak_circles")
data class StreakCircleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roomPasscode: String,
    val isOnline: Boolean = true,
    val collectiveStreakDays: Int = 0,
    val lastEvaluatedDate: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "streak_circle_members",
    foreignKeys = [
        ForeignKey(
            entity = StreakCircleEntity::class,
            parentColumns = ["id"],
            childColumns = ["circleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("circleId")]
)
data class StreakCircleMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val circleId: Long,
    val memberName: String,
    val currentStreakDays: Int = 0,
    val hasCompletedToday: Boolean = false,
    val lastCompletedDate: String = "",
    val isHost: Boolean = false
)
