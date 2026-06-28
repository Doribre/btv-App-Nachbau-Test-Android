package de.bibeltv.mediathek.feature.home

import de.bibeltv.mediathek.domain.model.ContentRow
import de.bibeltv.mediathek.domain.model.LiveChannel

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val live: List<LiveChannel>,
        val rows: List<ContentRow>,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
