package com.despertadorflash

import android.content.SharedPreferences
import android.os.SystemClock
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min

internal class TrucoSession {
    var match by mutableStateOf<TrucoMatch?>(null)
    var revision by mutableIntStateOf(0)
    var phrase by mutableStateOf("")
    var mood by mutableStateOf(RivalMood.CALM)
    var spoken by mutableIntStateOf(0)
    var captionUntil = 0L
        private set
    var dialogue = TrucoDialogue()
        private set
    var counted = false
    var paused by mutableStateOf(false)
    fun start(goal: Int, flor: Boolean) {
        match = TrucoMatch(goal, flor)
        dialogue = TrucoDialogue()
        phrase = ""
        captionUntil = 0L
        mood = RivalMood.CALM
        counted = false
        paused = false
        nextRevision()
    }
    fun nextRevision() { revision++ }
    fun announce(line: String, expression: RivalMood, until: Long) {
        captionUntil = until
        phrase = line
        mood = expression
        spoken++
    }
}

@Composable
internal fun GamesCover(record: Int, playerName: String, back: () -> Unit, onBlocks: () -> Unit, onTruco: () -> Unit) {
    var guide by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NeonHeader("Juegos", back)
        if (playerName.isNotBlank()) Text(playerName, color = cyan, fontFamily = graffiti, fontSize = 17.sp)
        GameCoverCard("Bloques de neón", "Récord personal  $record", cyan, onBlocks) {
            Canvas(Modifier.size(96.dp)) {
                val step = size.width / 5
                repeat(9) { index ->
                    val col = index % 3
                    val row = index / 3
                    if (index != 2 && index != 6) {
                        val point = Offset(step * (col + .5f), step * (row + .4f))
                        val tone = blockColors[1 + (row + col) % 3]
                        drawRoundRect(Brush.linearGradient(listOf(tone, tone.copy(alpha = .22f))),
                            point, Size(step - 3f, step - 3f), CornerRadius(step * .17f))
                    }
                }
            }
        }
        GameCoverCard("Truco argentino", "Mano a mano con la mesa", magenta, onTruco) {
            Box(Modifier.size(width = 86.dp, height = 100.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.graphicsLayer { rotationZ = 19f; translationX = 19.dp.toPx() }) {
                    PlayingCard(Card(Suit.CUPS, 12), 51.dp)
                }
                Box(Modifier.graphicsLayer { rotationZ = -13f; translationX = -12.dp.toPx() }) {
                    PlayingCard(Card(Suit.SWORDS, 1), 56.dp)
                }
            }
        }
        TextButton(onClick = { guide = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cómo jugar a Bloques") }
    }
    if (guide) AlertDialog(onDismissRequest = { guide = false }, title = { Text("Bloques de neón") },
        text = { Text("Completá filas de lado a lado. Mantené una flecha para mover en serie. Girá con ↻ y bajá de golpe con ↓. La vibración opcional está en Opciones de la partida.") },
        confirmButton = { TextButton(onClick = { guide = false }) { Text("Listo") } })
}

@Composable
private fun GameCoverCard(title: String, sub: String, accent: Color, open: () -> Unit, art: @Composable () -> Unit) {
    val shape = RoundedCornerShape(23.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 118.dp).clip(shape)
        .background(Brush.linearGradient(listOf(accent.copy(alpha = .17f), Color(0xFF08090D))))
        .border(1.dp, accent.copy(alpha = .66f), shape).clickable(role = Role.Button, onClick = open).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontFamily = graffiti, fontSize = 21.sp, color = Color.White)
            Text(sub, color = accent, style = MaterialTheme.typography.labelMedium)
        }
        art()
    }
}

@Composable
internal fun TrucoScreen(session: TrucoSession, prefs: SharedPreferences, microphone: String, detected: String,
    listening: Boolean, onListen: () -> Unit, back: () -> Unit, speak: (String, Boolean) -> Unit) {
    var goal by remember { mutableIntStateOf(prefs.getInt("goal", 15).takeIf { it == 30 } ?: 15) }
    var florEnabled by remember { mutableStateOf(prefs.getBoolean("flor", false)) }
    var strength by remember { mutableStateOf(TrucoLevel.entries[prefs.getInt("level", 1).coerceIn(0, 2)]) }
    var character by remember { mutableIntStateOf(prefs.getInt("rival", 0).coerceIn(0, 1)) }
    var voice by remember { mutableStateOf(prefs.getBoolean("voice", false)) }
    var subtitles by remember { mutableFloatStateOf(prefs.getFloat("captions_sp", 23f).coerceIn(17f, 30f)) }
    var seconds by remember { mutableFloatStateOf(prefs.getFloat("caption_seconds", 3.5f).coerceIn(2f, 7f)) }
    var tips by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    var wins by remember { mutableIntStateOf(prefs.getInt("won", 0)) }
    var losses by remember { mutableIntStateOf(prefs.getInt("lost", 0)) }
    val context = LocalContext.current
    val playerName = remember(context) { NeonIdentity.displayName(context).ifBlank { "Vos" } }
    val avatarPair by produceState<Pair<Bitmap?, Bitmap?>>(null to null, character) {
        value = withContext(Dispatchers.IO) {
            val first = NeonIdentity.menuPortrait(context, 160)
            val identity = NeonIdentity.savedDraft(context)
            val gender = if (character == 0) NeonGender.WOMAN else NeonGender.MAN
            val source = NeonIdentity.catalog(context)?.forGender(gender).orEmpty()
                .firstOrNull { it.id != identity.avatar }
            first to source?.let { NeonIdentity.bitmap(context, it.asset, 160) }
        }
    }
    val strategy = remember(strength) { TrucoStrategy(strength) }
    val match = session.match
    val revision = session.revision
    val player = remember(match, revision) { match?.view(Seat.HUMAN) }
    val opponent = if (character == 0) "La Rusa" else "El Pampa"
    var caption by remember { mutableStateOf(session.phrase) }

    BackHandler {
        if (match != null) {
            session.match = null
            session.phrase = ""
        } else back()
    }
    fun announce(line: String?, mood: RivalMood) {
        session.mood = mood
        if (line != null) {
            session.announce(line, mood, SystemClock.elapsedRealtime() + (seconds * 1000).toLong())
            speak(line, voice)
        }
    }

    fun afterHumanAction(active: TrucoMatch) {
        val outcome = active.matchWinner ?: active.handWinner
        if (outcome != null && SystemClock.elapsedRealtime() >= session.captionUntil) {
            val humanWon = outcome == Seat.HUMAN
            announce(session.dialogue.line(if (humanWon) RivalMoment.LOST else RivalMoment.WON, null,
                SystemClock.elapsedRealtime(), chance = 1f), if (humanWon) RivalMood.WORRIED else RivalMood.HAPPY)
        }
        session.nextRevision()
    }

    LaunchedEffect(match, match?.handNumber) {
        if (match != null) {
            val trailing = match.score(Seat.HUMAN) - match.score(Seat.CPU) >= 3
            val now = SystemClock.elapsedRealtime()
            announce(session.dialogue.opening(match.handNumber, now, trailing, now >= session.captionUntil),
                if (trailing) RivalMood.CHEEKY else RivalMood.CALM)
        }
    }
    LaunchedEffect(match, revision, session.paused) {
        if (match == null || session.paused || match.matchWinner != null || match.handWinner != null ||
            match.turn != Seat.CPU) return@LaunchedEffect
        val remaining = (session.captionUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        delay(maxOf(620, remaining))
        val before = match.view(Seat.CPU)
        val move = strategy.choose(before)
        val valid = when(val act = move.action) {
            is TrucoAction.Say -> match.call(Seat.CPU, act.call)
            is TrucoAction.Use -> match.play(Seat.CPU, act.card)
        }
        if (valid) {
            val result = match.matchWinner ?: match.handWinner
            val moment = rivalMoment(before, move, result)
            val call = (move.action as? TrucoAction.Say)?.call
            val line = session.dialogue.line(moment, call, SystemClock.elapsedRealtime(),
                chance = if (result != null) 1f else if (call != null) .38f else .56f)
            val mood = when(result) { Seat.CPU -> RivalMood.HAPPY; Seat.HUMAN -> RivalMood.WORRIED; else -> move.mood }
            announce(line, mood)
            session.nextRevision()
        }
    }
    LaunchedEffect(session.spoken, match) {
        caption = if (match == null) "" else session.phrase
        if (caption.isNotBlank()) {
            delay((session.captionUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            caption = ""
            session.phrase = ""
        }
    }
    LaunchedEffect(match, revision) {
        if (match?.matchWinner != null && !session.counted) {
            session.counted = true
            if (match.matchWinner == Seat.HUMAN) wins++ else losses++
            prefs.edit().putInt("won", wins).putInt("lost", losses).apply()
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 14.dp)
        .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NeonHeader("Truco argentino", {
            if (match != null) { session.match = null; session.phrase = "" } else back()
        }) {
            NeonOverflow("Opciones de Truco", linkedMapOf("prefs" to "Preferencias", "rules" to "Instrucciones",
                "leave" to "Volver a Juegos")) {
                when(it) { "prefs" -> options = true; "rules" -> tips = true; else -> {session.match = null; session.phrase = ""; back()} }
            }
        }
        if (match == null || player == null) {
            GameCoverCard("Jugar contra $opponent", "$goal puntos · ${strength.title}" +
                if (florEnabled) " · con flor" else " · sin flor", magenta, { session.start(goal, florEnabled) }) {
                Box(Modifier.graphicsLayer { rotationZ = 10f }) { PlayingCard(Card(Suit.COINS, 7), 60.dp) }
            }
            Text("Estadísticas locales", fontFamily = graffiti, color = cyan, fontSize = 19.sp)
            Text("Ganadas  $wins     Perdidas  $losses")
            if (listening) GameListen(microphone, detected, listening, onListen)
        } else {
            GameListen(microphone, detected, listening, onListen)
            Scoreboard(player, opponent, playerName, avatarPair.first)
            Row(Modifier.fillMaxWidth().heightIn(min = 70.dp).testTag("trucoCaption"), verticalAlignment = Alignment.CenterVertically) {
                RivalPortrait(avatarPair.second, session.mood, opponent)
                AnimatedContent(targetState = caption, transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }, label = "Subtítulos del rival") { line ->
                    Text(line.ifBlank { opponent }, color = if (line.isBlank()) Color(0xFFACBAC4) else yellow,
                        fontFamily = if (line.isBlank()) null else graffiti,
                        fontSize = if (line.isBlank()) 14.sp else subtitles.sp, lineHeight = (subtitles + 5).sp)
                }
            }
            TrucoSurface(player, match.handNumber)
            if (match.resolution.isNotBlank()) {
                Text(match.resolution, color = cyan, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (match.matchWinner != null || match.handWinner != null) {
                WinPanel(match.matchWinner ?: match.handWinner!!, match.matchWinner != null) {
                    if (match.matchWinner != null) session.match = null
                    else { if (match.nextHand()) session.nextRevision() }
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().testTag("trucoHand"), contentAlignment = Alignment.Center) {
                    val width = trucoHandCardWidth(maxWidth.value).dp
                    Row(horizontalArrangement = Arrangement.spacedBy(HAND_CARD_GAP.dp)) {
                        player.own.forEachIndexed { index, card ->
                            key(card, match.handNumber) {
                                val play = if (player.turn == Seat.HUMAN && player.bid == null && Call.FLOR !in player.calls) {
                                    {
                                        if (match.play(Seat.HUMAN, card)) afterHumanAction(match)
                                    }
                                } else null
                                ArrivingCard(card, width, index * 85, play)
                            }
                        }
                    }
                }
                Text(if (player.bid != null) "Cantada · ${player.bid.call.text}" else if (player.turn == Seat.HUMAN) "Tu turno" else "Juega $opponent",
                    color = cyan, style = MaterialTheme.typography.labelMedium)
                if (player.calls.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        player.calls.chunked(3).forEach { group ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                group.forEach { call ->
                                    OutlinedButton(onClick = {
                                        if (match.call(Seat.HUMAN, call)) afterHumanAction(match)
                                    }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(4.dp)) {
                                        Text(call.text, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    if (options) {
        AlertDialog(onDismissRequest = { options = false }, title = { Text("Preferencias de Truco") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Puntos de la próxima partida")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30).forEach { n ->
                            FilterChip(goal == n, { goal = n; prefs.edit().putInt("goal", n).apply() }, { Text("$n") })
                        }
                    }
                    Text("Nivel")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrucoLevel.entries.forEach { thisLevel ->
                            FilterChip(strength == thisLevel, { strength = thisLevel; prefs.edit().putInt("level", thisLevel.ordinal).apply() },
                                { Text(thisLevel.title) })
                        }
                    }
                    TextButton(onClick = { character = 1 - character; prefs.edit().putInt("rival", character).apply() },
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Rival: $opponent") }
                    LabeledSwitch("Flor en la próxima partida", florEnabled) { florEnabled = it; prefs.edit().putBoolean("flor", it).apply() }
                    LabeledSwitch("Voz local del rival", voice) { voice = it; prefs.edit().putBoolean("voice", it).apply() }
                    Text("La voz se suspende durante la escucha ambiental. Los subtítulos siguen disponibles.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Tamaño de subtítulos: ${subtitles.toInt()} sp")
                    Slider(subtitles, { subtitles = it }, valueRange = 17f..30f, onValueChangeFinished = {
                        prefs.edit().putFloat("captions_sp", subtitles).apply()
                    })
                    Text("Duración de subtítulos: ${seconds.toInt()} s")
                    Slider(seconds, { seconds = it }, valueRange = 2f..7f, onValueChangeFinished = {
                        prefs.edit().putFloat("caption_seconds", seconds).apply()
                    })
                }
            }, confirmButton = { TextButton(onClick = { options = false }) { Text("Listo") } })
    }
    if (tips) AlertDialog(onDismissRequest = { tips = false }, title = { Text("Truco mano a mano") },
        text = {
            Text("Jugá dos de las tres bazas para ganar la mano. En pardas, manda la primera baza ganada o el mano si empatan las tres. Los tantos del envido se cantan en la primera baza antes de jugar tu carta. Con flor habilitada, tres cartas del mismo palo se declaran. Configurá 15 o 30 puntos y el nivel en Preferencias.")
        }, confirmButton = { TextButton(onClick = { tips = false }) { Text("Listo") } })
}

@Composable
private fun LabeledSwitch(title: String, value: Boolean, choose: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(value, choose, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun GameListen(status: String, detection: String, listening: Boolean, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(detection.ifBlank { status }, color = if (detection.isBlank()) Color(0xFFACBBC4) else yellow,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = toggle, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (listening) "DETENER" else "ESCUCHAR") }
    }
}

@Composable
private fun Scoreboard(view: TrucoView, opponent: String, player: String, avatar: Bitmap?) {
    val frame = RoundedCornerShape(17.dp)
    Row(Modifier.fillMaxWidth().background(Color(0xFF10131B), frame)
        .border(1.dp, Brush.horizontalGradient(listOf(cyan, magenta)), frame).padding(8.dp)
        .testTag("trucoScore"),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RivalPortrait(avatar, null, player, size = 33.dp)
        ScoreSide(player, view.points.getValue(Seat.HUMAN), cyan, Modifier.weight(1f))
        Text("A ${view.goal}", color = yellow, fontSize = 12.sp)
        ScoreSide(opponent, view.points.getValue(Seat.CPU), magenta, Modifier.weight(1f))
    }
}

@Composable
private fun ScoreSide(name: String, score: Int, ink: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(name, color = ink, fontFamily = graffiti, fontSize = 15.sp, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$score", color = ink, fontFamily = graffiti, fontSize = 19.sp, maxLines = 1)
    }
}

@Composable
private fun RivalPortrait(bitmap: Bitmap?, mood: RivalMood?, name: String, size: Dp = 57.dp) {
    val lean by animateFloatAsState(when (mood) {
        RivalMood.CHEEKY -> -4f
        RivalMood.WORRIED -> 3f
        RivalMood.HAPPY -> -2f
        else -> 0f
    }, label = "Gesto del rival")
    Box(Modifier.size(size).border(1.dp, if (mood == null) cyan else magenta, CircleShape),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(remember(bitmap) { bitmap.asImageBitmap() }, name,
            Modifier.fillMaxSize().padding(2.dp).graphicsLayer { rotationZ = lean }, contentScale = ContentScale.Fit)
        else Text(name.take(1), color = cyan, fontFamily = graffiti)
        if (mood != null && mood != RivalMood.CALM) {
            val expression = when (mood) { RivalMood.WORRIED -> "😬"; RivalMood.HAPPY -> "😄"; else -> "😉" }
            Text(expression, fontSize = 18.sp, modifier = Modifier.align(Alignment.BottomEnd)
                .semantics { contentDescription = when (mood) {
                    RivalMood.WORRIED -> "Rival preocupado"; RivalMood.HAPPY -> "Rival sonriente"; else -> "Guiño del rival"
                } })
        }
    }
}

@Composable
internal fun TrucoSurface(view: TrucoView, hand: Int) {
    val outline = RoundedCornerShape(22.dp)
    val visible = view.tricks.lastOrNull()?.plays.orEmpty()
    val displayed = listOfNotNull(view.current).ifEmpty { visible }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("trucoTable").background(
        Brush.radialGradient(listOf(Color(0xFF153338), Color(0xFF0A1015), Color.Black)), outline)
        .border(1.dp, Color(0xFF4A6473), outline).padding(horizontal = 10.dp, vertical = 7.dp)) {
        val width = trucoTableCardWidth(maxWidth.value).dp
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (displayed.isEmpty()) "Mano: " + if (view.mano == Seat.HUMAN) "vos" else "rival"
                    else "Truco · ${view.handStake}", color = cyan, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(view.opponentCount) { index -> key(hand, index) { ArrivingCard(null, 17.dp, index * 65) } }
                }
            }
            Row(Modifier.heightIn(min = width / .68f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TABLE_CARD_GAP.dp)) {
                displayed.forEach { play ->
                    key(hand, play.card) { ArrivingCard(play.card, width, 0) }
                }
            }
            if (view.tricks.isNotEmpty()) Text(view.tricks.mapIndexed { index, trick ->
                "${index + 1} · " + when(trick.winner) { Seat.HUMAN -> "Vos"; Seat.CPU -> "Rival"; null -> "Parda" }
            }.joinToString("    "), color = Color(0xFFC6D5DB), fontSize = 12.sp)
        }
    }
}

@Composable
private fun WinPanel(winner: Seat, match: Boolean, onward: () -> Unit) {
    val progress = remember(winner, match) { Animatable(0f) }
    LaunchedEffect(winner, match) { progress.animateTo(1f, tween(500)) }
    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = .45f + .55f * progress.value },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (winner == Seat.HUMAN) { if (match) "¡Ganaste el partido!" else "¡Buena mano!" }
            else if (match) "Ganó la mesa" else "Mano del rival", color = yellow, fontFamily = graffiti, fontSize = 22.sp)
        TextButton(onClick = onward, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (match) "Cerrar partida" else "Siguiente mano")
        }
    }
}

@Composable
private fun ArrivingCard(card: Card?, width: Dp, waiting: Int, play: (() -> Unit)? = null) {
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(card, waiting) {
        if (waiting > 0) delay(waiting.toLong())
        arrival.animateTo(1f, tween(265))
    }
    Box(Modifier.graphicsLayer {
        translationY = (1 - arrival.value) * -13.dp.toPx()
        rotationZ = (1 - arrival.value) * -5f
        alpha = .2f + .8f * arrival.value
    }) { PlayingCard(card, width, play) }
}

@Composable
internal fun PlayingCard(card: Card?, width: Dp, play: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(9.dp)
    val accent = when (card?.suit) {
        Suit.SWORDS -> Color(0xFF00758D)
        Suit.COINS -> Color(0xFF995500)
        Suit.CUPS -> Color(0xFF942564)
        Suit.CLUBS -> Color(0xFF31673D)
        null -> Color(0xFF344D86)
    }
    Box(Modifier.width(width).aspectRatio(.68f).shadow(4.dp, shape).clip(shape)
        .background(Brush.linearGradient(if (card == null) listOf(Color(0xFF263955), Color(0xFF181423))
            else listOf(Color(0xFFFFF7E3), Color(0xFFE5D2A9))))
        .border(1.dp, if (card == null) cyan else accent.copy(alpha = .7f), shape)
        .then(if (play != null) Modifier.clickable(role = Role.Button, onClickLabel = "Jugar ${card?.label}", onClick = play) else Modifier)
        .then(if (card != null) Modifier.semantics { contentDescription = card.label } else Modifier)) {
        Canvas(Modifier.fillMaxSize()) {
            val margin = size.width * .08f
            drawRoundRect(accent.copy(alpha = .45f), Offset(margin, margin),
                Size(size.width - 2 * margin, size.height - 2 * margin), CornerRadius(margin), style = Stroke(.8f))
            if (card == null) {
                val axis = size.width / 9
                for (col in 1..7) for (row in 1..10) {
                    rotate(45f, Offset(col * axis, row * axis)) {
                        drawRect(Brush.linearGradient(listOf(cyan.copy(alpha = .21f), magenta.copy(alpha = .4f))),
                            Offset(col * axis, row * axis), Size(axis * .65f, axis * .65f))
                    }
                }
            } else if (card.number >= 10) {
                faceArt(accent, card)
            } else {
                val count = card.number
                val rows = (count + 1) / 2
                val unit = min(size.width * if (count == 1) .71f else .35f, size.height * if (count == 1) .47f else .25f)
                repeat(count) { index ->
                    val x = if (count == 1 || index == count - 1 && count % 2 != 0) size.width / 2
                        else size.width * if (index % 2 == 0) .33f else .67f
                    val y = if (rows == 1) size.height * .52f else size.height * (.26f + .47f * (index / 2) / (rows - 1))
                    translate(x - unit / 2, y - unit / 2) {
                        scale(unit / 100f, unit / 100f, pivot = Offset.Zero) { suitInk(card.suit, accent) }
                    }
                }
            }
            drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .48f), Color.Transparent, Color.Transparent)),
                size = size, cornerRadius = CornerRadius(margin))
        }
        if (card != null) {
            val labelInk = Color(0xFF1C2631)
            Text("${card.number}", color = labelInk, fontWeight = FontWeight.Bold,
                fontSize = (width.value / 3.8f).coerceIn(10f, 26f).sp, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 2.dp))
            Text(card.suit.label.replaceFirstChar(Char::uppercase), color = labelInk, fontWeight = FontWeight.SemiBold,
                fontSize = (width.value / 6.4f).coerceIn(8f, 14f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 3.dp, vertical = 3.dp))

        }
    }
}

private fun DrawScope.faceArt(ink: Color, card: Card) {
    val unit = size.width / 110f
    translate(size.width * .075f, size.height * .15f) {
        scale(unit, unit, pivot = Offset.Zero) {
            drawOval(Color(0xFFB18C50).copy(alpha = .27f), Offset(0f, 0f), Size(94f, 119f))
            drawOval(ink.copy(alpha = .68f), Offset(4f, 4f), Size(86f, 112f), style = Stroke(1.9f))
            val robe = Path().apply { moveTo(13f, 92f); lineTo(28f, 58f); lineTo(67f, 58f); lineTo(83f, 94f); lineTo(61f, 112f); lineTo(33f, 112f); close() }
            drawPath(robe, Brush.linearGradient(listOf(ink, Color(0xFF222D47))))
            drawCircle(Color(0xFFEBB47D), 14f, Offset(47f, 39f))
            val crown = Path().apply {
                moveTo(29f, 30f); lineTo(27f, 14f); lineTo(39f, 22f); lineTo(48f, 9f)
                lineTo(58f, 22f); lineTo(68f, 14f); lineTo(64f, 30f); close()
            }
            drawPath(crown, if (card.number == 12) Color(0xFFD09836) else ink)
            drawLine(Color(0xFFF7C86E), Offset(35f, 66f), Offset(64f, 106f), 5f)
            drawLine(Color(0xFFF7C86E), Offset(58f, 65f), Offset(31f, 102f), 2.5f)
            drawCircle(Color(0xFF47322C), 1.7f, Offset(42f, 39f))
            drawCircle(Color(0xFF47322C), 1.7f, Offset(52f, 39f))
            translate(48f, 75f) { scale(.38f, .38f, pivot = Offset.Zero) { suitInk(card.suit, Color(0xFFFFCB5C)) } }
        }
    }
}

private fun DrawScope.suitInk(suit: Suit, ink: Color) {
    val gilt = Color(0xFFDEAA4D)
    when (suit) {
        Suit.COINS -> {
            drawCircle(ink, 37f, Offset(50f, 50f))
            drawCircle(gilt, 29f, Offset(50f, 50f), style = Stroke(5f))
            drawCircle(Color(0xFFFDE9AD), 11f, Offset(45f, 46f), style = Stroke(3f))
        }
        Suit.CUPS -> {
            val bowl = Path().apply { moveTo(16f, 16f); lineTo(84f, 16f); quadraticTo(82f, 58f, 50f, 64f); quadraticTo(18f, 58f, 16f, 16f); close() }
            drawPath(bowl, Brush.verticalGradient(listOf(ink, gilt), 16f, 65f))
            drawLine(ink, Offset(50f, 61f), Offset(50f, 84f), 9f)
            drawLine(gilt, Offset(26f, 87f), Offset(74f, 87f), 8f)
        }
        Suit.SWORDS -> rotate(22f, Offset(50f, 50f)) {
            val blade = Path().apply { moveTo(50f, 6f); lineTo(59f, 61f); lineTo(41f, 61f); close() }
            drawPath(blade, ink)
            drawLine(gilt, Offset(29f, 64f), Offset(71f, 64f), 7f)
            drawLine(ink, Offset(50f, 66f), Offset(50f, 88f), 8f)
        }
        Suit.CLUBS -> rotate(-24f, Offset(50f, 50f)) {
            drawRoundRect(Brush.verticalGradient(listOf(ink, gilt), 14f, 87f), Offset(38f, 13f), Size(26f, 76f), CornerRadius(10f))
            drawLine(ink, Offset(42f, 39f), Offset(21f, 28f), 8f)
            drawLine(ink, Offset(59f, 55f), Offset(79f, 44f), 7f)
        }
    }
}
