package dev.brahmkshatriya.echo.data

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.UrlHolder.Companion.toUrlHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting

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
            .toUrlHolder()

    override val loginWebViewStopUrlRegex = "https://music\\.youtube\\.com/.*".toRegex()
    override suspend fun onLoginWebviewStop(cookies: Map<String, String>): List<User> {
        println("Cookies")
        cookies.forEach { (key, value) ->
            println("$key: $value")
        }
        return emptyList()
    }

    override suspend fun onLoginUserSelected(user: User) {
        TODO("Not yet implemented")
    }


}