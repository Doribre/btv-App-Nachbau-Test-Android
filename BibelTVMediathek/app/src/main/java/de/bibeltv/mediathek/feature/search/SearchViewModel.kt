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
import de.bibeltv.mediathek.feature.bible.data.BibleBook
import de.bibeltv.mediathek.feature.bible.data.BibleRepository
import de.bibeltv.mediathek.feature.bible.data.BibleSearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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

    /**
     * Beschränkte Top-N-Video-Treffer (ohne Pager) als Merge-Kandidaten. Jede Quelle
     * schluckt Fehler zu emptyList, damit eine langsame/fehlende Quelle die andere nie blockiert.
     */
    private val topVideos: StateFlow<List<VideoItem>> = query
        .debounce(350L)
        .mapLatest { q ->
            if (q.isBlank()) emptyList()
            else runCatching { repo.searchVideos(q.trim(), skip = 0, take = TOP_N) }.getOrDefault(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * EINE gemischte, nach Relevanz sortierte Trefferliste aus Bibel + Mediathek.
     * combine toleriert, dass eine Quelle noch ihre initiale emptyList hält (Laden) oder
     * per runCatching leer ausfällt. Scoring läuft off-main (flowOn Default).
     */
    val merged: StateFlow<List<SearchResultItem>> =
        combine(topVideos, bibleHits, query) { videos, bible, q ->
            if (q.isBlank()) emptyList() else SearchRelevance.merge(videos, bible, q.trim())
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Buch-Katalog (nur Metadaten, kein Verstext) – für die Referenz-Erkennung. */
    private val books: StateFlow<List<BibleBook>> = flow {
        emit(runCatching { bibleRepo.books() }.getOrDefault(emptyList()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Erkennt konkrete Kapitel-Eingaben ("mt 8", "Matthäus 8") -> direkter Absprung in die
     * Bibelthek statt Suchergebnisse. null = keine eindeutige Stelle -> normale gemischte Suche.
     */
    val chapterTarget: StateFlow<ChapterTarget?> =
        combine(query.debounce(450L), books) { q, bookList -> detectChapterTarget(q, bookList) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Synchron aus der AKTUELLEN Eingabe – für den Sprung bei "Enter" (ohne Debounce-Verzögerung). */
    fun currentChapterTarget(): ChapterTarget? = detectChapterTarget(query.value.trim(), books.value)

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    companion object {
        private const val KEY_QUERY = "search_query"
        private const val TOP_N = 40
    }
}
