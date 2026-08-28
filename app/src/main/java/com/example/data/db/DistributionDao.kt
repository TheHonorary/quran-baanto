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

    @Query("SELECT * FROM distributions WHERE id = :id")
    fun getDistributionByIdFlow(id: Long): Flow<DistributionEntity?>

    @Query("SELECT * FROM distributions WHERE id = :id")
    suspend fun getDistributionById(id: Long): DistributionEntity?

    @Query("SELECT * FROM participants WHERE distributionId = :distributionId ORDER BY displayOrder ASC")
    fun getParticipantsForDistribution(distributionId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE distributionId = :distributionId ORDER BY displayOrder ASC")
    suspend fun getParticipantsForDistributionList(distributionId: Long): List<ParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistribution(distribution: DistributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

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
        val updatedParticipants = participants.map { it.copy(distributionId = distId) }
        insertParticipants(updatedParticipants)
        return distId
    }

    @Transaction
    suspend fun updateFullDistribution(
        distribution: DistributionEntity,
        participants: List<ParticipantEntity>
    ) {
        updateDistribution(distribution)
        deleteParticipantsByDistribution(distribution.id)
        val updatedParticipants = participants.map { it.copy(distributionId = distribution.id) }
        insertParticipants(updatedParticipants)
    }
}

