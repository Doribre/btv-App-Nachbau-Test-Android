package de.bibeltv.mediathek.feature.search

import de.bibeltv.mediathek.domain.model.VideoItem
import de.bibeltv.mediathek.feature.bible.data.BibleBook
import de.bibeltv.mediathek.feature.bible.data.BibleReference
import de.bibeltv.mediathek.feature.bible.data.BibleSearchHit
import java.text.Normalizer
import java.util.Locale

/**
 * Ein gemischter Suchtreffer (Bibel ODER Mediathek). [score] dient NUR zum Sortieren
 * der gemeinsamen Liste, nie zum Filtern.
 */
sealed interface SearchResultItem {
    val score: Double

    data class Video(val video: VideoItem, override val score: Double) : SearchResultItem
    data class Bible(val hit: BibleSearchHit, override val score: Double) : SearchResultItem
}

/** Stabiler, kollisionsfreier LazyColumn-Key (nie der Index). */
fun SearchResultItem.key(): String = when (this) {
    is SearchResultItem.Video -> "v:${video.crn}"
    is SearchResultItem.Bible -> "b:${hit.bookSlug}-${hit.chapter}-${hit.verse}"
}

// --- Datei-private Normalisierung (deutsch-aware), von Object UND Feld genutzt ---

private val PUNCT = Regex("[^\\p{L}\\p{Nd}\\s]")
private val WS = Regex("\\s+")

/** NFC -> Kleinschreibung -> ae/oe/ue/ss-Faltung -> Satzzeichen zu Leerraum -> kollabieren. */
private fun normalize(raw: String): String {
    val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
    val lower = nfc.lowercase(Locale.GERMAN)
    val folded = buildString(lower.length) {
        for (c in lower) when (c) {
            'ä' -> append("ae")
            'ö' -> append("oe")
            'ü' -> append("ue")
            'ß' -> append("ss")
            else -> append(c)
        }
    }
    return WS.replace(PUNCT.replace(folded, " "), " ").trim()
}

private fun tokenize(normalized: String): List<String> =
    normalized.split(' ').filter { it.isNotEmpty() }.distinct()

/** Ein gewichtetes Dokumentfeld, EINMAL normalisiert (schützt die Score-Schleife). */
private class ScoredField(text: String, val weight: Double) {
    val norm: String = normalize(text)
    val words: List<String> = norm.split(' ').filter { it.isNotEmpty() }
}

/**
 * Deterministischer, rein funktionaler Relevanz-Ranker für die gemischte Suche.
 * Keine I/O, keine Persistenz, kein State. Gleiche Eingabe -> gleiche Reihenfolge.
 */
object SearchRelevance {

    // --- Tuning-Knöpfe (bewusst benannt, damit sie leicht justierbar sind) ---
    private const val BIBLE_MERGE_CAP = 6       // max. so viele Verse gehen in den Mix (verhindert Bibel-Dominanz)
    private const val BIBLE_BALANCE = 1.0       // Faktor auf Bibel-Scores; höher = Verse steigen früher
    private const val BOOK_ONLY_DEMOTION = 0.5  // Vers, der NUR in Referenz/Buchname matcht (nicht im Verstext), zählt halb

    private const val W_EXACT = 1.0             // ganzes Wort == Token
    private const val W_PREFIX = 0.6            // Wort beginnt mit Token
    private const val W_SUBSTR = 0.3            // Token irgendwo enthalten
    private const val PHRASE_BONUS = 4.0        // ganze Query als zusammenhängender Teilstring

    private fun fieldTokenScore(f: ScoredField, token: String): Double = when {
        f.words.any { it == token } -> W_EXACT
        f.words.any { it.startsWith(token) } -> W_PREFIX
        f.norm.contains(token) -> W_SUBSTR
        else -> 0.0
    }

    private fun scoreDoc(fields: List<ScoredField>, queryTokens: List<String>, queryNorm: String): Double {
        if (queryTokens.isEmpty()) return 0.0
        var weighted = 0.0
        var matched = 0
        // Phrasen-Bonus nur bei Mehrwort-Query (sonst redundant zur Token-Schleife).
        if (queryTokens.size > 1) {
            val best = fields.filter { it.norm.contains(queryNorm) }.maxByOrNull { it.weight }
            if (best != null) weighted += PHRASE_BONUS * best.weight
        }
        for (t in queryTokens) {
            var bestForToken = 0.0
            for (f in fields) bestForToken = maxOf(bestForToken, fieldTokenScore(f, t) * f.weight)
            if (bestForToken > 0.0) matched++
            weighted += bestForToken
        }
        val coverage = matched.toDouble() / queryTokens.size
        val coverageFactor = 0.4 + 0.6 * coverage // Boden 0.4: ein starker Teiltreffer überlebt
        return weighted * coverageFactor
    }

    // Video: title > series > subtitle > genres > description
    private fun videoFields(v: VideoItem) = listOf(
        ScoredField(v.title, 6.0),
        ScoredField(v.seriesTitle ?: "", 4.0),
        ScoredField(v.subtitle ?: "", 3.0),
        ScoredField(v.genres.joinToString(" "), 2.0),
        ScoredField(v.description ?: "", 1.0),
    )

    private fun bibleRefField(h: BibleSearchHit) = ScoredField("${h.reference} ${h.bookName}", 6.0)
    private fun bibleSnippetField(h: BibleSearchHit) = ScoredField(h.snippet, 2.5)

    /**
     * Gemischte, relevanz-sortierte Liste. Filtert NIE (nur Reihenfolge); der Bibel-Input wird auf
     * [BIBLE_MERGE_CAP] gedeckelt, damit hunderte Buch-Verse einen exakten Video-Titel nicht begraben.
     */
    fun merge(videos: List<VideoItem>, bible: List<BibleSearchHit>, rawQuery: String): List<SearchResultItem> {
        val qNorm = normalize(rawQuery)
        val qTokens = tokenize(qNorm)
        if (qTokens.isEmpty()) return emptyList()

        val videoScored = videos.mapIndexed { i, v ->
            Scored(SearchResultItem.Video(v, scoreDoc(videoFields(v), qTokens, qNorm)), source = 0, idx = i)
        }
        val bibleScored = bible.take(BIBLE_MERGE_CAP).mapIndexed { i, h ->
            val refScore = scoreDoc(listOf(bibleRefField(h)), qTokens, qNorm)
            val snippetScore = scoreDoc(listOf(bibleSnippetField(h)), qTokens, qNorm)
            // Buchname-only-Demotion: Query matcht nur in Referenz/Buchname, nicht im Verstext.
            val refFactor = if (snippetScore <= 0.0 && refScore > 0.0) BOOK_ONLY_DEMOTION else 1.0
            val total = (refScore * refFactor + snippetScore) * BIBLE_BALANCE
            Scored(SearchResultItem.Bible(h, total), source = 1, idx = i)
        }

        return (videoScored + bibleScored)
            .sortedWith(
                compareByDescending<Scored> { it.item.score }
                    .thenBy { it.source } // bei Score-Gleichstand: Video (0) vor Bibel (1)
                    .thenBy { it.idx }    // sonst: Original-Reihenfolge je Quelle
                    .thenBy { it.item.key() }, // finaler Total-Order-Garant -> keine Reshuffles
            )
            .map { it.item }
    }

    private class Scored(val item: SearchResultItem, val source: Int, val idx: Int)
}

/** Eine erkannte, eindeutige Kapitel-Referenz -> direkter Absprung in die Bibelthek. */
data class ChapterTarget(val slug: String, val name: String, val chapter: Int)

// "<Buch> <Kapitel>[,/:/. Versteil]" – Buch darf Leerzeichen enthalten ("1 Kor"), Kapitel 1–3-stellig.
private val CHAPTER_REF = Regex("""^(.+?)\s+(\d{1,3})(?:[.,:].*)?$""")

/**
 * Erkennt eine konkrete Kapitel-Eingabe ("mt 8", "Matthäus 8", "1 Kor 3", "joh 3,16") und löst
 * sie über den bereits geladenen Buch-Katalog auf. Gibt null zurück, wenn keine eindeutige
 * Stelle vorliegt (dann normale, gemischte Suche). Verse werden ignoriert – es wird das Kapitel
 * geöffnet. Ungültige Kapitelnummern (> Kapitelanzahl des Buchs) gelten NICHT als Referenz.
 */
fun detectChapterTarget(rawQuery: String, books: List<BibleBook>): ChapterTarget? {
    if (books.isEmpty()) return null
    val m = CHAPTER_REF.find(rawQuery.trim()) ?: return null
    val chapter = m.groupValues[2].toIntOrNull() ?: return null
    if (chapter < 1) return null
    val bookToken = normalize(m.groupValues[1])
    if (bookToken.isEmpty()) return null
    val book = resolveBook(bookToken, books) ?: return null
    if (book.chapters > 0 && chapter > book.chapters) return null
    return ChapterTarget(book.slug, book.name, chapter)
}

/** Vergleicht ein Kandidaten-Feld mit dem (bereits normalisierten) Token, auch leerzeichen-tolerant. */
private fun matchesToken(candidate: String, normToken: String): Boolean {
    val c = normalize(candidate)
    return c == normToken || c.replace(" ", "") == normToken.replace(" ", "")
}

/**
 * Buch-Token (bereits normalisiert) -> Katalogeintrag: über Slug, vollen Namen/Titel, die
 * GNB-Abkürzungen ODER die ökumenischen Loccumer Abkürzungen (Gen, Ex, Koh, Ez, Ijob …).
 */
private fun resolveBook(normToken: String, books: List<BibleBook>): BibleBook? {
    books.firstOrNull {
        matchesToken(it.slug, normToken) || matchesToken(it.name, normToken) || matchesToken(it.title, normToken)
    }?.let { return it }
    val slug = (BibleReference.ABBR_TO_SLUG + BibleReference.LOCCUM_TO_SLUG).entries
        .firstOrNull { matchesToken(it.key, normToken) }?.value
    if (slug != null) return books.firstOrNull { it.slug == slug }
    return null
}
