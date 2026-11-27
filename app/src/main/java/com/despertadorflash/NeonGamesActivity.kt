package com.despertadorflash

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal class NeonGameModel : ViewModel() {
    val game = NeonBlocks()
    var screen by mutableIntStateOf(0)
    var paused by mutableStateOf(true)
        private set
    var blockHaptics by mutableStateOf(false)
    val truco = TrucoSession()
    private var elapsed = 0L
    private var runningSince = SystemClock.elapsedRealtime()

    fun clock() = elapsed + if (paused) 0L else SystemClock.elapsedRealtime() - runningSince

    fun pause(value: Boolean) {
        if (value == paused) return
        elapsed = clock()
        runningSince = SystemClock.elapsedRealtime()
        paused = value
    }
    var revision by mutableIntStateOf(0)
    var record by mutableIntStateOf(0)
    var session by mutableIntStateOf(0)

    fun update(move: NeonBlocks.() -> Unit) {
        game.move()
        revision++
    }
}

class NeonGamesActivity : ComponentActivity() {
    private val model by viewModels<NeonGameModel>()
    private val settings by lazy { getSharedPreferences("aurora", MODE_PRIVATE) }
    private val scores by lazy { getSharedPreferences("aurora_neon_blocks", MODE_PRIVATE) }
    private val trucoPrefs by lazy { getSharedPreferences("aurora_truco", MODE_PRIVATE) }
    private var voice: TextToSpeech? = null
    private var voiceReady = false
    private var pendingVoice: Pair<String, Long>? = null
    @Volatile private var quietUntil = 0L
    private var audioHandoff: Job? = null
    private var attached by mutableStateOf(false)
    private var handingOff by mutableStateOf(false)
    private var requestedAt = 0L
    private var listening by mutableStateOf(false)
    private var microphone by mutableStateOf("Micrófono apagado")
    private var detected by mutableStateOf("")
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (requiredPermissions().all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED } && model.screen > 0) {
            startGameListening()
        } else microphone = "Permisos de escucha pendientes"
    }
    private val events = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            AuroraSoundMonitorService.KEY_GAME_ACTIVE, AuroraSoundMonitorService.KEY_ACTIVE,
            AuroraSoundMonitorService.KEY_ERROR, AuroraSoundMonitorService.KEY_GAME_LAST -> updateMicrophone()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.record = maxOf(model.record, scores.getInt("record", 0))
        model.blockHaptics = scores.getBoolean("haptics", false)
        setContent {
            val playerName = remember { NeonIdentity.displayName(this@NeonGamesActivity) }
            MaterialTheme(colorScheme = darkColorScheme(primary = yellow, secondary = cyan, tertiary = magenta,
                onPrimary = Color.Black, background = Color.Black, surface = nearBlack,
                onSurface = Color.White)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    when (model.screen) {
                        1 -> BlockScreen(model, playerName, microphone, detected, listening || attached, ::toggleListening, ::openSector,
                            onHaptics = { model.blockHaptics = it; scores.edit().putBoolean("haptics", it).apply() },
                            achieved = {
                                if (it > model.record) {
                                    model.record = it
                                    scores.edit().putInt("record", it).apply()
                                }
                            })
                        2 -> TrucoScreen(model.truco, trucoPrefs, microphone, detected, listening || attached || handingOff,
                            ::toggleListening, ::openSector, ::speakTruco)
                        else -> GamesCover(model.record, playerName, ::finish, onBlocks = {
                            model.update { reset(model.clock()) }
                            model.pause(false)
                            model.session++
                            model.screen = 1
                        }, onTruco = { model.screen = 2 })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        settings.registerOnSharedPreferenceChangeListener(events)
    }

    override fun onResume() {
        super.onResume()
        model.truco.paused = false
    }

    override fun onPause() {
        model.truco.paused = true
        pendingVoice = null
        voice?.stop()
        stopGameListening()
        super.onPause()
    }

    override fun onStop() {
        model.pause(true)
        settings.unregisterOnSharedPreferenceChangeListener(events)
        super.onStop()
    }

    private fun openSector() {
        stopGameListening()
        pendingVoice = null
        voice?.stop()
        model.pause(true)
        model.screen = 0
    }

    override fun onDestroy() {
        voice?.shutdown()
        voice = null
        super.onDestroy()
    }

    private fun speakTruco(line: String, enabled: Boolean) {
        if (!enabled || attached || handingOff || settings.getBoolean(AuroraSoundMonitorService.KEY_ACTIVE, false)) return
        if (voice == null) {
            pendingVoice = line to SystemClock.elapsedRealtime()
            voice = TextToSpeech(applicationContext) { result ->
                runOnUiThread {
                    val engine = voice ?: return@runOnUiThread
                    if (result == TextToSpeech.SUCCESS) {
                        val spanish = engine.voices.orEmpty().filter {
                            it.locale.language == "es" && !it.isNetworkConnectionRequired &&
                                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
                        }.sortedByDescending { it.locale == Locale("es", "AR") }.firstOrNull()
                        voiceReady = spanish != null && engine.setVoice(spanish) == TextToSpeech.SUCCESS
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) { quietUntil = SystemClock.elapsedRealtime() + 320 }
                            @Deprecated("Android legacy callback")
                            override fun onError(utteranceId: String?) { quietUntil = SystemClock.elapsedRealtime() + 320 }
                        })
                    }
                    val queued = pendingVoice
                    pendingVoice = null
                    if (queued != null && voiceReady && model.screen == 2 && model.truco.match != null &&
                        SystemClock.elapsedRealtime() - queued.second < 1700L) {
                        speakTruco(queued.first, trucoPrefs.getBoolean("voice", false))
                    }
                }
            }
            return
        }
        if (voiceReady) {
            quietUntil = SystemClock.elapsedRealtime() + 420
            voice?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "truco-rival")
        }
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun toggleListening() {
        if (attached || handingOff) {
            stopGameListening()
            return
        }
        val missing = requiredPermissions().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startGameListening() else permissions.launch(missing.toTypedArray())
    }

    private fun startGameListening() {
        pendingVoice = null
        if (AlarmEffectsService.activeId >= 0) {
            microphone = "Detené la alarma para escuchar"
            return
        }
        if (voice?.isSpeaking == true || SystemClock.elapsedRealtime() < quietUntil) {
            voice?.stop()
            quietUntil = 0
            handingOff = true
            microphone = "Iniciando escucha…"
            audioHandoff = lifecycleScope.launch {
                delay(440)
                quietUntil = 0
                handingOff = false
                startGameListening()
            }
            return
        }
        microphone = "Iniciando escucha…"
        detected = ""
        requestedAt = SystemClock.elapsedRealtime()
        try {
            ContextCompat.startForegroundService(this,
                Intent(this, AuroraSoundMonitorService::class.java).setAction(AuroraSoundMonitorService.START_GAME))
            attached = true
        } catch (_: IllegalStateException) {
            microphone = "Android no permitió iniciar la escucha"
        } catch (_: SecurityException) {
            microphone = "Revisá el permiso de micrófono"
        }
    }

    private fun updateMicrophone() {
        if (!attached) return
        listening = settings.getBoolean(AuroraSoundMonitorService.KEY_GAME_ACTIVE, false)
        val error = settings.getString(AuroraSoundMonitorService.KEY_ERROR, "").orEmpty()
        microphone = if (listening) "Escuchando sonidos" else error.ifBlank { "Micrófono apagado" }
        if (!listening && !settings.getBoolean(AuroraSoundMonitorService.KEY_ACTIVE, false)) {
            attached = false
            detected = ""
        } else if (settings.getLong(AuroraSoundMonitorService.KEY_GAME_LAST, 0L) >= requestedAt) {
            detected = settings.getString(AuroraSoundMonitorService.KEY_LAST, "").orEmpty()
        }
    }

    private fun stopGameListening() {
        audioHandoff?.cancel()
        audioHandoff = null
        handingOff = false
        if (attached) {
            attached = false
            runCatching { startService(Intent(this, AuroraSoundMonitorService::class.java).setAction(AuroraSoundMonitorService.STOP_GAME)) }
        }
        listening = false
        detected = ""
        microphone = "Micrófono apagado"
    }
}

internal val blockColors = listOf(Color.Black, cyan, yellow, magenta, Color(0xFF76E7FF),
    Color(0xFFB6FB69), Color(0xFFFF8BD9), Color(0xFF9895FF))

@Composable
private fun BlockScreen(
    model: NeonGameModel, playerName: String, microphone: String, detected: String, listening: Boolean,
    toggleMic: () -> Unit, finish: () -> Unit, onHaptics: (Boolean) -> Unit, achieved: (Int) -> Unit
) {
    val game = model.game
    @Suppress("UNUSED_VARIABLE") val updates = model.revision
    val rows = game.pending
    val effect = remember { Animatable(1f) }
    var confirmRestart by remember { mutableStateOf(false) }
    BackHandler(onBack = finish)
    val view = LocalView.current
    LaunchedEffect(model.session, model.paused, updates) {
        if (!model.paused && !game.ended && rows.isEmpty()) {
            delay((game.nextEvent() - model.clock()).coerceAtLeast(1))
            val prior = game.board
            model.update { tick(model.clock()) }
            if (prior !== game.board && model.blockHaptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    LaunchedEffect(rows, model.paused, model.session) {
        if (rows.isNotEmpty() && !model.paused) {
            effect.snapTo(0f)
            val animation = launch { effect.animateTo(1f, tween(NeonBlocks.CLEAR_MS.toInt(), easing = LinearEasing)) }
            delay(NeonBlocks.CLEAR_MS)
            animation.cancel()
            model.update { removeLines(model.clock()) }
            if (model.blockHaptics) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            achieved(game.score)
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp)) {
        NeonHeader("Bloques de neón", finish) {
            NeonOverflow("Opciones de la partida",
                linkedMapOf("pause" to if (model.paused) "Continuar" else "Pausar", "reset" to "Reiniciar", "haptics" to if (model.blockHaptics) "Desactivar vibración del juego" else "Activar vibración del juego",
                    "exit" to "Salir")) { route ->
                when (route) {
                    "pause" -> model.pause(!model.paused)
                    "reset" -> { confirmRestart = true; model.pause(true) }
                    "haptics" -> onHaptics(!model.blockHaptics)
                    "exit" -> finish()
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight * 1.4f && maxWidth >= 560.dp
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GameBoard(game, updates, effect, Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.weight(.9f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        GameDetails(game.score, game.next, model.record, playerName, microphone, detected, listening, toggleMic)
                        PlayControls(model)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    GameDetails(game.score, game.next, model.record, playerName, microphone, detected, listening, toggleMic)
                    GameBoard(game, updates, effect, Modifier.weight(1f).fillMaxWidth())
                    PlayControls(model)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
    if (confirmRestart) {
        AlertDialog(onDismissRequest = { confirmRestart = false }, title = { Text("¿Reiniciar partida?") },
            text = { Text("El récord personal se conserva.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    model.update { reset(model.clock()) }
                    model.session++
                    model.pause(false)
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("Cancelar") } })
    }
}

@Composable
private fun GameDetails(score: Int, next: BlockShape, record: Int, playerName: String, microphone: String, detected: String, listening: Boolean, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            if (playerName.isNotBlank()) Text(playerName, color = cyan, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            Text("Puntos  $score", color = yellow, fontFamily = graffiti, fontSize = 20.sp)
            Text("Récord  $record", color = cyan, style = MaterialTheme.typography.labelMedium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Próxima", style = MaterialTheme.typography.labelMedium)
            Canvas(Modifier.size(width = 64.dp, height = 38.dp).semantics { contentDescription = "Forma de la próxima pieza" }) {
                val step = min(size.width / 5, size.height / 3)
                next.cells.forEach { cell ->
                    neonCell(size.width / 2 + (cell.x - .5f) * step,
                        size.height / 2 + (cell.y - .6f) * step, step, blockColors[next.ink])
                }
            }
        }
        TextButton(onClick = toggle, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (listening) "DETENER" else "ESCUCHAR", color = cyan)
        }
    }
    Text(detected.ifBlank { microphone }, color = if (detected.isEmpty()) Color(0xFFC6CBD2) else yellow,
        style = MaterialTheme.typography.labelMedium, maxLines = 2,
        modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp).semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun PlayControls(model: NeonGameModel) {
    val enabled = remember(model.revision, model.paused) { !model.paused && !model.game.ended && model.game.pending.isEmpty() }
    if (model.paused || model.game.ended) {
        OutlinedButton(onClick = {
            if (model.game.ended) { model.update { reset(model.clock()) }; model.session++ }
            model.pause(false)
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (model.game.ended) "Jugar otra vez" else "Continuar partida")
        }
    }
    val view = LocalView.current
    fun press(feedback: Int, action: NeonBlocks.() -> Unit) {
        val before = model.game.active
        val board = model.game.board
        model.update(action)
        if (model.blockHaptics && (before != model.game.active || board !== model.game.board)) {
            view.performHapticFeedback(feedback)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        RepeatMove("←", "Mover a la izquierda", enabled) { press(HapticFeedbackConstants.CLOCK_TICK) { shift(-1, model.clock()) } }
        TextButton(onClick = { press(HapticFeedbackConstants.KEYBOARD_TAP) { turn(model.clock()) } },
            enabled = enabled, modifier = controlSize("Girar pieza")) { Text("↻", fontSize = 23.sp) }
        TextButton(onClick = { press(HapticFeedbackConstants.LONG_PRESS) { hardDrop(model.clock()) } },
            enabled = enabled, modifier = controlSize("Caída instantánea")) { Text("↓", fontSize = 23.sp) }
        RepeatMove("→", "Mover a la derecha", enabled) { press(HapticFeedbackConstants.CLOCK_TICK) { shift(1, model.clock()) } }
    }
}

@Composable
private fun RepeatMove(symbol: String, label: String, enabled: Boolean, action: () -> Unit) {
    val invoke by rememberUpdatedState(action)
    var pressed by remember { mutableStateOf(false) }
    Box(controlSize(label).background(if (pressed) Color(0xFF14343D) else nearBlack, RoundedCornerShape(12.dp))
        .semantics {
            role = Role.Button
            if (!enabled) disabled()
            onClick { if (enabled) invoke(); enabled }
        }.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(onPress = {
                pressed = true
                invoke()
                kotlinx.coroutines.coroutineScope {
                    val repeat = launch {
                        delay(175)
                        while (true) { invoke(); delay(62) }
                    }
                    try { tryAwaitRelease() } finally { repeat.cancel(); pressed = false }
                }
            })
        }, contentAlignment = Alignment.Center) {
        Text(symbol, color = if (enabled) cyan else Color.Gray, fontSize = 25.sp)
    }
}

private fun controlSize(label: String) =
    Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = label }

@Composable
private fun GameBoard(game: NeonBlocks, revision: Int, effect: Animatable<Float, *>, modifier: Modifier) {
    val pieces = remember(revision) { game.active?.positions().orEmpty() }
    val rows = game.pending
    val shadow = remember(revision) { game.landing()?.positions().orEmpty() }
    Canvas(modifier.padding(vertical = 4.dp).semantics { contentDescription = "Tablero de bloques" }) {
        val step = min(size.width / NeonBlocks.WIDTH, size.height / NeonBlocks.HEIGHT)
        val left = (size.width - step * NeonBlocks.WIDTH) / 2
        val top = (size.height - step * NeonBlocks.HEIGHT) / 2
        val field = Size(step * NeonBlocks.WIDTH, step * NeonBlocks.HEIGHT)
        drawRect(Color(0xFF090B11), Offset(left, top), field)
        for (row in 0 until NeonBlocks.HEIGHT) {
            for (col in 0 until NeonBlocks.WIDTH) {
                val x = left + col * step
                val y = top + row * step
                drawRect(Color(0xFF171D26), Offset(x, y), Size(step, step), style = Stroke(1f))
                val value = game.board[row][col]
                if (value != 0) neonCell(x, y, step, blockColors[value])
            }
        }
        shadow.forEach { cell ->
            drawRoundRect(cyan.copy(alpha = .38f), Offset(left + cell.x * step + 2f, top + cell.y * step + 2f),
                Size((step - 4f).coerceAtLeast(0f), (step - 4f).coerceAtLeast(0f)), CornerRadius(step * .12f), style = Stroke(1.3f))
        }
        pieces.forEach { cell -> neonCell(left + cell.x * step, top + cell.y * step, step, blockColors[game.active?.shape?.ink ?: 0]) }
        drawRect(Brush.linearGradient(listOf(cyan, magenta, yellow)), Offset(left, top), field, style = Stroke(1.5f))
        val progress = effect.value
        rows.forEach { row ->
            val y = top + row * step
            drawRect(Brush.horizontalGradient(listOf(cyan, yellow, magenta)), Offset(left, y),
                Size(field.width, step), alpha = .62f + .32f * (1f - progress))
            if (progress < 1f) {
                repeat(18) { particle ->
                    val phase = particle * 2.19f
                    val x = left + (particle + .5f) * field.width / 18
                    drawCircle(blockColors[1 + particle % 3].copy(alpha = 1f - progress),
                        radius = step * .095f, center = Offset(x + cos(phase) * step * .7f * progress,
                            y + step / 2 + sin(phase) * step * 1.2f * progress))
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.neonCell(x: Float, y: Float, step: Float, ink: Color) {
    if (step <= 0f) return
    val edge = step * .10f
    val corner = CornerRadius(step * .16f)
    val cell = Size(step - edge * 2, step - edge * 2)
    val start = Offset(x + edge, y + edge)
    drawRoundRect(ink.copy(alpha = .15f), Offset(x, y), Size(step, step), corner)
    drawRoundRect(Brush.linearGradient(listOf(ink, ink.copy(alpha = .33f)), start, Offset(x + step, y + step)), start, cell, corner)
    drawRoundRect(ink.copy(alpha = .8f), start, cell, corner, style = Stroke(step * .055f))
    drawLine(Color.White.copy(alpha = .64f), Offset(x + step * .28f, y + step * .24f),
        Offset(x + step * .72f, y + step * .24f), step * .055f)
}
