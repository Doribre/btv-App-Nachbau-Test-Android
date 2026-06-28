package de.bibeltv.mediathek.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.bibeltv.mediathek.data.repository.VideoHubRepository
import de.bibeltv.mediathek.domain.model.SeriesDetailModel
import de.bibeltv.mediathek.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val repo: VideoHubRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: Route.SeriesDetail = savedStateHandle.toRoute()

    private val _state = MutableStateFlow<DetailUiState<SeriesDetailModel>>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState<SeriesDetailModel>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = DetailUiState.Loading
            val result = runCatching { repo.seriesDetail(args.id) }.getOrNull()
            _state.value = result?.let { DetailUiState.Content(it) }
                ?: DetailUiState.Error("Serie nicht gefunden.")
        }
    }
}
