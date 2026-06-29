package de.bibeltv.mediathek.feature.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.bibeltv.mediathek.R

/** Wortmarke „✱ bibel APP" – adaptiv eingefärbt (Stern/APP = primary, Wortmarke = onSurface). */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Image(
            painter = painterResource(R.drawable.ic_btv_star),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier.height(26.dp),
        )
        Spacer(Modifier.width(7.dp))
        Image(
            painter = painterResource(R.drawable.ic_btv_wordmark),
            contentDescription = "bibel",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.height(19.dp),
        )
        Text(
            text = "APP",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            // Die Textbox reicht unter die Grundlinie (Descender); nach unten schieben,
            // damit "APP" bündig mit Stern und Wortmarke abschließt.
            modifier = Modifier
                .padding(start = 6.dp)
                .offset(y = 4.dp),
        )
    }
}
