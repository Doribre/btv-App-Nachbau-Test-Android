package de.bibeltv.mediathek.feature.detail

sealed interface DetailUiState<out T> {
    data object Loading : DetailUiState<Nothing>
    data class Content<T>(val data: T) : DetailUiState<T>
    data class Error(val message: String) : DetailUiState<Nothing>
}
