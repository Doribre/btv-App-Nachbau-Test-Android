package de.bibeltv.mediathek.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Persistiert die App-Einstellungen (Theme-Modus + Hell-Zeitfenster) via DataStore. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val mode = stringPreferencesKey("theme_mode")
        val lightStart = intPreferencesKey("light_start_min")
        val lightEnd = intPreferencesKey("light_end_min")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.mode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.CUSTOM_TIME,
            lightStartMinutes = p[Keys.lightStart] ?: (5 * 60),
            lightEndMinutes = p[Keys.lightEnd] ?: (17 * 60),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.mode] = mode.name }
    }

    suspend fun setLightWindow(startMinutes: Int, endMinutes: Int) {
        dataStore.edit {
            it[Keys.lightStart] = startMinutes
            it[Keys.lightEnd] = endMinutes
        }
    }
}
