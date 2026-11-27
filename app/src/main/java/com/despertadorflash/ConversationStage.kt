package com.despertadorflash

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class ConversationUi(
    val transcript: String = "", val status: String = "", val translated: String = "",
    val listening: Boolean = false, val showTranslation: Boolean = false, val translationStatus: String = ""
)

internal class ConversationStage(activity: SenseActivity) {
    private var state by mutableStateOf(ConversationUi())
    private var avatar by mutableStateOf<Bitmap?>(null)
    private var name by mutableStateOf("")
    val view = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { ConversationScreen(state, avatar, name, activity::performConversationAction) }
    }

    fun update(transcript: String, status: String, translated: String, listening: Boolean, showTranslation: Boolean, translationStatus: String) {
        state = ConversationUi(transcript, status, translated, listening, showTranslation, translationStatus)
    }

    fun identity(image: Bitmap?, visibleName: String) {
        avatar = image
        name = visibleName
    }
}

private val conversationActions = linkedMapOf(
    "languages" to "Idiomas", "translation" to "Traducción",
    "history" to "Historial de esta conversación", "records" to "Registros guardados",
    "diagnostic" to "Diagnóstico", "assist" to "Asistente de imágenes",
    "sounds" to "Escuchar sonidos", "rotate" to "Vista horizontal"
)

@Composable
internal fun ConversationScreen(state: ConversationUi, portrait: Bitmap?, name: String, onAction: (String) -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = yellow, secondary = cyan, tertiary = magenta,
        background = Color.Black, surface = nearBlack, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 18.dp)) {
                NeonHeader("Transcripción", { onAction("back") }) { NeonAvatar(portrait, name) { onAction("profile") } }
                if (state.status.isNotBlank()) Text(state.status, color = cyan,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite })
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 9.dp)) {
                    if (state.showTranslation && maxWidth > 660.dp) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            TranscriptPanel(state.transcript, Modifier.weight(1f).fillMaxHeight(), "Lo que se dijo")
                            TranscriptPanel(state.translated, Modifier.weight(1f).fillMaxHeight(), "Traducción", state.translationStatus)
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            TranscriptPanel(state.transcript, Modifier.weight(1f).fillMaxWidth())
                            if (state.showTranslation) {
                                HorizontalDivider(color = Color(0xFF243039))
                                TranscriptPanel(state.translated, Modifier.weight(.7f).fillMaxWidth(), "Traducción", state.translationStatus)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        StageControl(start = !state.listening, enabled = true,
                            startLabel = "Transcribir", stopLabel = "Detener") {
                            onAction(if (state.listening) "stop" else "start")
                        }
                    }
                    NeonOverflow("Opciones de transcripción", conversationActions, onAction)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TranscriptPanel(text: String, modifier: Modifier, label: String? = null, status: String = "") {
    val scroll = rememberScrollState()
    LaunchedEffect(text, scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    Column(modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, color = cyan, style = MaterialTheme.typography.labelLarge)
        if (status.isNotBlank()) Text(status, color = Color(0xFFB5BEC8), style = MaterialTheme.typography.bodySmall)
        SelectionContainer {
            Text(text.ifBlank { "Esperando conversación…" }, fontSize = 27.sp, lineHeight = 36.sp,
                color = Color(0xFFF5F5FA))
        }
    }
}
