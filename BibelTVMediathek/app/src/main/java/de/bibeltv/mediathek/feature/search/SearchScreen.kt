package de.bibeltv.mediathek.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.feature.bible.data.BibleSearchHit
import de.bibeltv.mediathek.feature.common.CenteredHint
import de.bibeltv.mediathek.feature.common.DurationBadge
import de.bibeltv.mediathek.feature.common.ErrorRetry
import de.bibeltv.mediathek.feature.common.LoadingBox

@Composable
fun SearchScreen(
    onVideoClick: (VideoItem) -> Unit,
    onOpenBibleVerse: (BibleSearchHit) -> Unit,
    onOpenBibleChapter: (bookSlug: String, bookName: String, chapter: Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val merged by viewModel.merged.collectAsStateWithLifecycle()
    val items = viewModel.results.collectAsLazyPagingItems()
    val chapterTarget by viewModel.chapterTarget.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Titel, Serie, Thema … oder in der Bibel") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            // Sprung in die Bibelthek erst bei "Enter"/Suchen – kein Auto-Sprung beim Tippen.
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.currentChapterTarget()?.let { onOpenBibleChapter(it.slug, it.name, it.chapter) }
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        when {
            query.isBlank() ->
                CenteredHint("Suche nach Titel, Serie, Thema, Genre – oder Volltext in der Bibel.")

            // Konkrete Bibelstelle erkannt -> KEINE Suchergebnisse, nur der direkte Absprung.
            chapterTarget != null -> {
                val target = chapterTarget!!
                ChapterJumpCard(target) { onOpenBibleChapter(target.slug, target.name, target.chapter) }
            }

            else -> {
                var showMore by rememberSaveable(query) { mutableStateOf(false) }
                val videoRefreshing = items.loadState.refresh is LoadState.Loading
                // crns, die bereits oben (gemischt) gezeigt werden -> im Tail nicht doppeln.
                val shownCrns = merged.mapNotNull { (it as? SearchResultItem.Video)?.video?.crn }.toSet()

                when {
                    merged.isEmpty() && videoRefreshing ->
                        LoadingBox(Modifier.fillMaxWidth().weight(1f))

                    merged.isEmpty() ->
                        CenteredHint("Keine Treffer für \"$query\".", Modifier.fillMaxWidth().weight(1f))

                    else -> LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        itemsIndexed(merged, key = { _, item -> item.key() }) { _, item ->
                            when (item) {
                                is SearchResultItem.Bible -> BibleResultRow(item.hit) { onOpenBibleVerse(item.hit) }
                                is SearchResultItem.Video -> VideoResultRow(item.video) { onVideoClick(item.video) }
                            }
                        }

                        if (!showMore) {
                            item {
                                TextButton(
                                    onClick = { showMore = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) { Text("Weitere Mediathek-Treffer laden") }
                            }
                        } else {
                            items(count = items.itemCount, key = items.itemKey { "tail:" + it.crn }) { index ->
                                items[index]?.let { v ->
                                    if (v.crn !in shownCrns) VideoResultRow(v) { onVideoClick(v) }
                                }
                            }
                            when (items.loadState.append) {
                                is LoadState.Loading -> item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { CircularProgressIndicator() }
                                }
                                is LoadState.Error -> item {
                                    ErrorRetry(
                                        message = "Weitere Inhalte konnten nicht geladen werden.",
                                        onRetry = { items.retry() },
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Direkter Absprung ins Kapitel, wenn eine konkrete Bibelstelle eingegeben wurde. */
@Composable
private fun ChapterJumpCard(target: ChapterTarget, onOpen: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onOpen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                Icons.Filled.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${target.name} ${target.chapter}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Kapitel in der Bibelthek öffnen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/** Kleiner Typ-Marker links auf jeder Trefferzeile (Bibel vs. Video). */
@Composable
private fun TypeChip(label: String, icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun BibleResultRow(hit: BibleSearchHit, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeChip("Bibel", Icons.Filled.Book)
            Spacer(Modifier.width(8.dp))
            Text(
                hit.reference,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (hit.snippet.isNotBlank()) {
            Text(
                hit.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun VideoResultRow(video: VideoItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(132.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            DurationBadge(video.durationSeconds)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val secondary = video.seriesTitle ?: video.subtitle
            if (!secondary.isNullOrBlank()) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            TypeChip("Video", Icons.Filled.PlayArrow)
        }
    }
}
