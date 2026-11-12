package com.polysius.applocker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.polysius.applocker.permissions.GrantState
import com.polysius.applocker.permissions.PermissionChecker
import com.polysius.applocker.permissions.PermissionKind
import com.polysius.applocker.permissions.needsAttention
import com.polysius.applocker.settings.SettingsRepository
import com.polysius.applocker.settings.settingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { LOADING, SETUP, HOME }

data class MainUiState(
    val screen: Screen = Screen.LOADING,
    val grants: Map<PermissionKind, GrantState> = emptyMap(),
) {
    val anyPermissionNeedsAttention: Boolean
        get() = grants.values.any { it.needsAttention }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val permissionChecker = PermissionChecker(application)
    private val settingsRepository = SettingsRepository(application.settingsDataStore)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val healthPermissions: Set<String> get() = permissionChecker.healthPermissions

    init {
        viewModelScope.launch { settingsRepository.seedDefaultsIfNeeded() }
    }

    /**
     * Re-reads every permission. Called whenever the app returns to the foreground, so the checklist
     * updates after the user comes back from a Settings screen.
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val grants = permissionChecker.checkAll()
            _uiState.update { current ->
                current.copy(grants = grants, screen = current.screen.orInitialChoice(grants))
            }
        }
    }

    fun showSetup() = _uiState.update { it.copy(screen = Screen.SETUP) }

    fun showHome() = _uiState.update { it.copy(screen = Screen.HOME) }

    /** Leaves setup. Onboarding counts as complete only when nothing is missing. */
    fun finishSetup() {
        viewModelScope.launch {
            val allGranted = !_uiState.value.anyPermissionNeedsAttention
            settingsRepository.setOnboardingCompleted(allGranted)
        }
        showHome()
    }

    /** First launch, and any later launch with a missing permission, opens on setup (FR-48). */
    private fun Screen.orInitialChoice(grants: Map<PermissionKind, GrantState>): Screen {
        if (this != Screen.LOADING) return this
        return if (grants.values.any { it.needsAttention }) Screen.SETUP else Screen.HOME
    }
}
