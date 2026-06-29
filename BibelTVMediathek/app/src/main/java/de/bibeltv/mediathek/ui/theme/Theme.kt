package de.bibeltv.mediathek.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import de.bibeltv.mediathek.data.settings.AppSettings
import de.bibeltv.mediathek.data.settings.ThemeMode
import kotlinx.coroutines.delay
import java.time.LocalTime

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF6FF7E6),
    secondary = TealBright,
    onSecondary = Color(0xFF00201C),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = LiveRed,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6FF7E6),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Teal,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Color(0xFFC62828),
    onError = Color.White,
)

@Composable
fun BibelTVMediathekTheme(
    // v1 ist dunkel-first; das helle Schema bleibt für später vorhanden.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}

/**
 * Ermittelt aus den Einstellungen, ob gerade dunkel angezeigt werden soll.
 * Bei „Eigene Zeiten" wird minütlich neu geprüft, damit das Theme z. B. um 17:00 automatisch umschaltet.
 */
@Composable
fun resolveDarkTheme(settings: AppSettings): Boolean = when (settings.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.CUSTOM_TIME -> rememberDarkByTime(settings.lightStartMinutes, settings.lightEndMinutes)
}

@Composable
private fun rememberDarkByTime(lightStartMinutes: Int, lightEndMinutes: Int): Boolean {
    var minuteOfDay by remember { mutableIntStateOf(nowMinuteOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            minuteOfDay = nowMinuteOfDay()
            delay(30_000L)
        }
    }
    val isLight = if (lightStartMinutes <= lightEndMinutes) {
        minuteOfDay in lightStartMinutes until lightEndMinutes
    } else {
        // Über Mitternacht gewickeltes Fenster (z. B. hell 22:00–06:00).
        minuteOfDay >= lightStartMinutes || minuteOfDay < lightEndMinutes
    }
    return !isLight
}

private fun nowMinuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
