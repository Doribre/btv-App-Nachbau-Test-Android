package de.bibeltv.mediathek.data.mapper

import de.bibeltv.mediathek.domain.model.LiveChannel
import de.bibeltv.mediathek.domain.model.PlayoutSource
import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.graphql.LiveStreamsQuery
import de.bibeltv.mediathek.graphql.VideoPlayoutQuery
import de.bibeltv.mediathek.graphql.fragment.VideoCard

private const val IMGIX_BASE = "https://bibeltv.imgix.net/"

/** Baut eine ladbare Bild-URL: absolute URLs unverändert, relative Pfade über das imgix-CDN. */
fun imageUrl(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return if (raw.startsWith("http", ignoreCase = true)) {
        raw
    } else {
        IMGIX_BASE + raw.trimStart('/') + "?w=640&auto=format,compress"
    }
}

private fun bestThumbnail(images: List<VideoCard.Image>): String? =
    imageUrl(images.firstOrNull { it.url.isNotBlank() }?.url)

fun VideoCard.toDomain(): VideoItem = VideoItem(
    id = id,
    crn = crn,
    title = title,
    subtitle = subtitle.ifBlank { null },
    durationSeconds = duration,
    seriesTitle = serie?.title,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    thumbnailUrl = bestThumbnail(images),
    genres = genres.map { it.name },
)

fun LiveStreamsQuery.LiveStream.toDomain(): LiveChannel = LiveChannel(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = imageUrl(staticThumbnail ?: liveThumbnail),
    hlsUrl = streamUrlHls,
)

private fun mimeFor(type: String?): String? = when {
    type == null -> null
    type.contains("mpegurl", true) || type.contains("m3u8", true) || type.equals("hls", true) -> "application/x-mpegURL"
    type.contains("dash", true) || type.contains("mpd", true) -> "application/dash+xml"
    type.contains("mp4", true) -> "video/mp4"
    else -> null
}

fun VideoPlayoutQuery.Video.toPlayoutSource(): PlayoutSource? {
    val urls = videoUrls.orEmpty().filterNotNull().filter { !it.src.isNullOrBlank() }
    if (urls.isEmpty()) return null
    val chosen = urls.firstOrNull { mimeFor(it.type) == "application/x-mpegURL" }
        ?: urls.firstOrNull { mimeFor(it.type) == "application/dash+xml" }
        ?: urls.first()
    return PlayoutSource(
        url = chosen.src!!,
        mimeType = mimeFor(chosen.type),
        widevineLicenseUrl = chosen.keySystem?.widevine?.url,
    )
}
