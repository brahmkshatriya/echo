package dev.brahmkshatriya.echo.app.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.playback.MediaQuality
import dev.brahmkshatriya.echo.app.playback.PlayableMedia
import dev.brahmkshatriya.kaudiooutput.AudioOutputKind
import dev.brahmkshatriya.kaudiooutput.AudioOutputSelection
import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_graphic_eq
import echo.client.generated.resources.ic_headphones
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private enum class AudioSettingsSheet {
    Quality,
    Output,
}

/** Optional UI content supplied by a host that requires a system route picker. */
val LocalAudioOutputPicker =
    staticCompositionLocalOf<(@Composable (AudioOutputs) -> Unit)?> { null }

@Composable
fun PlayerAudioSettings(
    entryId: Int,
    media: PlayableMedia,
    quality: MediaQuality,
    outputs: AudioOutputs,
    onQualitySelected: (String) -> Unit,
    onOutputSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openSheet by remember(entryId) { mutableStateOf<AudioSettingsSheet?>(null) }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(100))
                .background(colors.onPrimaryContainer.copy(alpha = 0.1f))
                .clickable(role = Role.Button, onClickLabel = "Choose audio quality") {
                    openSheet = AudioSettingsSheet.Quality
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Audio quality, ${quality.format}"
                }
                .heightIn(min = 40.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(Res.drawable.ic_graphic_eq), null, Modifier.size(18.dp))
            Text(quality.format, style = MaterialTheme.typography.labelLarge)
        }
        Row(
            Modifier.clip(RoundedCornerShape(100))
                .background(colors.onPrimaryContainer.copy(alpha = 0.1f))
                .clickable(role = Role.Button, onClickLabel = "Choose audio output") {
                    openSheet = AudioSettingsSheet.Output
                }
                .semantics { contentDescription = "Audio output" }
                .heightIn(min = 40.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(Res.drawable.ic_headphones), null, Modifier.size(20.dp))
        }
    }

    val sheet = openSheet ?: return
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    val scope = rememberCoroutineScope()
    fun dismissSheet() {
        scope.launch {
            sheetState.hide()
            openSheet = null
        }
    }
    ModalBottomSheet(onDismissRequest = { openSheet = null }, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                if (sheet == AudioSettingsSheet.Quality) "Audio quality" else "Audio output",
                Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                if (sheet == AudioSettingsSheet.Quality) media.title
                else "Choose where Echo sends audio",
                Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(Modifier.selectableGroup()) {
                when (sheet) {
                    AudioSettingsSheet.Quality -> {
                        media.availableQualities.forEach { option ->
                            AudioSettingsOption(
                                option.label,
                                option.description(),
                                option.id == quality.id,
                            ) {
                                onQualitySelected(option.id)
                                dismissSheet()
                            }
                        }
                        if (media.availableQualities.size == 1)
                            SettingsHint("This song's source provides one quality.")
                    }
                    AudioSettingsSheet.Output -> {
                        val outputState by outputs.state.collectAsState()
                        when (outputState.selection) {
                            AudioOutputSelection.Direct -> {
                                AudioSettingsOption(
                                    "System default",
                                    "Automatically follows your device's output",
                                    outputState.selectedDeviceId == null,
                                ) {
                                    onOutputSelected(null)
                                    dismissSheet()
                                }
                                outputState.devices.forEach { output ->
                                    AudioSettingsOption(
                                        output.name,
                                        output.kind.description(),
                                        output.id == outputState.selectedDeviceId,
                                    ) {
                                        onOutputSelected(output.id)
                                        dismissSheet()
                                    }
                                }
                                SettingsHint(
                                    "Connect headphones, a Bluetooth speaker or a USB audio device to see it here."
                                )
                            }
                            AudioOutputSelection.SystemPicker -> {
                                outputState.devices.forEach { output ->
                                    Text(
                                        output.name,
                                        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    )
                                }
                                val picker = LocalAudioOutputPicker.current
                                if (picker != null) picker(outputs)
                                else
                                    SettingsHint(
                                        "Choose an audio output in your device's sound settings."
                                    )
                            }
                            else ->
                                SettingsHint(
                                    "Choose an audio output in your device's sound settings."
                                )
                        }
                        outputState.error?.let { SettingsHint(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSettingsOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(selected, onClick = null)
    }
}

@Composable
private fun SettingsHint(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun MediaQuality.description(): String =
    listOfNotNull(
            format,
            bitrate?.let { "${it / 1_000} kbps" },
            sampleRate?.let { "${it / 1_000.0} kHz" },
        )
        .joinToString(" · ")

private fun AudioOutputKind.description(): String =
    when (this) {
        AudioOutputKind.Speaker -> "Speaker"
        AudioOutputKind.Headphones -> "Headphones"
        AudioOutputKind.Bluetooth -> "Bluetooth audio"
        AudioOutputKind.Usb -> "USB audio"
        AudioOutputKind.Hdmi -> "HDMI audio"
        AudioOutputKind.AirPlay -> "AirPlay"
        AudioOutputKind.Virtual -> "Virtual output"
        AudioOutputKind.Other -> "Audio output"
    }
