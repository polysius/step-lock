package com.polysius.applocker

import android.content.Context
import androidx.room.withTransaction
import com.polysius.applocker.blocking.AlwaysAllowedApps
import com.polysius.applocker.blocking.EnforcementState
import com.polysius.applocker.data.AppDatabase
import com.polysius.applocker.data.HistoryRepository
import com.polysius.applocker.data.LockedAppsRepository
import com.polysius.applocker.data.ScheduleRepository
import com.polysius.applocker.rules.BedtimeRule
import com.polysius.applocker.rules.RulesConfig
import com.polysius.applocker.rules.ScheduleRule
import com.polysius.applocker.settings.AppSettings
import com.polysius.applocker.settings.SettingsRepository
import com.polysius.applocker.settings.settingsDataStore
import java.time.DayOfWeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * The app's long-lived objects. The Accessibility service and the UI share them, so a change made
 * in a screen reaches the enforcement code without either side knowing about the other.
 */
class AppContainer(context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.create(context)

    /** Runs [block] as one database transaction: all of it is saved, or none of it. */
    suspend fun <T> inTransaction(block: suspend () -> T): T = database.withTransaction { block() }
    val settings = SettingsRepository(context.settingsDataStore)
    val schedule = ScheduleRepository(database.scheduleRuleDao())
    val lockedApps = LockedAppsRepository(database.lockedAppDao())
    val history = HistoryRepository(database.historyDao())
    val alwaysAllowedApps = AlwaysAllowedApps(context)

    /** Everything the rules need, in memory, so a foreground check never waits on the database. */
    val enforcementState: StateFlow<EnforcementState?> = combine(
        schedule.rules,
        lockedApps.apps,
        settings.settings,
    ) { rules, apps, settings ->
        EnforcementState(
            config = RulesConfig(rules, settings.toBedtimeRule()),
            lockedPackages = apps.filter { it.isLocked }.map { it.packageName }.toSet(),
            allowlistedPackages = apps.filter { it.isAllowlisted }.map { it.packageName }.toSet(),
            allAppsMode = settings.allAppsMode,
            protectionEnabled = settings.protectionEnabled,
        )
    }.stateIn(applicationScope, SharingStarted.Eagerly, null)

    val controller = AppController(this)
}

fun AppSettings.toBedtimeRule() = BedtimeRule(
    bedtimeMinutes = bedtimeMinutes,
    leadMinutes = bedtimeLeadMinutes,
    endMinutes = bedtimeEndMinutes,
    days = DayOfWeek.entries.toSet(),
)

/** Convenience for screens that show rules without the rest of the state. */
val EnforcementState.rules: List<ScheduleRule> get() = config.rules
