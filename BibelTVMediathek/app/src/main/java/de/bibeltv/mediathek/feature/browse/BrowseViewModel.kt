package de.bibeltv.mediathek.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import de.bibeltv.mediathek.data.paging.VideoHubPagingSource
import de.bibeltv.mediathek.data.repository.VideoHubRepository
import de.bibeltv.mediathek.domain.model.GenreItem
import de.bibeltv.mediathek.domain.model.VideoItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repo: VideoHubRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _genres = MutableStateFlow<List<GenreItem>>(emptyList())
    val genres: StateFlow<List<GenreItem>> = _genres.asStateFlow()

    /** Überlebt Prozess-Tod & Rotation. ALL (-1) = alle Inhalte. */
    val selectedGenreId: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_GENRE, ALL)

    val pages: Flow<PagingData<VideoItem>> = selectedGenreId.flatMapLatest { id ->
        Pager(
            config = PagingConfig(pageSize = 24, initialLoadSize = 48, prefetchDistance = 8, enablePlaceholders = false),
        ) {
            VideoHubPagingSource { skip, take ->
                if (id == ALL) repo.browseNewest(skip, take) else repo.browseByGenre(id, skip, take)
            }
        }.flow
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _genres.value = runCatching { repo.genres() }.getOrDefault(emptyList())
                .filter { it.videoCount > 0 }
                .sortedByDescending { it.videoCount }
        }
    }

    fun selectGenre(id: Int) {
        savedStateHandle[KEY_GENRE] = id
    }

    companion object {
        const val ALL = -1
        private const val KEY_GENRE = "browse_genre_id"
    }
}
