package de.bibeltv.mediathek.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.bibeltv.mediathek.data.settings.ThemeMode

private enum class Editing { START, END }

private fun formatTime(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            SectionHeader("Darstellung")
            ThemeOption(
                selected = settings.themeMode == ThemeMode.SYSTEM,
                title = "Wie Gerät",
                subtitle = "Folgt der System-Einstellung",
                onSelect = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
            )
            ThemeOption(
                selected = settings.themeMode == ThemeMode.LIGHT,
                title = "Hell",
                subtitle = "Immer heller Modus",
                onSelect = { viewModel.setThemeMode(ThemeMode.LIGHT) },
            )
            ThemeOption(
                selected = settings.themeMode == ThemeMode.DARK,
                title = "Dunkel",
                subtitle = "Immer dunkler Modus",
                onSelect = { viewModel.setThemeMode(ThemeMode.DARK) },
            )
            ThemeOption(
                selected = settings.themeMode == ThemeMode.CUSTOM_TIME,
                title = "Eigene Zeiten",
                subtitle = "Tagsüber hell, abends dunkel",
                onSelect = { viewModel.setThemeMode(ThemeMode.CUSTOM_TIME) },
            )

            if (settings.themeMode == ThemeMode.CUSTOM_TIME) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                TimeRow("Hell ab", formatTime(settings.lightStartMinutes)) { editing = Editing.START }
                TimeRow("Dunkel ab", formatTime(settings.lightEndMinutes)) { editing = Editing.END }
                Text(
                    text = "Aktuell hell von ${formatTime(settings.lightStartMinutes)} bis " +
                        "${formatTime(settings.lightEndMinutes)} Uhr, danach dunkel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }

    when (editing) {
        Editing.START -> TimePickerDialog(
            title = "Hell ab",
            initialMinutes = settings.lightStartMinutes,
            onConfirm = {
                viewModel.setLightWindow(it, settings.lightEndMinutes)
                editing = null
            },
            onDismiss = { editing = null },
        )
        Editing.END -> TimePickerDialog(
            title = "Dunkel ab",
            initialMinutes = settings.lightEndMinutes,
            onConfirm = {
                viewModel.setLightWindow(settings.lightStartMinutes, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
        null -> Unit
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun ThemeOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimeRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimeInput(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
