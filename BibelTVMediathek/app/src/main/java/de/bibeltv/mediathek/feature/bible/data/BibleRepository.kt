package de.bibeltv.mediathek.feature.bible.data

import de.bibeltv.mediathek.data.repository.VideoHubRepository
import de.bibeltv.mediathek.domain.model.ChapterVideoLink
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bibelthek-Repository. Lädt den Kapiteltext LIVE (www.bibeltv.de, GNB) und verknüpft die Videos
 * über das offizielle VideoHub-GraphQL (VerseItem) — kein Scraping für die Video-Brücke.
 * Bibeltext wird NIE persistiert; nur der Buchkatalog (Metadaten) lebt für die Sitzung im Speicher.
 */
@Singleton
class BibleRepository @Inject constructor(
    private val remote: BibleRemoteDataSource,
    private val videoHub: VideoHubRepository,
) {
    @Volatile
    private var cachedBooks: List<BibleBook>? = null

    suspend fun books(): List<BibleBook> {
        cachedBooks?.let { return it }
        val books = BibleHtmlParser.parseCatalog(remote.fetchCatalogHtml())
        if (books.isNotEmpty()) cachedBooks = books
        return books
    }

    /** Lädt ein Kapitel frisch: Text live aus dem Netz, Video-Verknüpfungen aus dem VideoHub. */
    suspend fun chapter(bookSlug: String, bookName: String, chapter: Int): BibleChapter {
        val parsed = BibleHtmlParser.parseChapter(remote.fetchChapterHtml(bookSlug, chapter))
        val abbr = BibleReference.SLUG_TO_ABBR[bookSlug]
        val links = if (abbr != null) {
            runCatching { videoHub.verseVideos(abbr, chapter) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        return resolve(bookSlug, bookName, chapter, parsed, links)
    }

    private fun resolve(
        bookSlug: String,
        bookName: String,
        chapter: Int,
        parsed: BibleHtmlParser.ParsedChapter,
        links: List<ChapterVideoLink>,
    ): BibleChapter {
        val perVerse = HashMap<String, LinkedHashMap<String, VerseVideo>>()
        val chapterVids = LinkedHashMap<String, VerseVideo>()

        for (link in links) {
            for (hit in link.refs) {
                val ref = BibleReference.parse(hit.verse) ?: continue
                if (ref.slug != bookSlug || ref.chapter != chapter) continue
                val vv = VerseVideo(link.crn, link.title, link.subtitle, link.thumbnailUrl, hit.timeSeconds, formatTime(hit.timeSeconds))
                if (ref.wholeChapter) {
                    chapterVids.putIfAbsent(link.crn, vv)
                } else {
                    for (verse in parsed.verses) {
                        if (verse.numbers.any { it in ref.verses }) {
                            val bucket = perVerse.getOrPut(verse.number) { LinkedHashMap() }
                            val existing = bucket[link.crn]
                            if (existing == null || vv.timeSeconds < existing.timeSeconds) bucket[link.crn] = vv
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
