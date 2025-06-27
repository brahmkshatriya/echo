package dev.brahmkshatriya.echo.ui.main.settings

import android.content.Context
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.MainApplication.Companion.applyLocale
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.ContextUtils.appVersion
import dev.brahmkshatriya.echo.utils.ContextUtils.copyToClipboard
import dev.brahmkshatriya.echo.utils.ui.prefs.LongClickPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.SwitchLongClickPreference
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MiscFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.misc)
    override val icon get() = R.drawable.ic_info.toResourceImageHolder()
    override val creator = { AboutPreference() }

    class AboutPreference : PreferenceFragmentCompat() {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            LongClickPreference(context).apply {
                val version = appVersion()
                title = getString(R.string.version)
                summary = version
                layoutResource = R.layout.preference
                isIconSpaceReserved = false
                isSelectable = false
                setOnLongClickListener {
                    val info = buildString {
                        appendLine("Echo Version: $version")
                        appendLine("Device: $BRAND $DEVICE")
                        appendLine("Architecture: ${getArch()}")
                        appendLine("OS Version: $CODENAME $RELEASE ($SDK_INT)")
                    }
                    context.copyToClipboard(title?.toString(), info)
                }
                screen.addPreference(this)
            }

            val languages = mapOf("system" to getString(R.string.system)) + languages
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

            SwitchLongClickPreference(context).apply {
                title = getString(R.string.check_for_extension_updates)
                summary = getString(R.string.check_for_extension_updates_summary)
                key = "check_for_extension_updates"
                layoutResource = R.layout.preference_switch
                isIconSpaceReserved = false
                setDefaultValue(true)
                screen.addPreference(this)
                setOnLongClickListener {
                    val viewModel by activityViewModel<ExtensionsViewModel>()
                    viewModel.update(requireActivity(), true)
                }
            }

            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener { pref, key ->
                if (key == "language") applyLocale(pref)
            }
        }

        private fun getArch(): String {
            SUPPORTED_ABIS.firstOrNull()?.let { return it }
            return System.getProperty("os.arch")
                ?: System.getProperty("os.product.cpu.abi")
                ?: "Unknown"
        }
    }

    companion object {
        val languages = mapOf(
            "as" to "Assamese",
            "de" to "Deutsch",
            "fr" to "Français",
            "en" to "English",
            "hi" to "हिन्दी",
            "hng" to "Hinglish",
            "hu" to "Magyar",
            "ja" to "日本語",
            "nb-rNO" to "Norsk bokmål",
            "nl" to "Nederlands",
            "pl" to "Polski",
            "pt" to "Português",
            "ru" to "Русский",
            "sa" to "संस्कृतम्",
            "sr" to "Српски",
            "tr" to "Türkçe",
            "zh-rCN" to "中文 (简体)",
        )
    }
}