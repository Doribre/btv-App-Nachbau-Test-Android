package de.bibeltv.mediathek.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.feature.common.PagedVideoGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onVideoClick: (VideoItem) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedGenreId.collectAsStateWithLifecycle()
    val items = viewModel.pages.collectAsLazyPagingItems()

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Entdecken") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedId == BrowseViewModel.ALL,
                        onClick = { viewModel.selectGenre(BrowseViewModel.ALL) },
                        label = { Text("Alle") },
                    )
                }
                items(genres, key = { it.id }) { genre ->
                    FilterChip(
                        selected = selectedId == genre.id,
                        onClick = { viewModel.selectGenre(genre.id) },
                        label = { Text(genre.name) },
                    )
                }
            }
            PagedVideoGrid(
                items = items,
                onVideoClick = onVideoClick,
                emptyText = "In dieser Kategorie gibt es noch nichts.",
            )
        }
    }
}
