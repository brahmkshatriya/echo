package dev.brahmkshatriya.echo.common.settings

/**
 * A category of settings. The UI will group the [items] with the following [title].
 *
 * @param title The title of the category.
 * @param key should be ignored.
 * @param items The [Setting]s to be grouped in the category.
 */
data class SettingCategory(
    override val title: String,
    override val key: String,
    val items: List<Setting>
) : Setting