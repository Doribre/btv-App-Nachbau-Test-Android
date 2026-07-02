package de.bibeltv.mediathek.feature.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.bibeltv.mediathek.BuildConfig
import de.bibeltv.mediathek.feature.common.BrandWordmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Info") },
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
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BrandWordmark()
            Text(
                text = "Mediathek von Bibel TV",
                style = MaterialTheme.typography.titleMedium,
            )
            InfoRow("Version", BuildConfig.VERSION_NAME)
            InfoRow("Inhalte", "VideoHub – über 20.000 Sendungen, Serien und Live-Kanäle")
            Text(
                text = "Diese App bündelt die Mediathek von Bibel TV: Filme, Serien, " +
                    "Predigten, Gottesdienste, Andachten und mehr – jederzeit abrufbar, " +
                    "dazu die Live-Sender. Eine integrierte Online-Bibel ist als nächster " +
                    "Schritt geplant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "© Bibel TV. Inhalte und Marke „Bibel TV“ gehören Bibel TV.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Text(
                text = "Architektur & Technik",
                style = MaterialTheme.typography.titleMedium,
            )
            InfoRow("Plattform", "Native Android · Kotlin · min SDK 26, target SDK 36")
            InfoRow("UI", "Jetpack Compose + Material 3 · Navigation Compose (typsichere Routen)")
            InfoRow(
                "Architektur",
                "MVVM / unidirektionaler State-Flow · Hilt (Dependency Injection) · " +
                    "Single-Module, nach Feature gegliedert",
            )
            InfoRow(
                "Daten",
                "Apollo Kotlin GraphQL (VideoHub) · Paging 3 · OkHttp · Coil (Bilder) · " +
                    "DataStore (Einstellungen)",
            )
            InfoRow("Player", "Media3 / ExoPlayer · HLS & DASH · Widevine-DRM")
            InfoRow(
                "Bibelthek",
                "Verse live geladen (Gute Nachricht Bibel), nichts gespeichert · " +
                    "Vers→Video über den VideoHub",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Text(
                text = "Token-Verbrauch dieser Session",
                style = MaterialTheme.typography.titleMedium,
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "≈ $DEV_TOKENS_OUTPUT",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "von Claude erzeugte Ausgabe-Tokens · exakt $DEV_TOKENS_OUTPUT_EXACT",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = DEV_TOKENS_DETAIL,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Hinweis: Der größte Teil davon ist wiederholt gelesener Kontext (Cache) – " +
                    "nicht neu erzeugt. Hintergrund-Subagenten sind nicht mitgezählt. Snapshot bis zu diesem Build.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Text(
                text = "Entwicklungs-Chronik",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "App-Version ${BuildConfig.VERSION_NAME}. Alle ${DEV_PROMPTS.size} in dieser " +
                    "Entwicklungs-Session getippten Prompts – chronologisch, mit Wochentag/Uhrzeit " +
                    "(Ortszeit), dem Antwort-Modell und den erzeugten Ausgabe-Tokens (Hauptdialog). " +
                    "Screenshots, System- und Task-Meldungen sowie Wiederholungen sind ausgelassen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DEV_PROMPTS.forEachIndexed { index, p ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${index + 1}.  ${p.time}  ·  ${p.model}  ·  ${p.tokens}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = p.text,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
