package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.copyToClipboard
import dev.brahmkshatriya.echo.utils.prefs.LongClickPreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialListPreference

class AboutFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.about)
    override val transitionName = "about"
    override val creator = { AboutPreference() }


    class AboutPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = context.packageName
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            LongClickPreference(context).apply {
                val version = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                title = getString(R.string.version)
                summary = version
                layoutResource = R.layout.preference
                isIconSpaceReserved = false
                isSelectable = false
                setOnLongClickListener {
                    val info = "Echo Version: $version\n" +
                            "Device: $BRAND $DEVICE\n" +
                            "Architecture: ${getArch()}\n" +
                            "OS Version: $CODENAME $RELEASE ($SDK_INT)"
                    context.copyToClipboard(title?.toString(), info)
                }
                screen.addPreference(this)
            }

            val languages = mapOf(
                "system" to getString(R.string.system),
                "as" to "Assamese",
                "de" to "Deutsch",
                "fr" to "Français",
                "hi" to "हिन्दी",
                "hng" to "Hinglish",
                "ja" to "日本語",
                "nb-rNO" to "Norsk bokmål",
                "nl" to "Nederlands",
                "pl" to "Polski",
                "pt" to "Português",
                "sa" to "संस्कृतम्",
                "tr" to "Türkçe",
                "zh-rCN" to "中文 (简体)",
            )
            MaterialListPreference(context).apply {
                title = getString(R.string.language)
                summary = getString(R.string.language_summary)
                key = "language"
                entries = languages.map { it.value }.toTypedArray()
                entryValues = languages.map { it.key }.toTypedArray()
                layoutResource = R.layout.preference
                isIconSpaceReserved = false
                screen.addPreference(this)
            }
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener { pref, key ->
                if (key == "language") applyLocale(pref)
            }
        }

        private fun getArch(): String {
            SUPPORTED_ABIS.forEach {
                when (it) {
                    "arm64-v8a" -> return "aarch64"
                    "armeabi-v7a" -> return "arm"
                    "x86_64" -> return "x86_64"
                    "x86" -> return "i686"
                }
            }
            return System.getProperty("os.arch")
                ?: System.getProperty("os.product.cpu.abi")
                ?: "Unknown"
        }

        companion object{

            fun applyLocale(sharedPref: SharedPreferences) {
                val value = sharedPref.getString("language", "system") ?: "system"
                val locale = if (value == "system") LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(value)
                AppCompatDelegate.setApplicationLocales(locale)
            }
        }
    }
}