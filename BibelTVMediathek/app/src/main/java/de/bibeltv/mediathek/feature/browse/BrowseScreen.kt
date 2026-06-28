package de.bibeltv.mediathek.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.feature.common.VideoGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onVideoClick: (VideoItem) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val selected by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val items = viewModel.pages.collectAsLazyPagingItems()

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Entdecken") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selected == null,
                        onClick = { viewModel.selectGenre(null) },
                        label = { Text("Alle") },
                    )
                }
                items(genres) { genre ->
                    FilterChip(
                        selected = selected?.id == genre.id,
                        onClick = { viewModel.selectGenre(genre) },
                        label = { Text(genre.name) },
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = items.itemCount, key = items.itemKey { it.crn }) { index ->
                    val video = items[index]
                    if (video != null) {
                        VideoGridCard(video, onClick = { onVideoClick(video) })
                    }
                }
            }
        }
    }
}
