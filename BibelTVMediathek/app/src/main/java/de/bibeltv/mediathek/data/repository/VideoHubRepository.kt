package de.bibeltv.mediathek.data.repository

import com.apollographql.apollo.ApolloClient
import de.bibeltv.mediathek.data.mapper.toDetail
import de.bibeltv.mediathek.data.mapper.toDomain
import de.bibeltv.mediathek.data.mapper.toPlayoutSource
import de.bibeltv.mediathek.domain.model.GenreItem
import de.bibeltv.mediathek.domain.model.LiveChannel
import de.bibeltv.mediathek.domain.model.PlayoutSource
import de.bibeltv.mediathek.domain.model.SeriesDetailModel
import de.bibeltv.mediathek.domain.model.VideoDetailModel
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.graphql.GenreVideosQuery
import de.bibeltv.mediathek.graphql.GenresQuery
import de.bibeltv.mediathek.graphql.LiveStreamByIdQuery
import de.bibeltv.mediathek.graphql.LiveStreamsQuery
import de.bibeltv.mediathek.graphql.NewestVideosQuery
import de.bibeltv.mediathek.graphql.SearchVideosQuery
import de.bibeltv.mediathek.graphql.SeriesDetailQuery
import de.bibeltv.mediathek.graphql.VideoDetailQuery
import de.bibeltv.mediathek.graphql.VideoPlayoutQuery
import kotlinx.coroutines.delay
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Dünne Repository-Schicht über Apollo. Liefert ausschließlich Domänenmodelle. */
@Singleton
class VideoHubRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    private fun nowIso(): String = Instant.now().toString()

    /**
     * Wiederholt transiente Fehler (z. B. 504 Gateway Timeout von Beta) mit Backoff.
     * Der VideoHub-Beta antwortet sporadisch mit 504 – ein zweiter Versuch greift dann meist sofort.
     */
    private suspend fun <T> withRetry(attempts: Int = 3, block: suspend () -> T): T {
        var lastError: Exception? = null
        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt < attempts - 1) delay(400L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Anfrage fehlgeschlagen.")
    }

    suspend fun liveStreams(): List<LiveChannel> = withRetry {
        apollo.query(LiveStreamsQuery()).execute()
            .dataOrThrow().liveStreams.map { it.toDomain() }
    }

    suspend fun newestVideos(take: Int = 20): List<VideoItem> = withRetry {
        apollo.query(NewestVideosQuery(take = take, skip = 0, now = nowIso())).execute()
            .dataOrThrow().videos.map { it.videoCard.toDomain() }
    }

    suspend fun genres(): List<GenreItem> = withRetry {
        apollo.query(GenresQuery()).execute()
            .dataOrThrow().genres.map { GenreItem(it.id, it.name, it.videoCount ?: 0) }
    }

    suspend fun videosByGenre(genreId: Int, take: Int = 15): List<VideoItem> = withRetry {
        apollo.query(GenreVideosQuery(genreId = genreId, take = take, skip = 0, now = nowIso())).execute()
            .dataOrThrow().genre?.videos?.map { it.videoCard.toDomain() } ?: emptyList()
    }

    suspend fun browseNewest(skip: Int, take: Int): List<VideoItem> = withRetry {
        apollo.query(NewestVideosQuery(take = take, skip = skip, now = nowIso())).execute()
            .dataOrThrow().videos.map { it.videoCard.toDomain() }
    }

    suspend fun browseByGenre(genreId: Int, skip: Int, take: Int): List<VideoItem> = withRetry {
        apollo.query(GenreVideosQuery(genreId = genreId, take = take, skip = skip, now = nowIso())).execute()
            .dataOrThrow().genre?.videos?.map { it.videoCard.toDomain() } ?: emptyList()
    }

    suspend fun searchVideos(query: String, skip: Int, take: Int): List<VideoItem> = withRetry {
        apollo.query(SearchVideosQuery(q = query, take = take, skip = skip, now = nowIso())).execute()
            .dataOrThrow().videos.map { it.videoCard.toDomain() }
    }

    suspend fun videoDetail(crn: String): VideoDetailModel? = withRetry {
        apollo.query(VideoDetailQuery(crn = crn)).execute()
            .dataOrThrow().videos.firstOrNull()?.toDetail()
    }

    suspend fun seriesDetail(id: Int): SeriesDetailModel? = withRetry {
        apollo.query(SeriesDetailQuery(id = id, now = nowIso())).execute()
            .dataOrThrow().serie?.toDetail()
    }

    suspend fun videoPlayout(crn: String): PlayoutSource? = withRetry {
        apollo.query(VideoPlayoutQuery(crn = crn)).execute()
            .dataOrThrow().videos.firstOrNull()?.toPlayoutSource()
    }

    suspend fun liveSource(id: Int): PlayoutSource? = withRetry {
        val ls = apollo.query(LiveStreamByIdQuery(id = id)).execute().dataOrThrow().liveStream ?: return@withRetry null
        val url = ls.streamUrlHls?.takeIf { it.isNotBlank() } ?: ls.streamUrlDash?.takeIf { it.isNotBlank() } ?: return@withRetry null
        val mime = if (!ls.streamUrlHls.isNullOrBlank()) "application/x-mpegURL" else "application/dash+xml"
        PlayoutSource(url = url, mimeType = mime, widevineLicenseUrl = null)
    }
}
