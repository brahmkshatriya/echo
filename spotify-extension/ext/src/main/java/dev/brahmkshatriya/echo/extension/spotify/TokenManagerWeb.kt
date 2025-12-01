package dev.brahmkshatriya.echo.extension.spotify

import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.extension.spotify.SpotifyApi.Companion.userAgent
import dev.brahmkshatriya.echo.extension.spotify.TOTP.convertToHex
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.io.encoding.ExperimentalEncodingApi

class TokenManagerWeb(
    private val api: SpotifyApi,
) {
    private val json = api.json
    val httpClient = OkHttpClient.Builder().addInterceptor {
        val req = it.request().newBuilder()
        val cookie = api.cookie
        if (cookie != null) req.addHeader("Cookie", cookie)
        req.addHeader(userAgent.first, userAgent.second)
        req.addHeader("Referer", "https://open.spotify.com/")
        it.proceed(req.build())
    }.build()

    var accessToken: String? = null
    private var tokenExpiration: Long = 0

    private suspend fun createAccessToken(): String {
        val req = Request.Builder().url(getUrl())
        val res = httpClient.newCall(req.build()).await()
        val body = res.body.string()
        val token = runCatching { json.decode<TokenResponse>(body) }.getOrElse {
            throw runCatching { json.decode<ErrorMessage>(body).error }.getOrElse {
                Exception(body.ifEmpty { "Token Code ${res.code}" })
            }
        }

        accessToken = token.accessToken
        tokenExpiration = token.accessTokenExpirationTimestampMs - 5 * 60 * 1000
        return accessToken!!
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun getUrl(): String {
        val (secret, version) = getDataFromSite()
        val time = System.currentTimeMillis()
        val totp = TOTP.generateTOTP(secret, (time / 30000).toHexString().uppercase())
        val url =
            "https://open.spotify.com/api/token?reason=init&productType=web-player&totp=${totp}&totpServer=${totp}&totpVer=$version"
        return url
    }

    private val secretsUrl =
        "https://raw.githubusercontent.com/itsmechinmoy/echo-extensions/refs/heads/main/noidea.txt"

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getDataFromSite(): Secret {
        val string =
            httpClient.newCall(Request.Builder().url(secretsUrl).build()).await().body.string()
        val (secret, version) = json.decode<Secret>(string)
        val hex = convertToHex(secret)
        return Secret(hex, version)
    }

    suspend fun getToken() =
        if (accessToken == null || !isTokenWorking(tokenExpiration)) createAccessToken()
        else accessToken!!

    fun clear() {
        accessToken = null
        tokenExpiration = 0
    }

    private fun isTokenWorking(expiry: Long): Boolean {
        return (System.currentTimeMillis() < expiry)
    }


    @Serializable
    data class Secret(
        val secret: String,
        val version: Int,
    )

    @Serializable
    data class TokenResponse(
        val isAnonymous: Boolean,
        val accessTokenExpirationTimestampMs: Long,
        val clientId: String,
        val accessToken: String,
    )

    @Serializable
    data class ErrorMessage(
        val error: Error,
    )

    @Serializable
    data class Error(
        val code: Int,
        override val message: String,
    ) : Exception(message)
}