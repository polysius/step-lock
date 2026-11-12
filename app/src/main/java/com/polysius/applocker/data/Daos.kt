package com.polysius.applocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_app")
    fun observeAll(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_app WHERE packageName = :packageName")
    suspend fun get(packageName: String): LockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: LockedAppEntity)
}

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM schedule_rule ORDER BY startMinutes, id")
    fun observeAll(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT COUNT(*) FROM schedule_rule")
    suspend fun count(): Int

    @Insert
    suspend fun insert(rule: ScheduleRuleEntity): Long

    @Update
    suspend fun update(rule: ScheduleRuleEntity)

    @Query("DELETE FROM schedule_rule WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM schedule_rule WHERE id = :id")
    suspend fun get(id: Long): ScheduleRuleEntity?
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entry ORDER BY timestampMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntryEntity)
}
