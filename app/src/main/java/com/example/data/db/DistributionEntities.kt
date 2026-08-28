package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DistributionType {
    KHATM,
    AMAL
}

@Entity(tableName = "distributions")
data class DistributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DistributionType = DistributionType.KHATM,
    val totalTarget: Int = 30, // 30 for Khatm, custom integer for Amal (e.g. 110)
    val createdTimestamp: Long = System.currentTimeMillis(),
    val startYear: Int,
    val startMonth: Int // 1..12
)

@Entity(
    tableName = "participants",
    foreignKeys = [
        ForeignKey(
            entity = DistributionEntity::class,
            parentColumns = ["id"],
            childColumns = ["distributionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("distributionId")]
)
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val distributionId: Long,
    val name: String,
    val allocatedCount: Int, // Number of Paras (for Khatm) or Recitations (for Amal)
    val displayOrder: Int
)


