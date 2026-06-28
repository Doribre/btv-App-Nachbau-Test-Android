package de.bibeltv.mediathek.feature.browse

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
) : ViewModel() {

    private val _genres = MutableStateFlow<List<GenreItem>>(emptyList())
    val genres: StateFlow<List<GenreItem>> = _genres.asStateFlow()

    private val _selectedGenre = MutableStateFlow<GenreItem?>(null)
    val selectedGenre: StateFlow<GenreItem?> = _selectedGenre.asStateFlow()

    val pages: Flow<PagingData<VideoItem>> = _selectedGenre.flatMapLatest { genre ->
        Pager(
            config = PagingConfig(pageSize = 24, initialLoadSize = 48, prefetchDistance = 8, enablePlaceholders = false),
        ) {
            VideoHubPagingSource { skip, take ->
                if (genre == null) repo.browseNewest(skip, take) else repo.browseByGenre(genre.id, skip, take)
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

    fun selectGenre(genre: GenreItem?) {
        _selectedGenre.value = genre
    }
}
