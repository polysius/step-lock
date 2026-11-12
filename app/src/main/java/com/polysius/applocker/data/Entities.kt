package com.polysius.applocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.polysius.applocker.rules.ScheduleKind

@Entity(tableName = "locked_app")
data class LockedAppEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    /** Locked when the app list mode is on. */
    val isLocked: Boolean,
    /** Never blocked when all-apps mode is on. */
    val isAllowlisted: Boolean,
    val addedAt: Long,
)

@Entity(tableName = "schedule_rule")
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: ScheduleKind,
    /** See [com.polysius.applocker.rules.toDayMask]. */
    val daysMask: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class HistoryType { BLOCK_ATTEMPT, SCHEDULE_CHANGE, APP_LIST_CHANGE, PROTECTION_CHANGE, SETTINGS_CHANGE }

@Entity(tableName = "history_entry")
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val type: HistoryType,
    val packageName: String? = null,
    val detail: String,
)
