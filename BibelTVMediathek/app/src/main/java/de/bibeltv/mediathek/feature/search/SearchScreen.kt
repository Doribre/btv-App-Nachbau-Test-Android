package de.bibeltv.mediathek.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.feature.common.CenteredHint
import de.bibeltv.mediathek.feature.common.PagedVideoGrid

@Composable
fun SearchScreen(
    onVideoClick: (VideoItem) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val items = viewModel.results.collectAsLazyPagingItems()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Titel, Serie, Thema …") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (query.isBlank()) {
            CenteredHint("Suche nach Titel, Serie, Thema oder Genre …")
        } else {
            PagedVideoGrid(
                items = items,
                onVideoClick = onVideoClick,
                emptyText = "Keine Treffer für „$query“.",
            )
        }
    }
}
