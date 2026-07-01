package de.bibeltv.mediathek.feature.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.bibeltv.mediathek.feature.common.DurationBadge
import de.bibeltv.mediathek.domain.model.ContentRow
import de.bibeltv.mediathek.domain.model.LiveChannel
import de.bibeltv.mediathek.domain.model.VideoItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVideoClick: (VideoItem) -> Unit = {},
    onLiveClick: (LiveChannel) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
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
        if (content.featured.isNotEmpty()) {
            item { HeroCarousel(content.featured, onClick = onVideoClick) }
        }
        if (content.live.isNotEmpty()) {
            item {
                SectionTitle("Live")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(content.live, key = { it.id }) { channel -> LiveCard(channel, onClick = { onLiveClick(channel) }) }
                }
                val context = LocalContext.current
                Text(
                    text = "TV-Programm (EPG) ansehen ›",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bibeltv.de/programm")))
                        },
                )
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
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            DurationBadge(video.durationSeconds)
        }
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

@Composable
private fun HeroCarousel(items: List<VideoItem>, onClick: (VideoItem) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (true) {
                delay(5000)
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val video = items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick(video) },
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    video.seriesTitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val teaser = video.subtitle
                    if (!teaser.isNullOrBlank()) {
                        Text(
                            text = teaser,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Button(onClick = { onClick(video) }, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Ansehen")
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(items.size) { index ->
                val active = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (active) 9.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.4f)),
                )
            }
        }
    }
}
