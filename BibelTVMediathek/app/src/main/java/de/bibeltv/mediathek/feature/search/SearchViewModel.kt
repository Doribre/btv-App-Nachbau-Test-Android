package de.bibeltv.mediathek.feature.search

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
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.feature.bible.data.BibleRepository
import de.bibeltv.mediathek.feature.bible.data.BibleSearchHit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: VideoHubRepository,
    private val bibleRepo: BibleRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Suchanfrage überlebt Prozess-Tod & Rotation. */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val results: Flow<PagingData<VideoItem>> = query
        .debounce(350L)
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(pageSize = 24, initialLoadSize = 24, prefetchDistance = 8, enablePlaceholders = false),
                ) {
                    VideoHubPagingSource { skip, take -> repo.searchVideos(q.trim(), skip, take) }
                }.flow
            }
        }
        .cachedIn(viewModelScope)

    /** Bibel-Volltextsuche (GNB), parallel zu den Video-Treffern. */
    val bibleHits: StateFlow<List<BibleSearchHit>> = query
        .debounce(350L)
        .mapLatest { q ->
            if (q.isBlank()) emptyList()
            else runCatching { bibleRepo.searchBible(q.trim()) }.getOrDefault(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    companion object {
        private const val KEY_QUERY = "search_query"
    }
}
