package com.polysius.applocker.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    private fun newRepository() = SettingsRepository(InMemoryPreferencesDataStore())

    @Test
    fun `seeding writes the preset values`() = runBlocking {
        val repository = newRepository()

        repository.seedDefaultsIfNeeded()

        val settings = repository.settings.first()
        assertEquals(AppDefaults.DEFAULT_BEDTIME_MINUTES, settings.bedtimeMinutes)
        assertEquals(AppDefaults.DEFAULT_BEDTIME_LEAD_MINUTES, settings.bedtimeLeadMinutes)
        assertEquals(AppDefaults.DEFAULT_DAY_BOUNDARY_MINUTES, settings.dayBoundaryMinutes)
        assertEquals(AppDefaults.DEFAULT_WARNING_THRESHOLD_MINUTES, settings.warningThresholdMinutes)
        assertEquals(false, settings.protectionEnabled)
    }

    @Test
    fun `seeding twice keeps a value changed in between`() = runBlocking {
        val repository = newRepository()
        repository.seedDefaultsIfNeeded()
        repository.setOnboardingCompleted(true)

        repository.seedDefaultsIfNeeded()

        assertTrue(repository.settings.first().onboardingCompleted)
    }
}

/**
 * A DataStore that keeps preferences in memory. The real file-backed store replaces its file with
 * a rename that fails on Windows when the file already exists, which would make these tests depend
 * on the OS they run on.
 */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
