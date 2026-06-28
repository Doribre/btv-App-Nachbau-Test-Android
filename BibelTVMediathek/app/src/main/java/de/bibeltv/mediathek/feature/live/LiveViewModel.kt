package de.bibeltv.mediathek.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.bibeltv.mediathek.data.repository.VideoHubRepository
import de.bibeltv.mediathek.domain.model.LiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LiveUiState {
    data object Loading : LiveUiState
    data class Content(val channels: List<LiveChannel>) : LiveUiState
    data class Error(val message: String) : LiveUiState
}

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val repo: VideoHubRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LiveUiState>(LiveUiState.Loading)
    val state: StateFlow<LiveUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = LiveUiState.Loading
            val result = runCatching { repo.liveStreams() }.getOrNull()
            _state.value = when {
                result == null -> LiveUiState.Error("Live-Kanäle konnten nicht geladen werden.")
                result.isEmpty() -> LiveUiState.Error("Aktuell keine Live-Kanäle online.")
                else -> LiveUiState.Content(result)
            }
        }
    }
}
