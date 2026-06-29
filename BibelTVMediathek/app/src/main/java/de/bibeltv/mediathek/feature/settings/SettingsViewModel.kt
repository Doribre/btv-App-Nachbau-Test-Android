package de.bibeltv.mediathek.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.bibeltv.mediathek.data.settings.AppSettings
import de.bibeltv.mediathek.data.settings.SettingsRepository
import de.bibeltv.mediathek.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setLightWindow(startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch { repo.setLightWindow(startMinutes, endMinutes) }
    }
}
