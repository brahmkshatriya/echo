package dev.brahmkshatriya.echo.app.ui.errors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.brahmkshatriya.echo.app.playback.PlayerSession
import dev.brahmkshatriya.echo.app.platform.setPlainText
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_back
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Stable
internal class PlayerSnackbarAnchors {
    val tickerBounds = mutableStateMapOf<Int, Rect>()
    fun clear() = tickerBounds.clear()
}
internal val LocalPlayerSnackbarAnchors = staticCompositionLocalOf<PlayerSnackbarAnchors> {
    error("PlayerBottomSheet must provide snackbar anchors")
}

@Composable
internal fun Modifier.playerTickerAnchor(entryId: Int): Modifier {
    val anchors = LocalPlayerSnackbarAnchors.current
    DisposableEffect(anchors, entryId) { onDispose { anchors.tickerBounds.remove(entryId) } }
    return onGloballyPositioned { anchors.tickerBounds[entryId] = it.boundsInRoot() }
}

/** One host above the sheet and all overlays; geometry is read only during placement. */
@Composable
internal fun PlaybackErrors(
    session: PlayerSession,
    sheet: BetterSheet,
    anchors: PlayerSnackbarAnchors,
    startPadding: androidx.compose.ui.unit.Dp,
    endPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    var detailsTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var detailsTrace by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(session, sheet.snackBarHostState) {
        snapshotFlow { session.pendingErrors.firstOrNull() }.filterNotNull().collectLatest { error ->
            val result = sheet.snackBarHostState.showSnackbar(
                message = error.title.replace('\n', ' '), actionLabel = "View",
                withDismissAction = true, duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                detailsTitle = error.title
                detailsTrace = error.details
            }
            session.dismissError(error.id)
        }
    }
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val safeBottom = safePadding.calculateBottomPadding()
    val safeTop = safePadding.calculateTopPadding()
    Layout(
        content = {
            SnackbarHost(sheet.snackBarHostState) { data ->
                PlaybackErrorSnackbar(data)
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val start = startPadding.roundToPx() + gap
        val end = endPadding.roundToPx() + gap
        val child = measurables.single().measure(constraints.copy(
            minWidth = 0, minHeight = 0, maxWidth = (constraints.maxWidth - start - end).coerceAtLeast(0),
        ))
        val baseBottom = constraints.maxHeight - safeBottom.roundToPx() - bottomPadding.roundToPx() - gap
        val fallbackExpandedBottom = constraints.maxHeight - safeBottom.roundToPx() - 64.dp.roundToPx() - gap
        layout(constraints.maxWidth, constraints.maxHeight) {
            val progress = sheet.progressState.value
            val tickerTop = anchors.tickerBounds[session.currentEntryId]?.top
            val expandedBottom = tickerTop?.takeIf {
                it >= child.height + safeTop.roundToPx() + gap && it <= constraints.maxHeight
            }?.minus(gap)?.roundToInt() ?: fallbackExpandedBottom
            val collapsedBottom = if (session.hasQueue)
                minOf(baseBottom, sheet.offsetState.value.roundToInt() - gap) else baseBottom
            val expansion = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
            val bottom = if (progress <= 0f) collapsedBottom else
                (collapsedBottom + (expandedBottom - collapsedBottom) * expansion).roundToInt()
            val y = (bottom - child.height).coerceAtLeast(safeTop.roundToPx() + gap)
            child.placeRelative(start, y)
        }
    }
    detailsTitle?.let { title ->
        ErrorDetailsPage(title, detailsTrace, onBack = { detailsTitle = null })
    }
}

@Composable
private fun PlaybackErrorSnackbar(data: SnackbarData) {
    val appColors = MaterialTheme.colorScheme
    val fallbackColors = remember { darkColorScheme() }
    val colors = if (appColors.surface.luminance() < 0.5f) appColors else fallbackColors
    MaterialTheme(colorScheme = colors) {
        Snackbar(
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
            actionOnNewLine = true,
            action = {
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (data.visuals.withDismissAction) {
                        TextButton(
                            onClick = data::dismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) { Text("Dismiss") }
                    }
                    data.visuals.actionLabel?.let { label ->
                        FilledTonalButton(
                            onClick = data::performAction,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        ) { Text(label) }
                    }
                }
            },
        ) {
            Text(data.visuals.message, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ErrorDetailsPage(title: String, details: String, onBack: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Playback error") },
                        navigationIcon = { IconButton(onClick = onBack) {
                            Icon(painterResource(Res.drawable.ic_back), contentDescription = "Back")
                        } },
                        actions = {
                            TextButton(onClick = {
                                scope.launch { clipboard.setPlainText(details) }
                            }) { Text("Copy") }
                        },
                    )
                },
            ) { padding ->
                SelectionContainer {
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall)
                        Text(details, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
