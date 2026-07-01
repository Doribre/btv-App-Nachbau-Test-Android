package de.bibeltv.mediathek.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.layout.Row
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.feature.bible.data.BibleSearchHit
import de.bibeltv.mediathek.feature.common.CenteredHint
import de.bibeltv.mediathek.feature.common.PagedVideoGrid

@Composable
fun SearchScreen(
    onVideoClick: (VideoItem) -> Unit,
    onOpenBibleVerse: (BibleSearchHit) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val items = viewModel.results.collectAsLazyPagingItems()
    val bibleHits by viewModel.bibleHits.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Titel, Serie, Thema … oder in der Bibel") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (query.isBlank()) {
            CenteredHint("Suche nach Titel, Serie, Thema, Genre – oder Volltext in der Bibel.")
        } else {
            if (bibleHits.isNotEmpty()) {
                BibleResults(bibleHits, onOpenBibleVerse)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
            PagedVideoGrid(
                items = items,
                onVideoClick = onVideoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                emptyText = "Keine Video-Treffer für „$query“.",
            )
        }
    }
}

@Composable
private fun BibleResults(hits: List<BibleSearchHit>, onOpen: (BibleSearchHit) -> Unit) {
    val shown = hits.take(5)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
            Icon(
                Icons.Filled.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                "In der Bibel (${hits.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        shown.forEach { hit ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(hit) }
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    hit.reference,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (hit.snippet.isNotBlank()) {
                    Text(
                        hit.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (hits.size > shown.size) {
            Text(
                "+ ${hits.size - shown.size} weitere Bibelstellen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
