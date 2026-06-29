package de.bibeltv.mediathek.data.settings

/** Theme-Modus laut Nutzer-Wunsch: Gerät folgen, fest hell, fest dunkel oder zeitgesteuert. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, CUSTOM_TIME }

/**
 * App-Einstellungen. Default: zeitgesteuert, hell von 05:00 bis 17:00, danach dunkel
 * (damit es zum abendlichen Filmeschauen dunkel wird).
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.CUSTOM_TIME,
    val lightStartMinutes: Int = 5 * 60,   // 05:00
    val lightEndMinutes: Int = 17 * 60,    // 17:00
)
