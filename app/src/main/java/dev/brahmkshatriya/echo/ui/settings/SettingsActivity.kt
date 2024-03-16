package dev.brahmkshatriya.echo.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ActivitySettingsBinding

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivitySettingsBinding.inflate(layoutInflater)
    }

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsFragmentContainerView, SettingsFragment(
                    getString(R.string.settings),
                    PreferenceFragment()
                ))
                .commit()
        }
    }

}