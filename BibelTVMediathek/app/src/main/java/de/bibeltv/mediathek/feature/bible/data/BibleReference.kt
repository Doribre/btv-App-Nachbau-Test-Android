package de.bibeltv.mediathek.feature.bible.data

/**
 * Parsen der deutschen GNB-Versreferenzen (z. B. "Joh 3,16", "Apg 20,29-31", "Jes 35,4-6.10", "Ps 73")
 * und Abbildung der Buch-Abkürzungen auf die Katalog-Slugs. Tabelle aus der Datenanalyse abgeleitet.
 */
object BibleReference {

    /** Buch-Abkürzung (wie in den Video-Referenzen) → Katalog-Slug. */
    val ABBR_TO_SLUG: Map<String, String> = mapOf(
        "1 Mose" to "1-mose", "2 Mose" to "2-mose", "3 Mose" to "3-mose", "4 Mose" to "4-mose", "5 Mose" to "5-mose",
        "Jos" to "jos", "Ri" to "ri", "Rut" to "rut", "1 Sam" to "1-sam", "2 Sam" to "2-sam",
        "1 Kön" to "1-kon", "2 Kön" to "2-kon", "1 Chr" to "1-chr", "2 Chr" to "2-chr",
        "Esra" to "esra", "Neh" to "neh", "Est" to "est", "Hiob" to "hiob", "Ps" to "ps",
        "Spr" to "spr", "Pred" to "pred", "Hld" to "hld", "Jes" to "jes", "Jer" to "jer",
        "Klgl" to "klgl", "Hes" to "hes", "Dan" to "dan", "Hos" to "hos", "Joel" to "joel",
        "Am" to "am", "Obd" to "obd", "Jona" to "jona", "Mi" to "mi", "Nah" to "nah",
        "Hab" to "hab", "Zef" to "zef", "Hag" to "hag", "Sach" to "sach", "Mal" to "mal",
        "Mt" to "mt", "Mk" to "mk", "Lk" to "lk", "Joh" to "joh", "Apg" to "apg",
        "Röm" to "rom", "1 Kor" to "1-kor", "2 Kor" to "2-kor", "Gal" to "gal", "Eph" to "eph",
        "Phil" to "phil", "Kol" to "kol", "1 Thess" to "1-thess", "2 Thess" to "2-thess",
        "1 Tim" to "1-tim", "2 Tim" to "2-tim", "Tit" to "tit", "Phlm" to "phlm", "Hebr" to "hebr",
        "Jak" to "jak", "1 Petr" to "1-petr", "2 Petr" to "2-petr", "1 Joh" to "1-joh",
        "2 Joh" to "2-joh", "3 Joh" to "3-joh", "Jud" to "jud", "Offb" to "offb",
    )

    /** Slug → Abkürzung (für die VideoHub-VerseItem-Query, z. B. "joh" → "Joh"). */
    val SLUG_TO_ABBR: Map<String, String> = ABBR_TO_SLUG.entries.associate { (abbr, slug) -> slug to abbr }

    data class VerseRef(
        val slug: String,
        val chapter: Int,
        val verses: Set<Int>,    // leer = ganzes Kapitel
        val wholeChapter: Boolean,
    )

    // "<BuchAbk> <Kapitel>[,<Versteil>]" – Abkürzung kann Leerzeichen enthalten ("1 Kor").
    private val REF = Regex("""^(.+?)\s+(\d+)(?:[,:](.+))?$""")

    /** Parst eine Referenz; null, wenn Buch unbekannt oder keine Kapitelangabe (z. B. nur "Lk"). */
    fun parse(raw: String): VerseRef? {
        val s = raw.trim()
        val m = REF.find(s) ?: return null
        val abbr = m.groupValues[1].trim().replace(Regex("\\s+"), " ")
        val slug = ABBR_TO_SLUG[abbr] ?: ABBR_TO_SLUG[abbr.replace(".", "").trim()] ?: return null
        val chapter = m.groupValues[2].toIntOrNull() ?: return null
        val versePart = m.groupValues[3].takeIf { it.isNotBlank() }
            ?: return VerseRef(slug, chapter, emptySet(), wholeChapter = true)
        val verses = expandVerses(versePart)
        return VerseRef(slug, chapter, verses, wholeChapter = verses.isEmpty())
    }

    /**
     * Expandiert einen Versteil ("4-6.10", "16.17", "2b", "5f") zu konkreten Versnummern.
     * Halbvers-Buchstaben (a/b/c) und f/ff werden auf den Basisvers reduziert.
     */
    fun expandVerses(part: String): Set<Int> {
        val out = sortedSetOf<Int>()
        for (rawSeg in part.split(".")) {
            val seg = rawSeg.trim().replace(Regex("[a-zA-Z]+$"), "")
            if (seg.isBlank()) continue
            val range = seg.split("-")
            if (range.size == 2) {
                val a = range[0].toIntOrNull()
                val b = range[1].toIntOrNull()
                if (a != null && b != null && b >= a) {
                    for (v in a..b) out.add(v)
                } else {
                    range[0].toIntOrNull()?.let(out::add)
                }
            } else {
                seg.toIntOrNull()?.let(out::add)
            }
        }
        return out
    }
}
