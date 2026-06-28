package de.bibeltv.mediathek.feature.home

import de.bibeltv.mediathek.domain.model.ContentRow
import de.bibeltv.mediathek.domain.model.LiveChannel
import de.bibeltv.mediathek.domain.model.VideoItem

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val featured: List<VideoItem>,
        val live: List<LiveChannel>,
        val rows: List<ContentRow>,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
