package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DistributionType {
    KHATM,          // Monthly Rotating 30-Para Qur'an Khatm
    ONE_TIME_KHATM, // One-Time 30-Para Qur'an Khatm (celebrates & auto-cleans up when 30/30 done)
    AMAL            // Custom Amal Recitation (supports Reopen Room with new passcode)
}

enum class RoomMode {
    LOCAL,
    ONLINE
}

@Entity(tableName = "distributions")
data class DistributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DistributionType = DistributionType.KHATM,
    val roomMode: RoomMode = RoomMode.LOCAL,
    val roomPasscode: String? = null,
    val totalTarget: Int = 30, // 30 for KHATM / ONE_TIME_KHATM, custom integer for AMAL
    val createdTimestamp: Long = System.currentTimeMillis(),
    val startYear: Int,
    val startMonth: Int, // 1..12
    val roundNumber: Int = 1, // Increments when an Amal room is reopened
    val isClosedForever: Boolean = false, // True when a One-Time Khatm completes all 30 Paras and prunes row data
    val closedSummaryText: String? = null, // Lightweight summary kept after One-Time Khatm auto-cleanup
    val completedTimestamp: Long? = null,
    val hasPendingSync: Boolean = false, // True when offline changes need to be flushed to Firestore
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val lastSyncedTimestamp: Long = 0L
) {
    val isOnline: Boolean
        get() = roomMode == RoomMode.ONLINE
}

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
    val allocatedCount: Int, // Number of Paras (for KHATM) or Recitations (for AMAL) or 1 (for ONE_TIME_KHATM)
    val displayOrder: Int,
    val specificParaNumber: Int? = null, // 1..30 for ONE_TIME_KHATM first-come-first-served Para claims
    val completedCount: Int = 0, // Tactile recitation counter progress (0..allocatedCount) for AMAL
    val isCompleted: Boolean = false, // Completion tickmark for ONE_TIME_KHATM and AMAL
    val completedCycleMonthsCsv: String = "", // Comma-separated "YYYY-M" keys for Monthly KHATM completion
    val isGuestEntry: Boolean = false, // True if claimed on behalf of an elderly family member / guest (e.g. Grandma)
    val addedByProfileName: String = "",
    val isPinned: Boolean = false, // Pin My Paras to the top of the room view
    val hasPendingSync: Boolean = false, // True when local offline updates are waiting to sync to Firestore
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val updatedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun isCompletedForMonth(year: Int, month: Int): Boolean {
        if (completedCycleMonthsCsv.isBlank()) return false
        val key = "$year-$month"
        return completedCycleMonthsCsv.split(",").any { it.trim() == key }
    }

    fun withToggledMonthCompletion(year: Int, month: Int): ParticipantEntity {
        val key = "$year-$month"
        val currentSet = completedCycleMonthsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        if (currentSet.contains(key)) {
            currentSet.remove(key)
        } else {
            currentSet.add(key)
        }
        return copy(completedCycleMonthsCsv = currentSet.joinToString(","))
    }
}
