package de.bibeltv.mediathek.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.bibeltv.mediathek.domain.model.VideoDetailModel
import de.bibeltv.mediathek.feature.common.ErrorRetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    onOpenSeries: (Int) -> Unit,
    viewModel: VideoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val titleText = (state as? DetailUiState.Content)?.data?.title.orEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                is DetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is DetailUiState.Error -> ErrorRetry(s.message, onRetry = { viewModel.load() })
                is DetailUiState.Content -> VideoDetailContent(s.data, onPlay, onOpenSeries)
            }
        }
    }
}

@Composable
private fun VideoDetailContent(
    v: VideoDetailModel,
    onPlay: (String) -> Unit,
    onOpenSeries: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AsyncImage(
            model = v.imageUrl,
            contentDescription = v.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(v.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            val meta = buildList {
                v.seriesTitle?.let { add(it) }
                if (v.seasonNumber != null && v.episodeNumber != null) add("S${v.seasonNumber} · E${v.episodeNumber}")
                v.productionYear?.let { add(it.toString()) }
                if (v.durationSeconds > 0) add("${v.durationSeconds / 60000} Min")
                v.fsk?.let { add("FSK $it") }
            }.joinToString("  ·  ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onPlay(v.crn) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abspielen")
                }
                if (v.seriesId != null) {
                    OutlinedButton(onClick = { onOpenSeries(v.seriesId) }) { Text("Zur Serie") }
                }
            }

            v.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (v.genres.isNotEmpty()) {
                Text(
                    text = "Genres: " + v.genres.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
