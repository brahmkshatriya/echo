package dev.brahmkshatriya.echo.extension.spotify

import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.extension.spotify.mercury.MercuryConnection
import dev.brahmkshatriya.echo.extension.spotify.mercury.StoredToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

class TokenManagerApp(
    private val api: SpotifyApi,
) {
    val client = api.web.httpClient
    private suspend fun createRefreshToken(): String {
        val codeVerifier = generateCode()
        val challenge = generateCodeChallenge(codeVerifier)
        val authRequest = Request.Builder().url(getAuthUrl(challenge)).head().build()
        val cookies = client.newCall(authRequest).await().headers("set-cookie")
        val crsfToken = cookies.firstOrNull { it.startsWith("csrf_token=") }
            ?.substringAfter("csrf_token=")?.substringBefore(";")
            ?: throw Exception("CRSF Token not found")

        val codeReq = Request.Builder()
            .url("https://accounts.spotify.com/en/authorize/accept?ajax_redirect=1")
            .addHeader("Cookie", "csrf_token=$crsfToken")
            .post(
                FormBody.Builder()
                    .add("request", "undefined")
                    .add("response_type", "code")
                    .add("client_id", CLIENT_ID)
                    .add("redirect_uri", REDIRECT_URI)
                    .add("code_challenge", challenge)
                    .add("code_challenge_method", "S256")
                    .add("scope", scopes)
                    .add("csrf_token", crsfToken)
                    .build()
            )
            .build()
        val code = client.newCall(codeReq).await()
            .header("Location")?.substringAfter("code=")
            ?: throw Exception("Location header not found")

        val accessTokenJson = client.newCall(
            Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(
                    FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("client_id", CLIENT_ID)
                        .add("redirect_uri", REDIRECT_URI)
                        .add("code", code)
                        .add("code_verifier", codeVerifier)
                        .build()
                )
                .build()
        ).await().body.string()
        return api.json.decode<Response>(accessTokenJson).refreshToken
            ?: throw Exception("Refresh Token not found")
    }

    var accessToken: String? = null
    var tokenExpiration: Long = 0
    var mercuryToken: StoredToken? = null
    fun clear() {
        accessToken = null
        tokenExpiration = 0
        mercuryToken = null
    }

    suspend fun getToken(): String {
        if (accessToken == null || !isTokenWorking(tokenExpiration)) runCatching {
            val refreshToken = api.filesDir.resolve("refresh.txt").readText()
            createAccessToken(refreshToken)
        }.getOrElse {
            val refreshToken = createRefreshToken()
            val file = api.filesDir.resolve("refresh.txt")
            file.parentFile?.mkdirs()
            file.writeText(refreshToken)
            createAccessToken(refreshToken)
        }
        return accessToken!!
    }

    suspend fun getMercuryToken(): StoredToken {
        if (mercuryToken == null) {
            val file = api.filesDir.resolve("mercury.txt")
            mercuryToken = if (file.exists()) {
                val token = api.json.decode<StoredToken>(file.readText())
                token
            } else {
                val accessToken = this.getToken()
                val token = MercuryConnection.getStoredToken(accessToken)
                file.parentFile?.mkdirs()
                file.writeText(api.json.encode<StoredToken>(token))
                token
            }
        }
        return mercuryToken!!
    }

    private suspend fun createAccessToken(refreshToken: String): String {
        val accessTokenJson = client.newCall(
            Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(
                    FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("client_id", CLIENT_ID)
                        .add("refresh_token", refreshToken)
                        .build()
                )
                .build()
        ).await().body.string()
        val response = api.json.decode<Response>(accessTokenJson)
        val token = response.accessToken
            ?: throw Exception("Access Token not found")
        accessToken = token
        tokenExpiration = System.currentTimeMillis() + ((response.expiresIn ?: 3600) * 1000)
        return token
    }


    private fun isTokenWorking(expiry: Long): Boolean {
        return (System.currentTimeMillis() < expiry)
    }

    private val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private fun generateCode() = buildString {
        repeat(128) { append(possible[Random.nextInt(possible.length)]) }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateCodeChallenge(code: String): String {

        val digest = MessageDigest.getInstance("SHA-256")!!
        val hashed = digest.digest(code.toByteArray(StandardCharsets.UTF_8))
        return Base64.encode(hashed).replace("=", "")
            .replace("+", "-")
            .replace("/", "_")
    }

    @Serializable
    data class Response(
        @SerialName("access_token")
        val accessToken: String? = null,
        @SerialName("token_type")
        val tokenType: String? = null,
        @SerialName("expires_in")
        val expiresIn: Long? = null,
        @SerialName("refresh_token")
        val refreshToken: String? = null,
        val scope: String? = null,
        val username: String? = null,
    )

    private fun getAuthUrl(challenge: String): String {
        val params = listOf(
            "client_id" to CLIENT_ID,
            "response_type" to "code",
            "redirect_uri" to REDIRECT_URI,
            "scope" to scopes,
            "code_challenge_method" to "S256",
            "code_challenge" to challenge
        ).joinToString("&") {
            "${it.first}=${URLEncoder.encode(it.second, "utf-8")}"
        }
        return "https://accounts.spotify.com/en/oauth2/v2/auth?$params"
    }

    companion object {
        const val REDIRECT_URI = "http://127.0.0.1:4381/login"
        const val CLIENT_ID = "65b708073fc0480ea92a077233ca87bd"
        val scopes = listOf(
            "app-remote-control",
            "playlist-modify",
            "playlist-modify-private",
            "playlist-modify-public",
            "playlist-read",
            "playlist-read-collaborative",
            "playlist-read-private",
            "streaming",
            "transfer-auth-session",
            "ugc-image-upload",
            "user-follow-modify",
            "user-follow-read",
            "user-library-modify",
            "user-library-read",
            "user-modify",
            "user-modify-playback-state",
            "user-modify-private",
            "user-personalized",
            "user-read-birthdate",
            "user-read-currently-playing",
            "user-read-email",
            "user-read-play-history",
            "user-read-playback-position",
            "user-read-playback-state",
            "user-read-private",
            "user-read-recently-played",
            "user-top-read"
        ).joinToString(",")
    }
}