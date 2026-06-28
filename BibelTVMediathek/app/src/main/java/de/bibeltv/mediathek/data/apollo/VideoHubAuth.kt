package de.bibeltv.mediathek.data.apollo

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import de.bibeltv.mediathek.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holt und cached ein M2M-Zugriffstoken (client_credentials) von Keycloak.
 * KEIN Nutzer-Login – nur Maschinen-Authentifizierung für den VideoHub.
 */
@Singleton
class VideoHubTokenProvider @Inject constructor() {
    private val http = OkHttpClient()
    private val mutex = Mutex()

    @Volatile private var token: String? = null
    @Volatile private var expiresAtMs: Long = 0L

    private fun valid(): String? =
        token?.takeIf { System.currentTimeMillis() < expiresAtMs - 30_000 }

    suspend fun bearer(): String {
        valid()?.let { return it }
        return mutex.withLock {
            valid() ?: fetchToken()
        }
    }

    private suspend fun fetchToken(): String = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", BuildConfig.VIDEOHUB_CLIENT_ID)
            .add("client_secret", BuildConfig.VIDEOHUB_CLIENT_SECRET)
            .build()
        val request = Request.Builder()
            .url(BuildConfig.KEYCLOAK_TOKEN_URL)
            .post(body)
            .build()
        // Bis zu 3 Versuche mit Backoff – fängt transiente Netzwerk-/5xx-Fehler ab.
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                http.newCall(request).execute().use { resp ->
                    val json = resp.body?.string().orEmpty()
                    check(resp.isSuccessful) { "Token-Anfrage fehlgeschlagen: HTTP ${resp.code}" }
                    val obj = JSONObject(json)
                    val access = obj.getString("access_token")
                    val expiresIn = obj.optLong("expires_in", 300L)
                    token = access
                    expiresAtMs = System.currentTimeMillis() + expiresIn * 1000L
                    return@withContext access
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) delay(400L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Token konnte nicht geladen werden.")
    }
}

/** Hängt an jede GraphQL-Anfrage einen frischen Bearer-Token. */
class AuthHttpInterceptor(
    private val tokenProvider: VideoHubTokenProvider,
) : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
        val bearer = tokenProvider.bearer()
        val authed = request.newBuilder().addHeader("Authorization", "Bearer $bearer").build()
        return chain.proceed(authed)
    }
}
