package dev.brahmkshatriya.echo.common.settings

interface Settings {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getStringSet(key: String): Set<String>?
    fun putStringSet(key: String, value: Set<String>?)
    fun getInt(key: String): Int?
    fun putInt(key:String, value: Int?)
    fun getBoolean(key: String): Boolean?
    fun putBoolean(key:String, value: Boolean?)
}