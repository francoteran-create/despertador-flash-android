package com.despertadorflash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.sin

internal data class SoundStageReading(val label: String, val score: Int, val accepted: Boolean) {
    val higherConfidence get() = accepted && score >= 65
    val title get() = label.removePrefix("POSIBLE ").ifBlank { "Sonido sin identificar" }
    val qualifier get() = if (higherConfidence) "Mayor confianza · IA $score/100" else "Detección probable · IA $score/100"
}

internal fun soundWave(pcm: FloatArray): FloatArray {
    val points = FloatArray(48)
    if (pcm.isEmpty()) return points
    val count = minOf(pcm.size, 2048)
    val start = pcm.size - count
    for (index in points.indices) {
        val from = start + (index * count / points.size)
        val until = maxOf(from + 1, start + ((index + 1) * count / points.size))
        var peak = 0f
        for (sample in from until until) {
            val value = pcm[sample]
            if (value.isFinite() && abs(value) > abs(peak)) peak = value
        }
        points[index] = (peak * 2.6f).coerceIn(-1f, 1f)
    }
    return points
}

internal class SoundStage(activity: SenseActivity) {
    private var running by mutableStateOf(false)
    private var status by mutableStateOf("Micrófono apagado")
    private var detail by mutableStateOf("")
    private var readings by mutableStateOf(emptyList<SoundStageReading>())
    private val wave = mutableStateOf(FloatArray(48))
    private var signalPresent by mutableStateOf(false)
    val view = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { SoundStageScreen(running, status, detail, readings, wave, signalPresent, activity::performSoundAction) }
    }

    fun status(active: Boolean, info: String, extra: String) {
        running = active
        status = info
        detail = extra
        if (!active) clearSignal()
    }

    fun clearSignal() {
        if (readings.isNotEmpty()) readings = emptyList()
        if (wave.value.any { it != 0f }) wave.value = FloatArray(48)
        signalPresent = false
    }

    fun levels(samples: FloatArray) {
        wave.value = samples
        signalPresent = samples.any { abs(it) > .026f }
    }

    fun detections(items: List<SoundLiveWindow.Item>, confirmed: Map<String, Long>, now: Long) {
        readings = items.map { item ->
            val acceptedAt = confirmed[item.label]
            SoundStageReading(item.label, item.score, acceptedAt != null && now - acceptedAt in 0..SoundLiveWindow.LIVE_MS)
        }
    }
}

internal val graffiti = FontFamily(Font(R.font.permanent_marker))
private val stageColors = darkColorScheme(
    primary = yellow, onPrimary = Color.Black, secondary = cyan, tertiary = magenta,
    background = Color.Black, surface = nearBlack, onSurface = Color.White
)
private val secondaries = linkedMapOf(
    "history" to "Historial de sonidos", "sensitivity" to "Sensibilidad",
    "alerts" to "Configuración de alertas", "vibration" to "Vibración", "flash" to "Flash",
    "diagnostic" to "Diagnóstico", "records" to "Registros guardados", "preferences" to "Preferencias",
    "assist" to "Asistente de fotos y sonidos", "voice" to "Transcripción y traducción",
    "spectrum" to "Espectro e intensidad", "rotate" to "Cambiar orientación"
)

@Composable
internal fun SoundStageScreen(
    active: Boolean, status: String, detail: String, readings: List<SoundStageReading>,
    wave: State<FloatArray>, signalPresent: Boolean, action: (String) -> Unit
) {
    MaterialTheme(colorScheme = stageColors) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
                val compact = maxHeight < 510.dp
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { action("back") },
                            modifier = Modifier.align(Alignment.CenterStart).sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { contentDescription = "Volver a AURORA" }) {
                            Text("‹", color = cyan, fontSize = 27.sp)
                        }
                        NeonLogo("interfaz/logo_aurora.png", Modifier.width(111.dp))
                        Text("SONIDOS", color = cyan, fontSize = 10.sp, letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.CenterEnd).semantics { heading() })
                    }
                    if (compact) {
                        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SoundScene(active, status, detail, readings, wave, signalPresent, Modifier.weight(1f).fillMaxHeight(), true)
                            Column(Modifier.widthIn(max = 280.dp).fillMaxWidth(.39f).padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StageControl(true, !active) { action("start") }
                                StageControl(false, active) { action("stop") }
                            }
                        }
                    } else {
                        SoundScene(active, status, detail, readings, wave, signalPresent, Modifier.weight(1f).fillMaxWidth(), false)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                            Box(Modifier.weight(1f)) { StageControl(true, !active) { action("start") } }
                            Box(Modifier.weight(1f)) { StageControl(false, active) { action("stop") } }
                        }
                    }
                    Box(Modifier.align(Alignment.End)) {
                        NeonOverflow("Opciones de sonidos", secondaries, action)
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundScene(
    active: Boolean, status: String, detail: String, readings: List<SoundStageReading>,
    wave: State<FloatArray>, signalPresent: Boolean, modifier: Modifier, compact: Boolean
) {
    val event = readings.firstOrNull()
    val waveformVisible = active && signalPresent
    val phase = neonPhase(active && (event != null || waveformVisible))
    Column(modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(status, color = if (active) cyan else Color(0xFFD0CDCF), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        SignalWave(wave, Modifier.fillMaxWidth().height(if (compact) 32.dp else 60.dp))
        SoundArtwork(event?.let { soundArt(it.label) } ?: if (waveformVisible) SoundArt.UNKNOWN else SoundArt.IDLE, phase,
            Modifier.size(if (compact) 76.dp else 152.dp))
        Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(event?.title ?: when {
                waveformVisible -> "Sonido sin identificar"
                active -> "Escuchando…"
                else -> "Escuchar sonidos"
            }, fontFamily = graffiti, fontSize = if (compact) 23.sp else 34.sp,
                lineHeight = if (compact) 27.sp else 38.sp, color = Color.White,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            if (event != null) {
                Text(event.qualifier, color = if (event.higherConfidence) cyan else yellow, textAlign = TextAlign.Center)
                if (readings.size > 1) {
                    Text(readings.drop(1).joinToString(" · ") { "${it.label} ${it.score}/100" },
                        color = Color(0xFFD0CDCF), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            } else if (!active && detail.isNotBlank()) {
                Text(detail, color = Color(0xFFD0CDCF), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SignalWave(wave: State<FloatArray>, modifier: Modifier) {
    val progress = remember { Animatable(1f) }
    var start by remember { mutableStateOf(FloatArray(48)) }
    var end by remember { mutableStateOf(FloatArray(48)) }
    LaunchedEffect(wave) {
        snapshotFlow { wave.value }.collectLatest { incoming ->
            if (incoming.all { it == 0f }) {
                start = incoming
                end = incoming
                progress.snapTo(1f)
            } else {
                val lastFraction = progress.value
                start = FloatArray(incoming.size) { i -> start[i] + (end[i] - start[i]) * lastFraction }
                end = incoming
                progress.snapTo(0f)
                progress.animateTo(1f, tween(140))
            }
        }
    }
    Canvas(modifier) {
        val samples = end
        val middle = size.height / 2
        drawLine(Color(0xFF323B41), Offset(0f, middle), Offset(size.width, middle), 1.dp.toPx())
        if (samples.isEmpty() || samples.all { it == 0f }) return@Canvas
        val path = Path()
        val from = start
        val fraction = progress.value
        samples.forEachIndexed { i, sample ->
            val x = size.width * i / (samples.size - 1).coerceAtLeast(1)
            val y = middle + middle * .88f * (from[i] + (sample - from[i]) * fraction)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val colors = Brush.horizontalGradient(listOf(magenta, cyan, yellow))
        drawPath(path, colors, alpha = .25f, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, colors, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun StageControl(
    start: Boolean, enabled: Boolean, startLabel: String = "Escuchar", stopLabel: String = "Detener",
    activate: () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, label = "Control de escucha")
    val tint = if (start) cyan else magenta
    val shape = RoundedCornerShape(21.dp)
    val title = if (start) startLabel else stopLabel
    Button(
        onClick = activate, enabled = enabled, interactionSource = interactions, shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = tint.copy(alpha = .13f), contentColor = tint,
            disabledContainerColor = Color(0xFF121416), disabledContentColor = Color(0xFF92999E)
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .border(if (focused) 3.dp else 1.5.dp,
                Brush.linearGradient(if (enabled) listOf(tint, yellow) else listOf(Color(0xFF50565A), Color(0xFF353A3D))), shape)
            .semantics { contentDescription = if (start) "Iniciar escucha" else "Detener escucha" }
            .testTag(if (start) "soundStart" else "soundStop"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val iconTint = LocalContentColor.current
            Canvas(Modifier.size(28.dp)) { speakerIcon(iconTint, !start) }
            Text(title, fontFamily = graffiti, fontSize = 17.sp)
        }
    }
}
