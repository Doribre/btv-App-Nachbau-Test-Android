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

/** Deutsche Funktionswörter (normalisiert). Zählen NICHT in die Query-Abdeckung. */
private val STOPWORDS = setOf(
    "der", "die", "das", "und", "in", "im", "zu", "zum", "zur", "von", "vom", "mit", "ist", "sind",
    "am", "an", "auf", "fuer", "den", "dem", "des", "ein", "eine", "einen", "als", "auch", "oder",
    "was", "wie", "wer", "dass", "nicht", "es", "er", "sie",
)

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
    private const val MIN_SUBSTR_LEN = 4        // Teilstring-Treffer erst ab dieser Tokenlänge (killt "am" in "Amsterdam")

    // Cross-Source-Fusion: die beiden Backends liefern KEINE vergleichbaren Scores -> rang-basiert mischen.
    private const val RRF_K = 60.0              // Reciprocal-Rank-Fusion-Dämpfung
    private const val W_RRF = 0.6               // Gewicht der Rang-Fusion
    private const val W_LEX = 0.4               // Gewicht des quellenweise normalisierten Lexik-Scores
    private const val BIBLE_BOOK_CAP = 2        // max. so viele Verse je Buch im Mix (Vielfalt)

    private fun fieldTokenScore(f: ScoredField, token: String): Double = when {
        f.words.any { it == token } -> W_EXACT
        f.words.any { it.startsWith(token) } -> W_PREFIX
        token.length >= MIN_SUBSTR_LEN && f.norm.contains(token) -> W_SUBSTR
        else -> 0.0
    }

    private fun scoreDoc(fields: List<ScoredField>, queryTokens: List<String>, queryNorm: String): Double {
        if (queryTokens.isEmpty()) return 0.0
        var weighted = 0.0
        var matched = 0
        var denom = 0
        // Phrasen-Bonus nur bei Mehrwort-Query (sonst redundant zur Token-Schleife).
        if (queryTokens.size > 1) {
            val best = fields.filter { it.norm.contains(queryNorm) }.maxByOrNull { it.weight }
            if (best != null) weighted += PHRASE_BONUS * best.weight
        }
        for (t in queryTokens) {
            val isStop = t in STOPWORDS
            if (!isStop) denom++
            var bestTier = 0.0        // ungewichtete Trefferstufe (für Abdeckung)
            var bestWeighted = 0.0    // gewichteter Beitrag (für den Score)
            for (f in fields) {
                val tier = fieldTokenScore(f, t)
                if (tier > bestTier) bestTier = tier
                bestWeighted = maxOf(bestWeighted, tier * f.weight)
            }
            weighted += bestWeighted
            // Nur echte Wort-/Präfix-Treffer (kein reiner Teilstring) zählen zur Abdeckung.
            if (!isStop && bestTier >= W_PREFIX) matched++
        }
        val coverage = if (denom == 0) 1.0 else matched.toDouble() / denom
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
     * Gemischte, relevanz-sortierte Liste. Filtert NIE (nur Reihenfolge).
     *
     * Ranking: Videos und Bibel liefern keine vergleichbaren Roh-Scores, daher wird je Quelle
     * (a) der Lexik-Score auf [0,1] normalisiert und (b) eine Reciprocal-Rank-Fusion über den
     * quelleninternen Rang gebildet; beides wird gemischt. Verse werden zuerst komplett bewertet
     * und dann als BESTE [BIBLE_MERGE_CAP] übernommen (max. [BIBLE_BOOK_CAP] je Buch) – nicht mehr
     * die zufälligen ersten der HTML-Reihenfolge.
     */
    fun merge(videos: List<VideoItem>, bible: List<BibleSearchHit>, rawQuery: String): List<SearchResultItem> {
        val qNorm = normalize(rawQuery)
        val qTokens = tokenize(qNorm)
        if (qTokens.isEmpty()) return emptyList()

        // Videos: alle Kandidaten bewerten.
        val videoRaw = videos.mapIndexed { i, v ->
            val s = scoreDoc(videoFields(v), qTokens, qNorm)
            RawScored(SearchResultItem.Video(v, s), s, i)
        }

        // Bibel: ALLE bewerten -> nach Score sortieren -> je Buch deckeln -> die besten übernehmen.
        val bibleRankedAll = bible.mapIndexed { i, h ->
            val refScore = scoreDoc(listOf(bibleRefField(h)), qTokens, qNorm)
            val snippetScore = scoreDoc(listOf(bibleSnippetField(h)), qTokens, qNorm)
            // Buchname-only-Demotion: Query matcht nur in Referenz/Buchname, nicht im Verstext.
            val refFactor = if (snippetScore <= 0.0 && refScore > 0.0) BOOK_ONLY_DEMOTION else 1.0
            val total = (refScore * refFactor + snippetScore) * BIBLE_BALANCE
            RawScored(SearchResultItem.Bible(h, total), total, i)
        }.sortedWith(compareByDescending<RawScored> { it.raw }.thenBy { it.idx })

        val perBook = HashMap<String, Int>()
        val bibleRaw = ArrayList<RawScored>()
        for (rs in bibleRankedAll) {
            val slug = (rs.item as SearchResultItem.Bible).hit.bookSlug
            val used = perBook[slug] ?: 0
            if (used >= BIBLE_BOOK_CAP) continue
            perBook[slug] = used + 1
            bibleRaw.add(rs)
            if (bibleRaw.size >= BIBLE_MERGE_CAP) break
        }

        return (fuse(videoRaw, source = 0) + fuse(bibleRaw, source = 1))
            .sortedWith(
                compareByDescending<Scored> { it.fused }
                    .thenBy { it.source } // bei Gleichstand: Video (0) vor Bibel (1)
                    .thenBy { it.idx }    // sonst: Original-Reihenfolge je Quelle
                    .thenBy { it.item.key() }, // finaler Total-Order-Garant -> keine Reshuffles
            )
            .map { it.item }
    }

    /** Normalisiert die Lexik-Scores einer Quelle auf [0,1] und mischt sie mit der Reciprocal-Rank-Fusion. */
    private fun fuse(items: List<RawScored>, source: Int): List<Scored> {
        if (items.isEmpty()) return emptyList()
        val maxS = items.maxOf { it.raw }
        val minS = items.minOf { it.raw }
        val out = ArrayList<Scored>(items.size)
        items.sortedByDescending { it.raw }.forEachIndexed { rank, rs ->
            val norm = if (maxS <= minS) { if (rs.raw > 0.0) 1.0 else 0.0 } else (rs.raw - minS) / (maxS - minS)
            val rrf01 = RRF_K / (RRF_K + rank)
            val fused = W_RRF * rrf01 + W_LEX * norm
            out.add(Scored(rs.item, source, rs.idx, fused))
        }
        return out
    }

    private class RawScored(val item: SearchResultItem, val raw: Double, val idx: Int)
    private class Scored(val item: SearchResultItem, val source: Int, val idx: Int, val fused: Double)
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
    val m = CHAPTER_REF.find(rawQuery.trim()) ?: return null
    val chapter = m.groupValues[2].toIntOrNull() ?: return null
    if (chapter < 1) return null
    val bookToken = normalize(m.groupValues[1])
    if (bookToken.isEmpty()) return null
    val slug = resolveSlug(bookToken, books) ?: return null
    val book = books.firstOrNull { it.slug == slug }
    // Kapitel-Obergrenze nur prüfen, wenn der Katalog schon geladen ist (Kaltstart-tolerant).
    if (book != null && book.chapters > 0 && chapter > book.chapters) return null
    val name = book?.name ?: BibleReference.SLUG_TO_ABBR[slug] ?: slug
    return ChapterTarget(slug, name, chapter)
}

/** Vergleicht ein Kandidaten-Feld mit dem (bereits normalisierten) Token, auch leerzeichen-tolerant. */
private fun matchesToken(candidate: String, normToken: String): Boolean {
    val c = normalize(candidate)
    return c == normToken || c.replace(" ", "") == normToken.replace(" ", "")
}

/**
 * Buch-Token (bereits normalisiert) -> Katalog-Slug: über Slug/Namen/Titel des Katalogs ODER die
 * GNB-/Loccumer-Abkürzungstabellen. Da die Tabellen Compile-Zeit-Konstanten sind, funktioniert die
 * Auflösung auch, wenn der Katalog noch nicht geladen ist (Kaltstart, z. B. "mt 8" gleich beim Öffnen).
 */
private fun resolveSlug(normToken: String, books: List<BibleBook>): String? {
    books.firstOrNull {
        matchesToken(it.slug, normToken) || matchesToken(it.name, normToken) || matchesToken(it.title, normToken)
    }?.let { return it.slug }
    return (BibleReference.ABBR_TO_SLUG + BibleReference.LOCCUM_TO_SLUG).entries
        .firstOrNull { matchesToken(it.key, normToken) }?.value
}
