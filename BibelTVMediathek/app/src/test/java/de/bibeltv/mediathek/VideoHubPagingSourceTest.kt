package de.bibeltv.mediathek

import androidx.paging.PagingSource
import de.bibeltv.mediathek.data.paging.VideoHubPagingSource
import de.bibeltv.mediathek.domain.model.VideoItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoHubPagingSourceTest {

    private fun v(i: Int) = VideoItem(
        id = i, crn = "c$i", title = "t$i", subtitle = null, durationSeconds = 0,
        seriesTitle = null, seasonNumber = null, episodeNumber = null, thumbnailUrl = null, genres = emptyList(),
    )

    private fun refresh(size: Int) = PagingSource.LoadParams.Refresh<Int>(null, size, false)

    @Test
    fun fullFirstPageHasNextKeyAndNoPrevKey() = runTest {
        val source = VideoHubPagingSource { skip, take -> List(take) { v(skip + it) } }
        val result = source.load(refresh(24)) as PagingSource.LoadResult.Page
        assertEquals(24, result.data.size)
        assertNull(result.prevKey)
        assertEquals(24, result.nextKey)
    }

    @Test
    fun shortLastPageHasNoNextKey() = runTest {
        val source = VideoHubPagingSource { _, _ -> List(5) { v(it) } }
        val result = source.load(refresh(24)) as PagingSource.LoadResult.Page
        assertNull(result.nextKey)
    }

    @Test
    fun exceptionBecomesLoadResultError() = runTest {
        val source = VideoHubPagingSource { _, _ -> throw RuntimeException("network") }
        assertTrue(source.load(refresh(24)) is PagingSource.LoadResult.Error)
    }
}
