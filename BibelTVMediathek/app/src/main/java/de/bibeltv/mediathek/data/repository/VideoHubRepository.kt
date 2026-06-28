package de.bibeltv.mediathek.data.repository

import com.apollographql.apollo.ApolloClient
import de.bibeltv.mediathek.data.mapper.toDomain
import de.bibeltv.mediathek.domain.model.GenreItem
import de.bibeltv.mediathek.domain.model.LiveChannel
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.domain.model.PlayoutSource
import de.bibeltv.mediathek.data.mapper.toPlayoutSource
import de.bibeltv.mediathek.graphql.GenreVideosQuery
import de.bibeltv.mediathek.graphql.GenresQuery
import de.bibeltv.mediathek.graphql.LiveStreamByIdQuery
import de.bibeltv.mediathek.graphql.LiveStreamsQuery
import de.bibeltv.mediathek.graphql.NewestVideosQuery
import de.bibeltv.mediathek.graphql.VideoPlayoutQuery
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Dünne Repository-Schicht über Apollo. Liefert ausschließlich Domänenmodelle. */
@Singleton
class VideoHubRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    private fun nowIso(): String = Instant.now().toString()

    suspend fun liveStreams(): List<LiveChannel> =
        apollo.query(LiveStreamsQuery()).execute()
            .dataOrThrow().liveStreams.map { it.toDomain() }

    suspend fun newestVideos(take: Int = 20): List<VideoItem> =
        apollo.query(NewestVideosQuery(take = take, skip = 0, now = nowIso())).execute()
            .dataOrThrow().videos.map { it.videoCard.toDomain() }

    suspend fun genres(): List<GenreItem> =
        apollo.query(GenresQuery()).execute()
            .dataOrThrow().genres.map { GenreItem(it.id, it.name, it.videoCount ?: 0) }

    suspend fun videosByGenre(genreId: Int, take: Int = 15): List<VideoItem> =
        apollo.query(GenreVideosQuery(genreId = genreId, take = take, skip = 0, now = nowIso())).execute()
            .dataOrThrow().genre?.videos?.map { it.videoCard.toDomain() } ?: emptyList()

    suspend fun videoPlayout(crn: String): PlayoutSource? =
        apollo.query(VideoPlayoutQuery(crn = crn)).execute()
            .dataOrThrow().videos.firstOrNull()?.toPlayoutSource()

    suspend fun liveSource(id: Int): PlayoutSource? {
        val ls = apollo.query(LiveStreamByIdQuery(id = id)).execute().dataOrThrow().liveStream ?: return null
        val url = ls.streamUrlHls?.takeIf { it.isNotBlank() } ?: ls.streamUrlDash?.takeIf { it.isNotBlank() } ?: return null
        val mime = if (!ls.streamUrlHls.isNullOrBlank()) "application/x-mpegURL" else "application/dash+xml"
        return PlayoutSource(url = url, mimeType = mime, widevineLicenseUrl = null)
    }
}
