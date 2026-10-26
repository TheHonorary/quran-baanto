package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DistributionDao {

    @Query("SELECT * FROM distributions ORDER BY createdTimestamp DESC")
    fun getAllDistributions(): Flow<List<DistributionEntity>>

    @Query("SELECT * FROM distributions WHERE roomMode = 'ONLINE'")
    suspend fun getAllOnlineDistributionsList(): List<DistributionEntity>

    @Query("SELECT * FROM distributions WHERE roomMode = 'ONLINE' AND hasPendingSync = 1")
    suspend fun getPendingSyncDistributionsList(): List<DistributionEntity>

    @Query("UPDATE distributions SET hasPendingSync = :pending, lastSyncedTimestamp = :syncedAt WHERE id = :id")
    suspend fun updateDistributionSyncState(id: Long, pending: Boolean, syncedAt: Long)

    @Query("UPDATE participants SET hasPendingSync = :pending WHERE distributionId = :distributionId")
    suspend fun updateAllParticipantsSyncState(distributionId: Long, pending: Boolean)

    @Query("UPDATE participants SET hasPendingSync = :pending WHERE id = :participantId")
    suspend fun updateParticipantSyncState(participantId: Long, pending: Boolean)

    @Query("SELECT * FROM distributions WHERE id = :id")
    fun getDistributionByIdFlow(id: Long): Flow<DistributionEntity?>

    @Query("SELECT * FROM distributions WHERE id = :id")
    suspend fun getDistributionById(id: Long): DistributionEntity?

    @Query("SELECT * FROM distributions WHERE UPPER(roomPasscode) = UPPER(:passcode) LIMIT 1")
    suspend fun getDistributionByPasscode(passcode: String): DistributionEntity?

    @Query("SELECT * FROM participants WHERE distributionId = :distributionId ORDER BY displayOrder ASC, id ASC")
    fun getParticipantsForDistribution(distributionId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants ORDER BY distributionId ASC, displayOrder ASC, id ASC")
    fun getAllParticipants(): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE distributionId = :distributionId ORDER BY displayOrder ASC, id ASC")
    suspend fun getParticipantsForDistributionList(distributionId: Long): List<ParticipantEntity>

    @Query("SELECT * FROM participants WHERE id = :id LIMIT 1")
    suspend fun getParticipantById(id: Long): ParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistribution(distribution: DistributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM participants WHERE id = :id")
    suspend fun deleteParticipantById(id: Long)

    @Query("DELETE FROM participants WHERE distributionId = :distributionId")
    suspend fun deleteParticipantsByDistribution(distributionId: Long)

    @Update
    suspend fun updateDistribution(distribution: DistributionEntity)

    @Query("DELETE FROM distributions WHERE id = :id")
    suspend fun deleteDistribution(id: Long)

    @Transaction
    suspend fun insertFullDistribution(
        distribution: DistributionEntity,
        participants: List<ParticipantEntity>
    ): Long {
        val distId = insertDistribution(distribution)
        if (participants.isNotEmpty()) {
            val updatedParticipants = participants.map { it.copy(distributionId = distId) }
            insertParticipants(updatedParticipants)
        }
        return distId
    }

    @Transaction
    suspend fun updateFullDistribution(
        distribution: DistributionEntity,
        participants: List<ParticipantEntity>
    ) {
        updateDistribution(distribution)
        deleteParticipantsByDistribution(distribution.id)
        if (participants.isNotEmpty()) {
            val updatedParticipants = participants.map { it.copy(distributionId = distribution.id) }
            insertParticipants(updatedParticipants)
        }
    }
}
