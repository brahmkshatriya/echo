package dev.brahmkshatriya.echo.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import dev.brahmkshatriya.echo.app.ui.player.components.LocalAudioOutputPicker
import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import dev.brahmkshatriya.kaudiooutput.createRoutePickerView

@Composable
fun IosApp() {
    CompositionLocalProvider(
        LocalAudioOutputPicker provides { outputs -> IosAudioOutputPicker(outputs) }
    ) {
        App()
    }
}

@Composable
private fun IosAudioOutputPicker(outputs: AudioOutputs) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Choose audio output", Modifier.weight(1f))
        UIKitView(factory = { outputs.createRoutePickerView() }, modifier = Modifier.size(48.dp))
    }
}
