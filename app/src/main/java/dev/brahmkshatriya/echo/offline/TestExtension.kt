package dev.brahmkshatriya.echo.offline

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.ImportType

class TestExtension : ExtensionClient, LoginClient.UsernamePassword {

    val metadata = ExtensionMetadata(
        "TestExtension",
        "",
        ImportType.Inbuilt,
        "Test extension for offline testing",
        "Test Extension",
        "1.0.0",
        "Test",
        "Test",
        null,
    )

    override suspend fun onExtensionSelected() {}
    override val settingItems: List<Setting> = emptyList()
    override fun setSettings(settings: Settings) {}
    override suspend fun onLogin(username: String, password: String): List<User> {
        return listOf(User(username, username, null))
    }

    override suspend fun onSetLoginUser(user: User?) {
        println("onSetLoginUser: $user")
    }

    override suspend fun getCurrentUser(): User? = null
}