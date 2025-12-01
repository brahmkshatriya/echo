package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.settings.Settings

class MockedSettings : Settings {
    override fun getBoolean(key: String): Boolean? = null

    override fun getInt(key: String): Int? = null

    override fun getString(key: String): String? = null

    override fun getStringSet(key: String): Set<String>? = null

    override fun putBoolean(key: String, value: Boolean?) {}

    override fun putInt(key: String, value: Int?) {}

    override fun putString(key: String, value: String?) {}

    override fun putStringSet(key: String, value: Set<String>?) {}

}