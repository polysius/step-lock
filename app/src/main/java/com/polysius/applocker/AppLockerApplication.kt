package com.polysius.applocker

import android.app.Application
import com.polysius.applocker.data.LockedAppEntity
import com.polysius.applocker.rules.ScheduleKind
import com.polysius.applocker.rules.ScheduleRule
import com.polysius.applocker.rules.TimeWindow
import com.polysius.applocker.settings.SchedulePreset
import java.time.DayOfWeek
import kotlinx.coroutines.launch

class AppLockerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch {
            container.settings.seedDefaultsIfNeeded()
            seedPresetIfNeeded()
        }
    }

    /** Writes the first-run apps and schedule once. Later edits by the user are never overwritten. */
    private suspend fun seedPresetIfNeeded() {
        if (container.settings.isPresetSeeded()) return
        val now = System.currentTimeMillis()

        // One transaction, and only into an empty schedule, so a process that dies half way
        // through cannot leave the schedule seeded twice on the next start.
        container.inTransaction {
            if (!container.schedule.isEmpty()) return@inTransaction
            SchedulePreset.DEFAULT_RULES.forEach { preset ->
                val rule = ScheduleRule(
                    id = 0,
                    name = preset.name,
                    kind = preset.kind,
                    window = TimeWindow(DayOfWeek.entries.toSet(), preset.startMinutes, preset.endMinutes),
                    isEnabled = true,
                )
                container.schedule.save(rule, now)
            }
            container.lockedApps.setLocked(
                SchedulePreset.DEFAULT_LOCKED_PACKAGE,
                SchedulePreset.DEFAULT_LOCKED_LABEL,
                locked = true,
                nowMillis = now,
            )
            SchedulePreset.DEFAULT_ALLOWLISTED_APPS.forEach { (packageName, label) ->
                container.lockedApps.setAllowlisted(packageName, label, allowlisted = true, nowMillis = now)
            }
        }
        container.settings.markPresetSeeded()
    }
}

val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as AppLockerApplication).container
