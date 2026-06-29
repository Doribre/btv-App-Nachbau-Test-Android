package de.bibeltv.mediathek.feature.bible.data

/** Ein Buch der Bibel (GNB). Katalog wird zur Laufzeit geladen, nicht gespeichert. */
data class BibleBook(
    val slug: String,        // z. B. "1-mose", "joh"
    val name: String,        // Anzeigename, z. B. "1. Mose", "Johannes"
    val title: String,       // Langtitel, z. B. "1. Mose (Genesis)"
    val chapters: Int,       // Anzahl Kapitel
    val testament: String,   // "AT" oder "NT"
)

/** Ein mit einem Vers/Kapitel verknüpftes Video samt Sprungmarke. */
data class VerseVideo(
    val crn: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val timeSeconds: Int,        // Sprungposition im Video
    val formattedTime: String,   // z. B. "12:30"
)

/** Ein einzelner Vers. */
data class BibleVerse(
    val number: String,            // Anzeige: "16", "2a", "1-2"
    val numbers: Set<Int>,         // abgedeckte Versnummern (für Video-Zuordnung)
    val text: String,              // Verstext (Poesie-Zeilen via \n erhalten)
    val audioUrl: String?,         // Vorlese-mp3 (live), kein Download
    val heading: String?,          // optionale Abschnittsüberschrift davor
    val videos: List<VerseVideo>,  // verknüpfte Videos (im Repository gefüllt)
)

/** Ein geladenes Kapitel. Lebt nur im Speicher während der Anzeige. */
data class BibleChapter(
    val bookSlug: String,
    val bookName: String,
    val chapter: Int,
    val verses: List<BibleVerse>,
    val chapterVideos: List<VerseVideo>,  // Videos, die das ganze Kapitel betreffen
)
