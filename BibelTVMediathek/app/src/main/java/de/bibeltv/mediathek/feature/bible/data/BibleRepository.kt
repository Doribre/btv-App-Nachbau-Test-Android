package de.bibeltv.mediathek.feature.bible.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bibelthek-Repository. Lädt Buchkatalog und Kapitel LIVE und löst die Vers↔Video-Zuordnung auf.
 * Bibeltext wird NIE persistiert; nur der Buchkatalog (reine Metadaten) wird für die Sitzung im
 * Speicher gehalten, um nicht bei jedem Blättern erneut zu laden.
 */
@Singleton
class BibleRepository @Inject constructor(
    private val remote: BibleRemoteDataSource,
) {
    @Volatile
    private var cachedBooks: List<BibleBook>? = null

    suspend fun books(): List<BibleBook> {
        cachedBooks?.let { return it }
        val books = BibleHtmlParser.parseCatalog(remote.fetchCatalogHtml())
        if (books.isNotEmpty()) cachedBooks = books
        return books
    }

    /** Lädt ein Kapitel frisch aus dem Netz und verknüpft die Videos pro Vers. */
    suspend fun chapter(bookSlug: String, bookName: String, chapter: Int): BibleChapter {
        val parsed = BibleHtmlParser.parseChapter(remote.fetchChapterHtml(bookSlug, chapter))
        return resolve(bookSlug, bookName, chapter, parsed)
    }

    private fun resolve(
        bookSlug: String,
        bookName: String,
        chapter: Int,
        parsed: BibleHtmlParser.ParsedChapter,
    ): BibleChapter {
        // verseNumber -> (crn -> Video), dedupliziert; kleinste Zeit gewinnt als Sprungstart.
        val perVerse = HashMap<String, LinkedHashMap<String, VerseVideo>>()
        val chapterVids = LinkedHashMap<String, VerseVideo>()

        for (video in parsed.videos) {
            for (item in video.versesItems) {
                val ref = BibleReference.parse(item.verse) ?: continue
                if (ref.slug != bookSlug || ref.chapter != chapter) continue
                if (ref.wholeChapter) {
                    chapterVids.putIfAbsent(
                        video.crn,
                        VerseVideo(video.crn, video.title, video.subtitle, video.thumb, video.firstTime, formatTime(video.firstTime)),
                    )
                } else {
                    val vv = VerseVideo(video.crn, video.title, video.subtitle, video.thumb, item.timeSeconds, item.formattedTime)
                    for (verse in parsed.verses) {
                        if (verse.numbers.any { it in ref.verses }) {
                            val bucket = perVerse.getOrPut(verse.number) { LinkedHashMap() }
                            val existing = bucket[video.crn]
                            if (existing == null || vv.timeSeconds < existing.timeSeconds) bucket[video.crn] = vv
                        }
                    }
                }
            }
        }

        val verses = parsed.verses.map { v ->
            BibleVerse(
                number = v.number,
                numbers = v.numbers,
                text = v.text,
                audioUrl = v.audioUrl,
                heading = v.heading,
                videos = perVerse[v.number]?.values?.sortedBy { it.timeSeconds }.orEmpty(),
            )
        }
        return BibleChapter(bookSlug, bookName, chapter, verses, chapterVids.values.sortedBy { it.timeSeconds })
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
