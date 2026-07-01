package de.bibeltv.mediathek.feature.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Formatiert die (in Millisekunden gespeicherte) Laufzeit als "M:SS" bzw. "H:MM:SS". */
fun formatRuntime(durationMs: Int): String? {
    if (durationMs <= 0) return null
    val totalSec = durationMs / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Kleines Laufzeit-Label unten rechts auf einem Thumbnail (innerhalb einer Box). */
@Composable
fun BoxScope.DurationBadge(durationMs: Int) {
    val label = formatRuntime(durationMs) ?: return
    Surface(
        color = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}
