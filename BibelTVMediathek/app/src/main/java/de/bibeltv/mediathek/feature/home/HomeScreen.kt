package de.bibeltv.mediathek.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.bibeltv.mediathek.R
import de.bibeltv.mediathek.domain.model.ContentRow
import de.bibeltv.mediathek.domain.model.LiveChannel
import de.bibeltv.mediathek.domain.model.VideoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVideoClick: (VideoItem) -> Unit = {},
    onLiveClick: (LiveChannel) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Image(
                            painter = painterResource(R.drawable.ic_btv_star),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(26.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_btv_wordmark),
                            contentDescription = "bibel",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .height(19.dp)
                                .padding(bottom = 2.dp),
                        )
                        Text(
                            text = "APP",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 6.dp, bottom = 1.dp),
                        )
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
                is HomeUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is HomeUiState.Error ->
                    ErrorState(s.message, onRetry = viewModel::load, modifier = Modifier.align(Alignment.Center))
                is HomeUiState.Content ->
                    HomeContent(s, onVideoClick = onVideoClick, onLiveClick = onLiveClick)
            }
        }
    }
}

@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    onVideoClick: (VideoItem) -> Unit,
    onLiveClick: (LiveChannel) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (content.live.isNotEmpty()) {
            item {
                SectionTitle("Live")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(content.live, key = { it.id }) { channel -> LiveCard(channel, onClick = { onLiveClick(channel) }) }
                }
            }
        }
        items(content.rows, key = { it.title }) { row -> ContentRowView(row, onVideoClick = onVideoClick) }
    }
}

@Composable
private fun ContentRowView(row: ContentRow, onVideoClick: (VideoItem) -> Unit) {
    Column {
        SectionTitle(row.title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(row.items, key = { it.crn }) { video -> VideoCardView(video, onClick = { onVideoClick(video) }) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun VideoCardView(video: VideoItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        val secondary = video.seriesTitle ?: video.subtitle
        if (!secondary.isNullOrBlank()) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LiveCard(channel: LiveChannel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .clickable(onClick = onClick),
    ) {
        Box {
            AsyncImage(
                model = channel.thumbnailUrl,
                contentDescription = channel.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Surface(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(8.dp),
            ) {
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = channel.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Konnte nicht laden", style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) { Text("Erneut versuchen") }
    }
}
