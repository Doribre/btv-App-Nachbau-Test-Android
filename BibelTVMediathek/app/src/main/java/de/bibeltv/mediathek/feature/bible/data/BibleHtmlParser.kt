package de.bibeltv.mediathek.feature.bible.data

import org.json.JSONArray
import org.jsoup.Jsoup

/**
 * Parst die live geladenen GNB-Seiten (www.bibeltv.de/bibelthek). Rein funktional, kein Zustand,
 * speichert nichts. Robust gegen die in der Analyse gefundenen Eigenheiten (Versnummern als
 * Strings inkl. Bereiche/Suffixe, Poesie-Zeilen, h4-Überschriften, leere Videolisten).
 */
object BibleHtmlParser {

    data class RawVerseItem(val verse: String, val timeSeconds: Int, val formattedTime: String)
    data class RawVideo(
        val crn: String,
        val title: String,
        val subtitle: String?,
        val thumb: String?,
        val firstTime: Int,
        val versesItems: List<RawVerseItem>,
    )
    data class ParsedVerse(
        val number: String,
        val numbers: Set<Int>,
        val text: String,
        val audioUrl: String?,
        val heading: String?,
    )
    data class ParsedChapter(val verses: List<ParsedVerse>, val videos: List<RawVideo>)

    data class RawSearchHit(val slug: String, val reference: String, val snippet: String)

    fun parseSearch(html: String): List<RawSearchHit> {
        val doc = Jsoup.parse(html)
        return doc.select(".fulltext-search-result a[data-slug]").mapNotNull { a ->
            val slug = a.attr("data-slug").ifBlank { return@mapNotNull null }
            val reference = a.selectFirst(".ref")?.text()?.trim().orEmpty()
            val snippet = a.selectFirst(".verse")?.text()?.trim().orEmpty()
            RawSearchHit(slug, reference, snippet)
        }
    }

    fun parseCatalog(html: String): List<BibleBook> {
        val doc = Jsoup.parse(html)
        val selector = doc.selectFirst("[data-book-selector]")?.attr("data-book-selector") ?: return emptyList()
        val inner = Jsoup.parse(selector)
        return inner.select("li[data-slug]").mapNotNull { li ->
            val slug = li.attr("data-slug").ifBlank { return@mapNotNull null }
            val name = li.text().replace(' ', ' ').replace(Regex("\\s+"), " ").trim()
            val title = li.attr("title").ifBlank { name }
            val chapters = li.attr("data-max-chapter").toIntOrNull() ?: 1
            val isNt = li.parents().any { p -> p.classNames().any { it == "nt" || it.startsWith("nt-") } }
            BibleBook(slug, name, title, chapters, if (isNt) "NT" else "AT")
        }
    }

    fun parseChapter(html: String): ParsedChapter {
        val doc = Jsoup.parse(html)
        val verses = doc.select("div[data-verse-number]").map { el ->
            val number = el.attr("data-verse-number")
            val text = el.selectFirst(".verse-content")?.wholeText()?.let(::cleanText).orEmpty()
            val audio = parseAudioUrl(el.attr("data-audio"))
            val prev = el.previousElementSibling()
            val heading = if (prev != null && prev.tagName() in setOf("h2", "h3", "h4")) {
                prev.text().trim().ifBlank { null }
            } else {
                null
            }
            ParsedVerse(number, BibleReference.expandVerses(number), text, audio, heading)
        }
        val videosJson = doc.selectFirst("#video-list")?.attr("data-videos")
            ?: doc.selectFirst("[data-videos]")?.attr("data-videos")
        return ParsedChapter(verses, parseRawVideos(videosJson))
    }

    /** Intra-Zeilen-Whitespace kollabieren, aber Poesie-Zeilenumbrüche (\n) erhalten. */
    private fun cleanText(raw: String): String =
        raw.replace("\r", "")
            .split("\n")
            .joinToString("\n") { it.replace(Regex("[ \t]+"), " ").trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    private fun parseAudioUrl(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val arr = JSONArray(json)
            if (arr.length() == 0) null else arr.getJSONObject(0).optString("audio_url").ifBlank { null }
        }.getOrNull()
    }

    private fun parseRawVideos(json: String?): List<RawVideo> {
        if (json.isNullOrBlank() || json.trim() == "[]") return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val itemsArr = o.optJSONArray("versesItems")
                val items = if (itemsArr == null) emptyList() else (0 until itemsArr.length()).mapNotNull { j ->
                    val vi = itemsArr.optJSONObject(j) ?: return@mapNotNull null
                    RawVerseItem(
                        verse = vi.optString("###VERSE###"),
                        timeSeconds = vi.optInt("###TIME###"),
                        formattedTime = vi.optString("###FORMATTEDTIME###"),
                    )
                }
                RawVideo(
                    crn = o.optString("###CRN###"),
                    title = o.optString("###TITLE###"),
                    subtitle = o.optString("###SUBTITLE###").ifBlank { null },
                    thumb = o.optString("###THUMB###").ifBlank { null },
                    firstTime = o.optInt("###FIRSTTIME###"),
                    versesItems = items,
                )
            }.filter { it.crn.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
