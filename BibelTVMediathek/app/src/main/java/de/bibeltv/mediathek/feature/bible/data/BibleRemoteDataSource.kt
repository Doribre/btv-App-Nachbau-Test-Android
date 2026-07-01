package de.bibeltv.mediathek.feature.bible.data

import de.bibeltv.mediathek.di.BibleHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lädt die GNB-Seiten live von www.bibeltv.de. Es wird NICHTS lokal gespeichert:
 * der Client hat keinen Disk-Cache und jede Anfrage trägt `Cache-Control: no-store`.
 */
@Singleton
class BibleRemoteDataSource @Inject constructor(
    @BibleHttpClient private val client: OkHttpClient,
) {
    private val base = "https://www.bibeltv.de/bibelthek/GNB"
    private val userAgent =
        "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Mobile Safari/537.36"

    /** Buchkatalog: jede Kapitelseite trägt den vollständigen Buch-Auswähler; obd-1 ist klein. */
    suspend fun fetchCatalogHtml(): String = get("$base/obd-1")

    suspend fun fetchChapterHtml(bookSlug: String, chapter: Int): String = get("$base/$bookSlug-$chapter")

    /** Volltextsuche in der Bibel (GNB) – der Suchbegriff ist ein Pfadsegment. */
    suspend fun fetchSearchHtml(query: String): String {
        val url = base.toHttpUrl().newBuilder().addPathSegment(query).build().toString()
        return get(url)
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("X-Requested-With", "XMLHttpRequest") // sonst nur die leere Hülle
            .header("Cache-Control", "no-store")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string()?.takeIf { it.isNotBlank() } ?: throw IOException("Leere Antwort")
        }
    }
}
