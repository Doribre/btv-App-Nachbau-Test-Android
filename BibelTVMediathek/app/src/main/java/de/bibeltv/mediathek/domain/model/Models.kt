package de.bibeltv.mediathek.domain.model

/** Saubere, Apollo-freie Domänenmodelle. Die UI kennt nur diese Typen. */

data class VideoItem(
    val id: Int,
    val crn: String,
    val title: String,
    val subtitle: String?,
    val durationSeconds: Int,
    val seriesTitle: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val thumbnailUrl: String?,
    val genres: List<String>,
)

data class LiveChannel(
    val id: Int,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val hlsUrl: String?,
)

data class GenreItem(
    val id: Int,
    val name: String,
    val videoCount: Int,
)

/** Eine horizontale Reihe auf der Startseite. */
data class ContentRow(
    val title: String,
    val items: List<VideoItem>,
)

/** Abspielbare Quelle (URL + MIME + optionale Widevine-Lizenz-URL). */
data class PlayoutSource(
    val url: String,
    val mimeType: String?,
    val widevineLicenseUrl: String?,
)

data class VideoDetailModel(
    val crn: String,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val durationSeconds: Int,
    val seriesTitle: String?,
    val seriesId: Int?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val productionYear: Int?,
    val fsk: String?,
    val imageUrl: String?,
    val genres: List<String>,
)

data class SeriesDetailModel(
    val id: Int,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val episodes: List<VideoItem>,
)
