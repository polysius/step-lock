package com.polysius.applocker.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user-editable settings that exist in Phase 0. More are added as later phases need them. */
data class AppSettings(
    val bedtimeMinutes: Int,
    val bedtimeLeadMinutes: Int,
    val dayBoundaryMinutes: Int,
    val warningThresholdMinutes: List<Int>,
    val protectionEnabled: Boolean,
    val onboardingCompleted: Boolean,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    /**
     * Writes the preset values the first time the app runs. Later calls change nothing, so a value
     * the user edited is never overwritten.
     */
    suspend fun seedDefaultsIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[Keys.SEEDED] == true) return@edit
            prefs[Keys.BEDTIME_MINUTES] = AppDefaults.DEFAULT_BEDTIME_MINUTES
            prefs[Keys.BEDTIME_LEAD_MINUTES] = AppDefaults.DEFAULT_BEDTIME_LEAD_MINUTES
            prefs[Keys.DAY_BOUNDARY_MINUTES] = AppDefaults.DEFAULT_DAY_BOUNDARY_MINUTES
            prefs[Keys.WARNING_THRESHOLD_MINUTES] =
                AppDefaults.DEFAULT_WARNING_THRESHOLD_MINUTES.joinToString(THRESHOLD_SEPARATOR)
            prefs[Keys.PROTECTION_ENABLED] = false
            prefs[Keys.ONBOARDING_COMPLETED] = false
            prefs[Keys.SEEDED] = true
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    private fun Preferences.toSettings(): AppSettings {
        // Reading falls back to the preset only in the instant before seeding has run.
        return AppSettings(
            bedtimeMinutes = this[Keys.BEDTIME_MINUTES] ?: AppDefaults.DEFAULT_BEDTIME_MINUTES,
            bedtimeLeadMinutes =
                this[Keys.BEDTIME_LEAD_MINUTES] ?: AppDefaults.DEFAULT_BEDTIME_LEAD_MINUTES,
            dayBoundaryMinutes =
                this[Keys.DAY_BOUNDARY_MINUTES] ?: AppDefaults.DEFAULT_DAY_BOUNDARY_MINUTES,
            warningThresholdMinutes = this[Keys.WARNING_THRESHOLD_MINUTES]
                ?.split(THRESHOLD_SEPARATOR)
                ?.mapNotNull { it.toIntOrNull() }
                ?: AppDefaults.DEFAULT_WARNING_THRESHOLD_MINUTES,
            protectionEnabled = this[Keys.PROTECTION_ENABLED] ?: false,
            onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    private object Keys {
        val SEEDED = booleanPreferencesKey("seeded")
        val BEDTIME_MINUTES = intPreferencesKey("bedtime_minutes")
        val BEDTIME_LEAD_MINUTES = intPreferencesKey("bedtime_lead_minutes")
        val DAY_BOUNDARY_MINUTES = intPreferencesKey("day_boundary_minutes")
        val WARNING_THRESHOLD_MINUTES = stringPreferencesKey("warning_threshold_minutes")
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    private companion object {
        const val THRESHOLD_SEPARATOR = ","
    }
}
