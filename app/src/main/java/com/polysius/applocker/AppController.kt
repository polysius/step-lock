package com.polysius.applocker

import com.polysius.applocker.data.HistoryType
import com.polysius.applocker.rules.BedtimeRule
import com.polysius.applocker.rules.RulesConfig
import com.polysius.applocker.rules.ScheduleKind
import com.polysius.applocker.rules.ScheduleRule
import com.polysius.applocker.rules.StrictGuard
import com.polysius.applocker.rules.StrictResult
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

/**
 * Every change a user can make to the locking rules goes through here. Each method asks
 * [StrictGuard] first and only then writes, so no screen can skip the strict check (FR-40).
 * A refused change writes nothing. There is no override parameter (FR-43).
 */
class AppController(
    private val container: AppContainer,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {

    private suspend fun config(): RulesConfig {
        val state = container.enforcementState.first { it != null }
        return state!!.config
    }

    private fun millis() = System.currentTimeMillis()

    /**
     * Creates a rule when [rule] has id 0, or edits the stored one. The stored version is looked up
     * here, never taken from the caller, so an edit cannot skip the check on the active rule.
     */
    suspend fun saveRule(rule: ScheduleRule): StrictResult {
        val moment = now()
        val config = config()
        val previous = config.rules.firstOrNull { it.id == rule.id && rule.id != 0L }
        if (previous != null) {
            val editCheck = StrictGuard.changeRule(previous, moment)
            if (editCheck is StrictResult.Refused) return editCheck
        }
        val saveCheck = StrictGuard.saveRule(rule, config, moment)
        if (saveCheck is StrictResult.Refused) return saveCheck

        container.schedule.save(rule, millis())
        val verb = if (previous == null) "Added" else "Edited"
        container.history.log(HistoryType.SCHEDULE_CHANGE, "$verb ${rule.kind.label()} \"${rule.name}\"", nowMillis = millis())
        return StrictResult.Allowed
    }

    suspend fun deleteRule(rule: ScheduleRule): StrictResult {
        val check = StrictGuard.changeRule(rule, now())
        if (check is StrictResult.Refused) return check

        container.schedule.delete(rule.id)
        container.history.log(HistoryType.SCHEDULE_CHANGE, "Deleted ${rule.kind.label()} \"${rule.name}\"", nowMillis = millis())
        return StrictResult.Allowed
    }

    suspend fun setBedtime(bedtimeMinutes: Int, leadMinutes: Int, endMinutes: Int): StrictResult {
        val candidate = BedtimeRule(bedtimeMinutes, leadMinutes, endMinutes, DayOfWeek.entries.toSet())
        require(candidate.isValid) { "Bedtime lock would start exactly when it ends" }
        val check = StrictGuard.changeBedtime(config(), now())
        if (check is StrictResult.Refused) return check

        container.settings.setBedtime(bedtimeMinutes, leadMinutes, endMinutes)
        container.history.log(
            HistoryType.SETTINGS_CHANGE,
            "Bedtime set to ${formatMinutes(bedtimeMinutes)}, lead $leadMinutes min, ends ${formatMinutes(endMinutes)}",
            nowMillis = millis(),
        )
        return StrictResult.Allowed
    }

    suspend fun setLocked(packageName: String, label: String, locked: Boolean): StrictResult {
        if (!locked) {
            val check = StrictGuard.weakenProtection(config(), now())
            if (check is StrictResult.Refused) return check
        }
        container.lockedApps.setLocked(packageName, label, locked, millis())
        val action = if (locked) "Locked" else "Unlocked"
        container.history.log(HistoryType.APP_LIST_CHANGE, "$action $label", packageName, millis())
        return StrictResult.Allowed
    }

    /** Allowlisting only matters in all-apps mode, so that is the only case the guard applies to. */
    suspend fun setAllowlisted(packageName: String, label: String, allowlisted: Boolean): StrictResult {
        val state = container.enforcementState.first { it != null }!!
        if (allowlisted && state.allAppsMode) {
            val check = StrictGuard.weakenProtection(state.config, now())
            if (check is StrictResult.Refused) return check
        }
        container.lockedApps.setAllowlisted(packageName, label, allowlisted, millis())
        val action = if (allowlisted) "Allowlisted" else "Removed from allowlist"
        container.history.log(HistoryType.APP_LIST_CHANGE, "$action $label", packageName, millis())
        return StrictResult.Allowed
    }

    suspend fun setAllAppsMode(enabled: Boolean): StrictResult {
        val state = container.enforcementState.first { it != null }!!
        val check = if (enabled) {
            StrictGuard.enableAllAppsMode(state.config, now(), state.lockedPackages, state.allowlistedPackages)
        } else {
            StrictGuard.weakenProtection(state.config, now())
        }
        if (check is StrictResult.Refused) return check
        container.settings.setAllAppsMode(enabled)
        container.history.log(HistoryType.APP_LIST_CHANGE, "All-apps mode ${if (enabled) "on" else "off"}", nowMillis = millis())
        return StrictResult.Allowed
    }

    /** Turning protection on is always allowed. Turning it off is refused while a rule is active. */
    suspend fun setProtectionEnabled(enabled: Boolean): StrictResult {
        if (!enabled) {
            val check = StrictGuard.weakenProtection(config(), now())
            if (check is StrictResult.Refused) return check
        }
        container.settings.setProtectionEnabled(enabled)
        container.history.log(HistoryType.PROTECTION_CHANGE, "Protection turned ${if (enabled) "on" else "off"}", nowMillis = millis())
        return StrictResult.Allowed
    }

    private fun ScheduleKind.label() = if (this == ScheduleKind.BLOCK) "block" else "break"
}

fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
