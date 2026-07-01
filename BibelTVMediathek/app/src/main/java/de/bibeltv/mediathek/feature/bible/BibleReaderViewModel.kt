package de.bibeltv.mediathek.feature.bible

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.bibeltv.mediathek.feature.bible.data.BibleChapter
import de.bibeltv.mediathek.feature.bible.data.BibleRepository
import de.bibeltv.mediathek.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChapterUiState {
    data object Loading : ChapterUiState
    data class Content(val chapter: BibleChapter) : ChapterUiState
    data class Error(val message: String) : ChapterUiState
}

enum class ReaderFontScale(val sp: Int) { SMALL(17), MEDIUM(20), LARGE(24) }

@HiltViewModel
class BibleReaderViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repo: BibleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: Route.BibleReader = savedStateHandle.toRoute()
    val bookSlug: String = args.bookSlug
    val bookName: String = args.bookName

    private val _state = MutableStateFlow<ChapterUiState>(ChapterUiState.Loading)
    val state: StateFlow<ChapterUiState> = _state.asStateFlow()

    private val _currentChapter = MutableStateFlow(args.chapter)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _maxChapters = MutableStateFlow(args.chapter.coerceAtLeast(1))
    val maxChapters: StateFlow<Int> = _maxChapters.asStateFlow()

    private val _fontScale = MutableStateFlow(ReaderFontScale.MEDIUM)
    val fontScale: StateFlow<ReaderFontScale> = _fontScale.asStateFlow()

    private val _playingVerse = MutableStateFlow<String?>(null)
    val playingVerse: StateFlow<String?> = _playingVerse.asStateFlow()

    /** Steuert, ob pro Vers die Video-Anzahl (▸N) eingeblendet wird. */
    private val _showVideoBadges = MutableStateFlow(true)
    val showVideoBadges: StateFlow<Boolean> = _showVideoBadges.asStateFlow()

    fun toggleVideoBadges() { _showVideoBadges.value = !_showVideoBadges.value }

    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            _maxChapters.value = runCatching {
                repo.books().firstOrNull { it.slug == args.bookSlug }?.chapters
            }.getOrNull() ?: args.chapter
        }
        load(args.chapter)
    }

    fun load(chapter: Int) {
        stopAudio()
        _currentChapter.value = chapter
        viewModelScope.launch {
            _state.value = ChapterUiState.Loading
            _state.value = runCatching {
                ChapterUiState.Content(repo.chapter(args.bookSlug, args.bookName, chapter))
            }.getOrElse { ChapterUiState.Error(it.message ?: "Kapitel konnte nicht geladen werden.") }
        }
    }

    fun nextChapter() {
        val c = _currentChapter.value
        if (c < _maxChapters.value) load(c + 1)
    }

    fun previousChapter() {
        val c = _currentChapter.value
        if (c > 1) load(c - 1)
    }

    fun goToChapter(chapter: Int) {
        if (chapter != _currentChapter.value) load(chapter)
    }

    fun cycleFontScale() {
        _fontScale.value = when (_fontScale.value) {
            ReaderFontScale.SMALL -> ReaderFontScale.MEDIUM
            ReaderFontScale.MEDIUM -> ReaderFontScale.LARGE
            ReaderFontScale.LARGE -> ReaderFontScale.SMALL
        }
    }

    /** Spielt das Vorlese-mp3 eines Verses live ab (kein Download). */
    fun toggleVerseAudio(verseNumber: String, url: String) {
        if (_playingVerse.value == verseNumber) {
            stopAudio()
            return
        }
        stopAudio()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    _playingVerse.value = null
                    it.release()
                    if (mediaPlayer === it) mediaPlayer = null
                }
                setOnErrorListener { _, _, _ ->
                    _playingVerse.value = null
                    true
                }
                prepareAsync()
            }
            _playingVerse.value = verseNumber
        }.onFailure { _playingVerse.value = null }
    }

    fun stopAudio() {
        mediaPlayer?.let { runCatching { it.reset(); it.release() } }
        mediaPlayer = null
        _playingVerse.value = null
    }

    override fun onCleared() {
        stopAudio()
    }
}
