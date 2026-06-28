package de.bibeltv.mediathek.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import de.bibeltv.mediathek.domain.model.VideoItem

/** 2-spaltiges Paging-Grid mit Lade-/Leer-/Fehler-Zuständen und Append-Retry. */
@Composable
fun PagedVideoGrid(
    items: LazyPagingItems<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    emptyText: String = "Keine Inhalte gefunden.",
) {
    val refresh = items.loadState.refresh
    when {
        refresh is LoadState.Loading && items.itemCount == 0 -> LoadingBox(modifier)
        refresh is LoadState.Error && items.itemCount == 0 ->
            ErrorRetry(refresh.error.message ?: "Netzwerkfehler", onRetry = { items.retry() }, modifier = modifier)
        items.itemCount == 0 -> CenteredHint(emptyText, modifier)
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier,
        ) {
            items(count = items.itemCount, key = items.itemKey { it.crn }) { index ->
                items[index]?.let { video -> VideoGridCard(video, onClick = { onVideoClick(video) }) }
            }
            when (items.loadState.append) {
                is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
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
