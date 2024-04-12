package dev.brahmkshatriya.echo.data

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import java.security.MessageDigest

class TestExtension : ExtensionClient(), LoginClient.WebView {
    override val metadata = ExtensionMetadata(
        "test",
        "Test",
        "1.0",
        "Test extension",
        "Brahmkshatriya",
        null
    )

    override val settings = listOf<Setting>()
    override val loginWebViewInitialUrl =
        "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"
            .toRequest()

    override val loginWebViewStopUrlRegex = "https://music\\.youtube\\.com/.*".toRegex()

    override suspend fun onLoginWebviewStop(request: Request, cookie: String): List<User> {
        println("request: $request")
        println("cookie: $cookie")
        if (!cookie.contains("SAPISID"))
            throw Exception("Login Failed, could not load SAPISID")

        val headers = mutableMapOf("cookie" to cookie) .apply {
            val currentTime = System.currentTimeMillis() / 1000
            val id = cookie.split("SAPISID=")[1].split(";")[0]
            val idHash =
                sha1("$currentTime $id https://music.youtube.com")
            set("authorization", "SAPISIDHASH ${currentTime}_${idHash}")
        }
        println(headers)
        return listOf(User("Test", "Test", "https://picsum.photos/200".toImageHolder()))
    }

    private fun sha1(str: String) =
        MessageDigest.getInstance("SHA-1").digest(str.toByteArray())
            .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }


    override suspend fun onSetLoginUser(user: User) {
        TODO("Not yet implemented")
    }


}