package de.bibeltv.mediathek.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.bibeltv.mediathek.data.repository.VideoHubRepository
import de.bibeltv.mediathek.domain.model.ContentRow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: VideoHubRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // Kuratierte Reihen anhand der echten VideoHub-Genres.
    private val curatedGenres = listOf(
        "Spielfilm", "Serie", "Predigt", "Gottesdienst", "Andacht", "Kinder", "Doku", "Musik",
    )

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            try {
                val state = coroutineScope {
                    val liveDef = async { runCatching { repo.liveStreams() }.getOrDefault(emptyList()) }
                    val newestDef = async { runCatching { repo.newestVideos(20) }.getOrDefault(emptyList()) }
                    val allGenres = runCatching { repo.genres() }.getOrDefault(emptyList())
                    val picked = curatedGenres.mapNotNull { wanted ->
                        allGenres.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
                    }
                    val railDefs = picked.map { genre ->
                        genre to async { runCatching { repo.videosByGenre(genre.id, 15) }.getOrDefault(emptyList()) }
                    }
                    val rows = buildList {
                        val newest = newestDef.await()
                        if (newest.isNotEmpty()) add(ContentRow("Neu in der Mediathek", newest))
                        railDefs.forEach { (genre, def) ->
                            val items = def.await()
                            if (items.isNotEmpty()) add(ContentRow(genre.name, items))
                        }
                    }
                    val live = liveDef.await()
                    if (rows.isEmpty() && live.isEmpty()) {
                        HomeUiState.Error("Keine Inhalte geladen.")
                    } else {
                        HomeUiState.Content(live = live, rows = rows)
                    }
                }
                _state.value = state
            } catch (e: Exception) {
                _state.value = HomeUiState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
}
