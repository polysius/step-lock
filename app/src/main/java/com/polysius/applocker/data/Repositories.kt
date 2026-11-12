package com.polysius.applocker.data

import com.polysius.applocker.rules.ScheduleRule
import com.polysius.applocker.rules.TimeWindow
import com.polysius.applocker.rules.toDayMask
import com.polysius.applocker.rules.toDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepository(private val dao: ScheduleRuleDao) {

    val rules: Flow<List<ScheduleRule>> = dao.observeAll().map { list -> list.map { it.toRule() } }

    /** Inserts when [rule] has id 0, otherwise updates the stored rule. */
    suspend fun save(rule: ScheduleRule, nowMillis: Long) {
        if (rule.id == 0L) {
            dao.insert(rule.toEntity(createdAt = nowMillis, updatedAt = nowMillis))
        } else {
            val createdAt = dao.get(rule.id)?.createdAt ?: nowMillis
            dao.update(rule.toEntity(createdAt = createdAt, updatedAt = nowMillis))
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun isEmpty(): Boolean = dao.count() == 0

    private fun ScheduleRuleEntity.toRule() = ScheduleRule(
        id = id,
        name = name,
        kind = kind,
        window = TimeWindow(daysMask.toDays(), startMinutes, endMinutes),
        isEnabled = isEnabled,
    )

    private fun ScheduleRule.toEntity(createdAt: Long, updatedAt: Long) = ScheduleRuleEntity(
        id = id,
        name = name,
        kind = kind,
        daysMask = window.days.toDayMask(),
        startMinutes = window.startMinutes,
        endMinutes = window.endMinutes,
        isEnabled = isEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

class LockedAppsRepository(private val dao: LockedAppDao) {

    val apps: Flow<List<LockedAppEntity>> = dao.observeAll()

    suspend fun setLocked(packageName: String, label: String, locked: Boolean, nowMillis: Long) {
        val current = dao.get(packageName) ?: LockedAppEntity(packageName, label, false, false, nowMillis)
        dao.upsert(current.copy(appLabel = label, isLocked = locked))
    }

    suspend fun setAllowlisted(packageName: String, label: String, allowlisted: Boolean, nowMillis: Long) {
        val current = dao.get(packageName) ?: LockedAppEntity(packageName, label, false, false, nowMillis)
        dao.upsert(current.copy(appLabel = label, isAllowlisted = allowlisted))
    }
}

class HistoryRepository(private val dao: HistoryDao) {

    fun recent(limit: Int): Flow<List<HistoryEntryEntity>> = dao.observeRecent(limit)

    suspend fun log(type: HistoryType, detail: String, packageName: String? = null, nowMillis: Long) {
        dao.insert(HistoryEntryEntity(timestampMillis = nowMillis, type = type, packageName = packageName, detail = detail))
    }
}
