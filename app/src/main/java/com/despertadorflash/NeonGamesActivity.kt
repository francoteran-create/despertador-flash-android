package com.despertadorflash

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal class NeonGameModel : ViewModel() {
    val game = NeonBlocks()
    var playing by mutableStateOf(false)
    var paused by mutableStateOf(false)
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
    private var attached by mutableStateOf(false)
    private var requestedAt = 0L
    private var listening by mutableStateOf(false)
    private var microphone by mutableStateOf("Micrófono apagado")
    private var detected by mutableStateOf("")
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (requiredPermissions().all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED } && model.playing) {
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
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = yellow, secondary = cyan, tertiary = magenta,
                onPrimary = Color.Black, background = Color.Black, surface = nearBlack,
                onSurface = Color.White)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    if (model.playing) {
                        BlockScreen(model, microphone, detected, listening || attached, ::toggleListening, ::openSector) {
                            if (it > model.record) {
                                model.record = it
                                scores.edit().putInt("record", it).apply()
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            NeonHeader("Juegos", ::finish)
                            OutlinedButton(onClick = {
                                model.update { reset() }
                                model.paused = false
                                model.session++
                                model.playing = true
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
                                Text("Bloques de neón", fontFamily = graffiti, fontSize = 21.sp)
                            }
                            Text("Récord personal: ${model.record}", color = cyan)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        settings.registerOnSharedPreferenceChangeListener(events)
    }

    override fun onPause() {
        stopGameListening()
        super.onPause()
    }

    override fun onStop() {
        model.paused = true
        settings.unregisterOnSharedPreferenceChangeListener(events)
        super.onStop()
    }

    private fun openSector() {
        stopGameListening()
        model.playing = false
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun toggleListening() {
        if (attached) {
            stopGameListening()
            return
        }
        val missing = requiredPermissions().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startGameListening() else permissions.launch(missing.toTypedArray())
    }

    private fun startGameListening() {
        if (AlarmEffectsService.activeId >= 0) {
            microphone = "Detené la alarma para escuchar"
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
        if (attached) {
            attached = false
            runCatching { startService(Intent(this, AuroraSoundMonitorService::class.java).setAction(AuroraSoundMonitorService.STOP_GAME)) }
        }
        listening = false
        detected = ""
        microphone = "Micrófono apagado"
    }
}

private val blockColors = listOf(Color.Black, cyan, yellow, magenta, Color(0xFF76E7FF),
    Color(0xFFB6FB69), Color(0xFFFF8BD9), Color(0xFF9895FF))

@Composable
private fun BlockScreen(
    model: NeonGameModel, microphone: String, detected: String, listening: Boolean,
    toggleMic: () -> Unit, finish: () -> Unit, achieved: (Int) -> Unit
) {
    val game = model.game
    @Suppress("UNUSED_VARIABLE") val updates = model.revision
    val rows = game.pending
    val effect = remember { Animatable(1f) }
    var confirmRestart by remember { mutableStateOf(false) }
    BackHandler(onBack = finish)
    LaunchedEffect(model.session, model.paused, game.ended) {
        while (!model.paused && !game.ended) {
            delay(NeonBlocks.GRAVITY_MS)
            if (game.pending.isEmpty()) model.update { lower() }
        }
    }
    LaunchedEffect(rows, model.paused, model.session) {
        if (rows.isNotEmpty() && !model.paused) {
            effect.snapTo(0f)
            val animation = launch { effect.animateTo(1f, tween(NeonBlocks.CLEAR_MS.toInt(), easing = LinearEasing)) }
            delay(NeonBlocks.CLEAR_MS)
            animation.cancel()
            model.update { removeLines() }
            achieved(game.score)
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp)) {
        NeonHeader("Bloques de neón", finish) {
            NeonOverflow("Opciones de la partida",
                linkedMapOf("pause" to if (model.paused) "Continuar" else "Pausar", "reset" to "Reiniciar", "exit" to "Salir")) { route ->
                when (route) {
                    "pause" -> model.paused = !model.paused
                    "reset" -> { confirmRestart = true; model.paused = true }
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
                        GameDetails(game.score, game.next, model.record, microphone, detected, listening, toggleMic)
                        PlayControls(model)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    GameDetails(game.score, game.next, model.record, microphone, detected, listening, toggleMic)
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
                    model.update { reset() }
                    model.session++
                    model.paused = false
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("Cancelar") } })
    }
}

@Composable
private fun GameDetails(score: Int, next: BlockShape, record: Int, microphone: String, detected: String, listening: Boolean, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
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
            if (model.game.ended) { model.update { reset() }; model.session++ }
            model.paused = false
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (model.game.ended) "Jugar otra vez" else "Continuar partida")
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        TextButton(onClick = { model.update { shift(-1) } }, enabled = enabled, modifier = controlSize("Mover a la izquierda")) { Text("←", fontSize = 23.sp) }
        TextButton(onClick = { model.update { turn() } }, enabled = enabled, modifier = controlSize("Girar pieza")) { Text("↻", fontSize = 23.sp) }
        TextButton(onClick = { model.update { lower() } }, enabled = enabled, modifier = controlSize("Bajar pieza")) { Text("↓", fontSize = 23.sp) }
        TextButton(onClick = { model.update { shift(1) } }, enabled = enabled, modifier = controlSize("Mover a la derecha")) { Text("→", fontSize = 23.sp) }
    }
}

private fun controlSize(label: String) =
    Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = label }

@Composable
private fun GameBoard(game: NeonBlocks, revision: Int, effect: Animatable<Float, *>, modifier: Modifier) {
    val pieces = remember(revision) { game.active?.positions().orEmpty() }
    val rows = game.pending
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
        pieces.forEach { cell -> neonCell(left + cell.x * step, top + cell.y * step, step, blockColors[game.active?.shape?.ink ?: 0]) }
        drawRect(Brush.linearGradient(listOf(cyan, magenta, yellow)), Offset(left, top), field, style = Stroke(1.5f))
        val progress = effect.value
        rows.forEach { row ->
            val y = top + row * step
            drawRect(Brush.horizontalGradient(listOf(cyan, yellow, magenta)), Offset(left, y),
                Size(field.width, step), alpha = .44f + .36f * (1f - progress))
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
