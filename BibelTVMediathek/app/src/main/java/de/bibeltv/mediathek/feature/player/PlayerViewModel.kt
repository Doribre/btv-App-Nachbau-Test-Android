package de.bibeltv.mediathek.feature.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.bibeltv.mediathek.data.repository.VideoHubRepository
import de.bibeltv.mediathek.domain.model.PlayoutSource
import de.bibeltv.mediathek.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data object Ready : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repo: VideoHubRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: Route.Player = savedStateHandle.toRoute()
    val title: String = args.title

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply { playWhenReady = true }

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _state.value = PlayerUiState.Error("Wiedergabe nicht möglich (${error.errorCodeName}).")
            }
        })
        viewModelScope.launch {
            val source: PlayoutSource? = if (args.isLive) {
                runCatching { repo.liveSource(args.liveId) }.getOrNull()
            } else {
                runCatching { repo.videoPlayout(args.crn) }.getOrNull()
            }
            if (source == null || source.url.isBlank()) {
                _state.value = PlayerUiState.Error("Keine abspielbare Quelle gefunden.")
                return@launch
            }
            val item = MediaItem.Builder().setUri(source.url).apply {
                source.mimeType?.let { setMimeType(it) }
                source.widevineLicenseUrl?.takeIf { it.isNotBlank() }?.let { license ->
                    setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(license)
                            .build(),
                    )
                }
            }.build()
            if (args.startSeconds > 0) {
                player.setMediaItem(item, args.startSeconds * 1000L)
            } else {
                player.setMediaItem(item)
            }
            player.prepare()
            _state.value = PlayerUiState.Ready
        }
    }

    override fun onCleared() {
        player.pause()
        player.release()
    }
}
