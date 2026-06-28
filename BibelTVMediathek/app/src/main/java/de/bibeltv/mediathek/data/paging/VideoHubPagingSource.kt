package de.bibeltv.mediathek.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import de.bibeltv.mediathek.domain.model.VideoItem

/** Generische Offset-Paging-Quelle (Prisma take/skip) für Browse/Suche. */
class VideoHubPagingSource(
    private val fetch: suspend (skip: Int, take: Int) -> List<VideoItem>,
) : PagingSource<Int, VideoItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VideoItem> {
        val offset = params.key ?: 0
        return try {
            val items = fetch(offset, params.loadSize)
            LoadResult.Page(
                data = items,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                nextKey = if (items.size < params.loadSize) null else offset + params.loadSize,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, VideoItem>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(state.config.pageSize) ?: page?.nextKey?.minus(state.config.pageSize)
        }
}
