package de.bibeltv.mediathek.feature.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.bibeltv.mediathek.feature.bible.data.BibleBook
import de.bibeltv.mediathek.feature.bible.data.BibleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BooksUiState {
    data object Loading : BooksUiState
    data class Content(val at: List<BibleBook>, val nt: List<BibleBook>) : BooksUiState
    data class Error(val message: String) : BooksUiState
}

@HiltViewModel
class BibleBooksViewModel @Inject constructor(
    private val repo: BibleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BooksUiState>(BooksUiState.Loading)
    val state: StateFlow<BooksUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = BooksUiState.Loading
            _state.value = runCatching {
                val books = repo.books()
                if (books.isEmpty()) {
                    BooksUiState.Error("Bücher konnten nicht geladen werden.")
                } else {
                    BooksUiState.Content(
                        at = books.filter { it.testament == "AT" },
                        nt = books.filter { it.testament == "NT" },
                    )
                }
            }.getOrElse { BooksUiState.Error(it.message ?: "Keine Verbindung.") }
        }
    }
}
