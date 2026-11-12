package com.polysius.applocker.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The user-editable settings that exist in Phase 0. More are added as later phases need them. */
data class AppSettings(
    val bedtimeMinutes: Int,
    val bedtimeLeadMinutes: Int,
    val bedtimeEndMinutes: Int,
    val allAppsMode: Boolean,
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
            prefs[Keys.BEDTIME_END_MINUTES] = AppDefaults.DEFAULT_BEDTIME_END_MINUTES
            prefs[Keys.ALL_APPS_MODE] = AppDefaults.DEFAULT_ALL_APPS_MODE
            prefs[Keys.DAY_BOUNDARY_MINUTES] = AppDefaults.DEFAULT_DAY_BOUNDARY_MINUTES
            prefs[Keys.WARNING_THRESHOLD_MINUTES] =
                AppDefaults.DEFAULT_WARNING_THRESHOLD_MINUTES.joinToString(THRESHOLD_SEPARATOR)
            prefs[Keys.PROTECTION_ENABLED] = false
            prefs[Keys.ONBOARDING_COMPLETED] = false
            prefs[Keys.SEEDED] = true
        }
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PROTECTION_ENABLED] = enabled }
    }

    suspend fun setAllAppsMode(enabled: Boolean) {
        dataStore.edit { it[Keys.ALL_APPS_MODE] = enabled }
    }

    suspend fun setBedtime(bedtimeMinutes: Int, leadMinutes: Int, endMinutes: Int) {
        dataStore.edit {
            it[Keys.BEDTIME_MINUTES] = bedtimeMinutes
            it[Keys.BEDTIME_LEAD_MINUTES] = leadMinutes
            it[Keys.BEDTIME_END_MINUTES] = endMinutes
        }
    }

    /** True once the first-run preset (apps and schedule) has been written. */
    suspend fun isPresetSeeded(): Boolean =
        dataStore.data.first()[Keys.PRESET_SEEDED] == true

    suspend fun markPresetSeeded() {
        dataStore.edit { it[Keys.PRESET_SEEDED] = true }
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
            bedtimeEndMinutes =
                this[Keys.BEDTIME_END_MINUTES] ?: AppDefaults.DEFAULT_BEDTIME_END_MINUTES,
            allAppsMode = this[Keys.ALL_APPS_MODE] ?: AppDefaults.DEFAULT_ALL_APPS_MODE,
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
        val BEDTIME_END_MINUTES = intPreferencesKey("bedtime_end_minutes")
        val ALL_APPS_MODE = booleanPreferencesKey("all_apps_mode")
        val PRESET_SEEDED = booleanPreferencesKey("preset_seeded")
        val DAY_BOUNDARY_MINUTES = intPreferencesKey("day_boundary_minutes")
        val WARNING_THRESHOLD_MINUTES = stringPreferencesKey("warning_threshold_minutes")
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    private companion object {
        const val THRESHOLD_SEPARATOR = ","
    }
}
