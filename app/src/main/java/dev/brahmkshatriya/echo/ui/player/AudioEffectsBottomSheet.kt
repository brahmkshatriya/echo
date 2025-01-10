package dev.brahmkshatriya.echo.ui.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.PagerSnapHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogAudioFxBinding
import dev.brahmkshatriya.echo.databinding.FragmentAudioFxBinding
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.BASS_BOOST
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.CHANGE_PITCH
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.CUSTOM_EFFECTS
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.PLAYBACK_SPEED
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.deleteFxPrefs
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.getFxPrefs
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.globalFx
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.speedRange
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.custom.RulerAdapter
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch


class AudioEffectsBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogAudioFxBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAudioFxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel by activityViewModels<PlayerViewModel>()
        var mediaId: String? = null

        fun bind() {
            val settings = requireContext().globalFx()
            settings.edit {
                val customEffects = settings.getStringSet(CUSTOM_EFFECTS, null) ?: emptySet()
                putStringSet(CUSTOM_EFFECTS, customEffects + mediaId)
            }
            binding.audioFxDescription.isVisible = mediaId != null
            binding.audioFxFragment.bind(requireContext(), mediaId) { onEqualizerClicked() }
        }
        observe(viewModel.currentFlow) {
            mediaId = it?.mediaItem?.mediaId
            bind()
        }
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.reset -> {
                    val context = requireContext()
                    val id = mediaId ?: return@setOnMenuItemClickListener false
                    context.deleteFxPrefs(id)
                    bind()
                    true
                }

                else -> false
            }
        }
        PagerSnapHelper().attachToRecyclerView(binding.audioFxFragment.speedRecycler)
    }

    companion object {
        @SuppressLint("SetTextI18n")
        fun FragmentAudioFxBinding.bind(
            context: Context, mediaId: String? = null, onEqualizerClicked: () -> Unit
        ) {
            val settings = context.getFxPrefs(mediaId)
            val speed = settings.getInt(PLAYBACK_SPEED, speedRange.indexOf(1f))
            RulerAdapter(
                speedRecycler,
                List(speedRange.size) { index -> index to (index % 2 == 0) },
                speed,
                { "${speedRange.getOrNull(it) ?: 1f}x" },
            ) {
                speedValue.text = "${speedRange.getOrNull(it) ?: 1f}x"
                settings.edit { putInt(PLAYBACK_SPEED, it) }
            }
            pitchSwitch.isChecked = settings.getBoolean(CHANGE_PITCH, true)
            pitch.setOnClickListener {
                pitchSwitch.isChecked = !pitchSwitch.isChecked
            }
            pitchSwitch.setOnCheckedChangeListener { _, isChecked ->
                settings.edit { putBoolean(CHANGE_PITCH, isChecked) }
            }
            bassBoostSlider.value = settings.getInt(BASS_BOOST, 0).toFloat()
            bassBoostSlider.addOnChangeListener { _, value, _ ->
                settings.edit { putInt(BASS_BOOST, value.toInt()) }
            }
            equalizer.setOnClickListener { onEqualizerClicked() }
        }

        private fun openEqualizer(activity: Activity, sessionId: Int) {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, activity.packageName)
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            activity.startActivityForResult(intent, 0)
        }

        fun Fragment.onEqualizerClicked() {
            val viewModel by activityViewModels<PlayerViewModel>()
            val sessionId = viewModel.audioSessionFlow.value
            runCatching { openEqualizer(requireActivity(), sessionId) }.getOrElse {
                viewModel.run { viewModelScope.launch { throwableFlow.emit(it) } }
            }
        }
    }
}
