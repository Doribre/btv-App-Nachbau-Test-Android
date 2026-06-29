package de.bibeltv.mediathek.domain.model

/** Ein Video, das Verse eines Kapitels referenziert – aus dem VideoHub-GraphQL (VerseItem). */
data class ChapterVideoLink(
    val crn: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val refs: List<VerseRefHit>,
)

/** Eine Vers-Referenz eines Videos mit Sprungmarke. */
data class VerseRefHit(
    val verse: String,       // z. B. "Joh 3,16"
    val timeSeconds: Int,
)
