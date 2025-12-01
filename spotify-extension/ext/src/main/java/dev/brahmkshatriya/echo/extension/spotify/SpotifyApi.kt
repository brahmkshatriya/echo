package dev.brahmkshatriya.echo.extension.spotify

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.extension.spotify.mercury.MercuryConnection
import dev.brahmkshatriya.echo.extension.spotify.mercury.StoredToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.closeQuietly
import java.io.File
import java.net.URLEncoder

class SpotifyApi(val filesDir: File) {
    val json = Json()

    private val webMutex = Mutex()
    val web = TokenManagerWeb(this)
    private val appMutex = Mutex()
    val app = TokenManagerApp(this)

    val cookie get() = _cookie
    private var _cookie: String? = null
    fun setCookie(cookie: String?) {
        _cookie = cookie
        synchronized(web) { web.clear() }
        synchronized(app) { app.clear() }
    }


    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val builder = chain.request().newBuilder()
            builder.addHeader(userAgent.first, userAgent.second)
            builder.addHeader("Accept", "application/json")
            builder.addHeader("App-Platform", "WebPlayer")
            web.accessToken?.let {
                if (request.headers["Authorization"] == null)
                    builder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(builder.build())
        }
        .build()

    data class Response<T>(
        val json: T,
        val raw: String,
    )

    suspend fun graphCall(
        operationName: String,
        persistedQuery: String,
        variables: JsonObject = buildJsonObject { },
    ): String {
        val req = Request.Builder()
            .url("https://api-partner.spotify.com/pathfinder/v2/query")
            .post(
                buildJsonObject {
                    put("operationName", operationName)
                    put("variables", variables)
                    put("extensions", extensions(persistedQuery))
                }.toString().toRequestBody("application/json".toMediaType())
            )
        return callGetBody(req.build())
    }

    suspend inline fun <reified T> graphQuery(
        operationName: String,
        persistedQuery: String,
        variables: JsonObject = buildJsonObject { },
        print: Boolean = false,
    ): Response<T> {
        val raw = graphCall(operationName, persistedQuery, variables)
        if (print) println(raw)
        return Response(json.decode<T>(raw), raw)
    }

    suspend inline fun <reified T> clientQuery(path: String): Response<T> {
        val raw = callGetBody(
            Request.Builder()
                .url("https://spclient.wg.spotify.com/$path")
                .build()
        )
        return Response(json.decode<T>(raw), raw)
    }

    suspend inline fun <reified T> clientMutate(path: String, data: JsonObject): Response<T> {
        val raw = callGetBody(
            Request.Builder()
                .url("https://spclient.wg.spotify.com/$path")
                .post(data.toString().toRequestBody("application/json".toMediaType()))
                .build()
        )
        return Response(json.decode<T>(raw), raw)
    }

    suspend fun callGetBody(request: Request): String {
        runCatching { webMutex.withLock { web.getToken() } }.getOrElse {
            val id = userId
            if (id != null && it is TokenManagerWeb.Error) throw ClientException.Unauthorized(id)
            throw it
        }
        val response = call(request).body.string()
        return if (response.startsWith('{')) response else {
            throw Exception("Invalid response: $response")
        }
    }


    private fun extensions(persistedQuery: String): JsonObject {
        return buildJsonObject {
            putJsonObject("persistedQuery") {
                put("version", 1)
                put("sha256Hash", persistedQuery)
            }
        }
    }

    private suspend fun call(
        request: Request, ignore: Boolean = false, auth: String? = null,
    ) = run {
        val req = if (auth == null) request
        else request.newBuilder().addHeader("Authorization", "Bearer $auth").build()
        val res = client.newCall(req).await()
        if (ignore || res.isSuccessful) res
        else {
            res.closeQuietly()
            throw Exception("${res.code}: Failed to call - ${req.url}")
        }
    }

    suspend fun getWebAccessToken(): String {
        return webMutex.withLock { web.getToken() }
    }

    var userId: String? = null
    fun setUser(id: String?) {
        userId = id
    }

    suspend fun getAppAccessToken(): String {
        return appMutex.withLock { app.getToken() }
    }

    suspend fun getMercuryToken(): StoredToken {
        return appMutex.withLock {
            app.getMercuryToken()
        }
    }

    companion object {
        fun urlEncode(data: String): String = URLEncoder.encode(data, "UTF-8")
        val userAgent =
            "user-agent" to "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36"
    }
}