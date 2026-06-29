package de.bibeltv.mediathek.feature.bible

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.bibeltv.mediathek.feature.bible.data.BibleBook
import de.bibeltv.mediathek.feature.common.ErrorRetry

@Composable
fun BibleBooksScreen(
    onOpenBook: (BibleBook) -> Unit,
    viewModel: BibleBooksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is BooksUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is BooksUiState.Error ->
                ErrorRetry(s.message, onRetry = viewModel::load)
            is BooksUiState.Content -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { SectionHeader("Altes Testament") }
                items(s.at, key = { it.slug }) { book -> BookRow(book, onOpenBook) }
                item { SectionHeader("Neues Testament") }
                items(s.nt, key = { it.slug }) { book -> BookRow(book, onOpenBook) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun BookRow(book: BibleBook, onOpenBook: (BibleBook) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenBook(book) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(book.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (book.chapters == 1) "1 Kapitel" else "${book.chapters} Kapitel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
}
