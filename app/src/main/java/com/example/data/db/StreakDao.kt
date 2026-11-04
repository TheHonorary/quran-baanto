package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    // --- Streak & User Progress ---
    @Query("SELECT * FROM streaks WHERE id = 1 LIMIT 1")
    fun getStreak(): Flow<StreakEntity?>

    @Query("SELECT * FROM streaks WHERE id = 1 LIMIT 1")
    suspend fun getStreakDirect(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakEntity)

    // --- Daily Logs ---
    @Query("SELECT * FROM daily_logs WHERE dateKey = :dateKey LIMIT 1")
    fun getDailyLog(dateKey: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyLogDirect(dateKey: String): DailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyLog(log: DailyLogEntity)

    @Query("SELECT * FROM daily_logs ORDER BY dateKey DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 30): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY dateKey DESC")
    suspend fun getAllLogsDirect(): List<DailyLogEntity>

    // --- Reading Plans ---
    @Query("SELECT * FROM reading_plans ORDER BY createdTimestamp DESC")
    fun getAllPlans(): Flow<List<ReadingPlanEntity>>

    @Query("SELECT * FROM reading_plans WHERE isActive = 1 AND isCompleted = 0 ORDER BY createdTimestamp DESC")
    fun getActivePlans(): Flow<List<ReadingPlanEntity>>

    @Query("SELECT * FROM reading_plans WHERE id = :id LIMIT 1")
    fun getPlanById(id: Long): Flow<ReadingPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ReadingPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: ReadingPlanEntity)

    @Query("DELETE FROM reading_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    // --- Streak Circles (Group Streak Rooms) ---
    @Query("SELECT * FROM streak_circles ORDER BY createdTimestamp DESC")
    fun getAllCircles(): Flow<List<StreakCircleEntity>>

    @Query("SELECT * FROM streak_circles WHERE id = :id LIMIT 1")
    fun getCircleById(id: Long): Flow<StreakCircleEntity?>

    @Query("SELECT * FROM streak_circles WHERE id = :id LIMIT 1")
    suspend fun getCircleByIdDirect(id: Long): StreakCircleEntity?


    @Query("SELECT * FROM streak_circles WHERE roomPasscode = :code LIMIT 1")
    suspend fun getCircleByPasscode(code: String): StreakCircleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCircle(circle: StreakCircleEntity): Long

    @Update
    suspend fun updateCircle(circle: StreakCircleEntity)

    @Query("DELETE FROM streak_circles WHERE id = :id")
    suspend fun deleteCircleById(id: Long)

    // --- Streak Circle Members ---
    @Query("SELECT * FROM streak_circle_members WHERE circleId = :circleId ORDER BY isHost DESC, memberName ASC")
    fun getMembersForCircle(circleId: Long): Flow<List<StreakCircleMemberEntity>>

    @Query("SELECT * FROM streak_circle_members WHERE circleId = :circleId ORDER BY isHost DESC, memberName ASC")
    suspend fun getMembersForCircleDirect(circleId: Long): List<StreakCircleMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: StreakCircleMemberEntity): Long

    @Update
    suspend fun updateMember(member: StreakCircleMemberEntity)

    @Query("DELETE FROM streak_circle_members WHERE id = :id")
    suspend fun deleteMemberById(id: Long)
}
