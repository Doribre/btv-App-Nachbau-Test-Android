package de.bibeltv.mediathek.feature.search

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: VideoHubRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: Flow<PagingData<VideoItem>> = _query
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

    fun onQueryChange(value: String) {
        _query.value = value
    }
}
