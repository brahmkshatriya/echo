package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.copyToClipboard
import dev.brahmkshatriya.echo.utils.prefs.LongClickPreference

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

    }
}