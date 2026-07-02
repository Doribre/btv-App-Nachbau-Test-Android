package de.bibeltv.mediathek.feature.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.bibeltv.mediathek.feature.bible.data.BibleChapter
import de.bibeltv.mediathek.feature.bible.data.BibleVerse
import de.bibeltv.mediathek.feature.bible.data.VerseVideo
import de.bibeltv.mediathek.feature.common.ErrorRetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    onBack: () -> Unit,
    onPlayVideo: (crn: String, startSeconds: Int) -> Unit,
    viewModel: BibleReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val chapter by viewModel.currentChapter.collectAsStateWithLifecycle()
    val maxChapters by viewModel.maxChapters.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val playingVerse by viewModel.playingVerse.collectAsStateWithLifecycle()
    val showVideoBadges by viewModel.showVideoBadges.collectAsStateWithLifecycle()

    var selectedVerse by remember { mutableStateOf<BibleVerse?>(null) }
    var showChapterPicker by remember { mutableStateOf(false) }
    var showChapterVideos by remember { mutableStateOf(false) }

    val content = state as? ChapterUiState.Content
    val chapterVideos = content?.chapter?.chapterVideos.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showChapterPicker = true },
                    ) {
                        Text("${viewModel.bookName} $chapter", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Kapitel wählen")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::cycleFontScale) {
                        Icon(Icons.Filled.FormatSize, contentDescription = "Schriftgröße")
                    }
                    IconButton(onClick = viewModel::toggleVideoBadges) {
                        Icon(
                            Icons.Filled.OndemandVideo,
                            contentDescription = "Video-Anzahl je Vers anzeigen",
                            tint = if (showVideoBadges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (chapterVideos.isNotEmpty()) {
                        IconButton(onClick = { showChapterVideos = true }) {
                            BadgedBox(badge = { Badge { Text("${chapterVideos.size}") } }) {
                                Icon(Icons.Filled.OndemandVideo, contentDescription = "Videos zum Kapitel")
                            }
                        }
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
                is ChapterUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ChapterUiState.Error ->
                    ErrorRetry(s.message, onRetry = { viewModel.load(chapter) })
                is ChapterUiState.Content ->
                    ReaderContent(
                        chapter = s.chapter,
                        fontSizeSp = fontScale.sp,
                        showVideoBadges = showVideoBadges,
                        onVerseClick = { selectedVerse = it },
                        canPrev = chapter > 1,
                        canNext = chapter < maxChapters,
                        onPrev = viewModel::previousChapter,
                        onNext = viewModel::nextChapter,
                    )
            }
        }
    }

    selectedVerse?.let { verse ->
        ModalBottomSheet(
            onDismissRequest = { selectedVerse = null; viewModel.stopAudio() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            VerseSheet(
                bookName = viewModel.bookName,
                chapter = chapter,
                verse = verse,
                isPlaying = playingVerse == verse.number,
                onToggleAudio = { url -> viewModel.toggleVerseAudio(verse.number, url) },
                onPlayVideo = { crn, t -> selectedVerse = null; viewModel.stopAudio(); onPlayVideo(crn, t) },
            )
        }
    }

    if (showChapterVideos && chapterVideos.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showChapterVideos = false }) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    "Videos zu ${viewModel.bookName} $chapter",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                chapterVideos.forEach { video ->
                    VideoRow(video) { showChapterVideos = false; onPlayVideo(video.crn, video.timeSeconds) }
                }
            }
        }
    }

    if (showChapterPicker) {
        ModalBottomSheet(onDismissRequest = { showChapterPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${viewModel.bookName} – Kapitel",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items((1..maxChapters).toList()) { n ->
                        val selected = n == chapter
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { showChapterPicker = false; viewModel.goToChapter(n) },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$n",
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderContent(
    chapter: BibleChapter,
    fontSizeSp: Int,
    showVideoBadges: Boolean,
    onVerseClick: (BibleVerse) -> Unit,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val headingColor = MaterialTheme.colorScheme.primary

    val inlineContent = LinkedHashMap<String, InlineTextContent>()
    val text = buildAnnotatedString {
        chapter.verses.forEachIndexed { index, verse ->
            verse.heading?.let { h ->
                if (length > 0) append("\n\n")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = headingColor)) { append(h) }
                append("\n")
            }
            val hasVideos = verse.videos.isNotEmpty()
            val highlight = hasVideos && showVideoBadges
            withLink(
                LinkAnnotation.Clickable(
                    tag = "verse-${verse.number}",
                    linkInteractionListener = LinkInteractionListener { onVerseClick(verse) },
                ),
            ) {
                withStyle(
                    SpanStyle(
                        baselineShift = BaselineShift.Superscript,
                        fontSize = (fontSizeSp * 0.62f).sp,
                        color = if (highlight) accent else muted,
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                    ),
                ) {
                    append(verse.number)
                }
            }
            // Deutlich sichtbarer, tippbarer Video-Pill dort, wo es Videos gibt.
            if (highlight) {
                append(" ")
                val count = verse.videos.size
                val id = "vp-$index"
                appendInlineContent(id, "Videos")
                inlineContent[id] = InlineTextContent(
                    Placeholder(
                        width = (1.6f + 0.55f * count.toString().length).em,
                        height = 1.35.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    VerseVideoPill(count = count, fontSizeSp = fontSizeSp) { onVerseClick(verse) }
                }
            }
            append(" ")
            append(verse.text)
            if (index < chapter.verses.lastIndex) append("  ")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            inlineContent = inlineContent,
            fontFamily = FontFamily.Serif,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.6f).sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onPrev, enabled = canPrev, modifier = Modifier.weight(1f)) {
                Text("Zurück")
            }
            Button(onClick = onNext, enabled = canNext, modifier = Modifier.weight(1f)) {
                Text("Weiter")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VerseSheet(
    bookName: String,
    chapter: Int,
    verse: BibleVerse,
    isPlaying: Boolean,
    onToggleAudio: (String) -> Unit,
    onPlayVideo: (String, Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Text(
            "$bookName $chapter,${verse.number}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            verse.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        verse.audioUrl?.let { url ->
            TextButton(onClick = { onToggleAudio(url) }) {
                Icon(if (isPlaying) Icons.Filled.Stop else Icons.Filled.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPlaying) "Vorlesen stoppen" else "Vers vorlesen")
            }
        }
        if (verse.videos.isEmpty()) {
            Text(
                "Zu diesem Vers gibt es noch keine Videos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            Text(
                "Videos zu diesem Vers",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            verse.videos.forEach { video ->
                VideoRow(video) { onPlayVideo(video.crn, video.timeSeconds) }
            }
        }
    }
}

/** Grüner, tippbarer Pill im Bibeltext, der klar signalisiert: zu diesem Vers gibt es Videos. */
@Composable
private fun VerseVideoPill(count: Int, fontSizeSp: Int, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "▶$count",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = (fontSizeSp * 0.5f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun VideoRow(video: VerseVideo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
            )
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            video.subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "springt zu ${video.formattedTime}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
