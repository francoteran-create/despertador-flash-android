package com.despertadorflash

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.min
import kotlin.math.sin

internal enum class SoundArt { DOOR, DOG, SIREN, BELL, HORN, CRY, PHONE, PEOPLE, UNKNOWN, IDLE }

internal fun soundArt(label: String): SoundArt = when {
    "TIMBRE" in label || "CAMPANA" in label -> SoundArt.BELL
    "PUERTA" in label || "PORTAZO" in label || "CAJÓN" in label -> SoundArt.DOOR
    "PERRO" in label || "AULLIDO" in label -> SoundArt.DOG
    "ALARMA" in label || "SIRENA" in label -> SoundArt.SIREN
    "BOCINA" in label -> SoundArt.HORN
    "LLANTO" in label -> SoundArt.CRY
    "TELÉFONO" in label -> SoundArt.PHONE
    "CONVERSACIÓN" in label || "VOZ HUMANA" in label -> SoundArt.PEOPLE
    else -> SoundArt.UNKNOWN
}

@Composable
internal fun SoundArtwork(art: SoundArt, phase: FloatState, modifier: Modifier) {
    Canvas(modifier) {
        val unit = min(size.width, size.height) / 100f
        val wave = sin(phase.floatValue * 6.283185f)
        translate((size.width - 100 * unit) / 2, (size.height - 100 * unit) / 2) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawCircle(cyan.copy(alpha = .055f), 44f, Offset(50f, 50f))
                drawCircle(magenta.copy(alpha = .38f), 43f, Offset(50f, 50f), style = Stroke(.7f))
                drawArc(cyan, -50f, 76f, false, Offset(4f, 4f), Size(92f, 92f), style = Stroke(1.6f))
                drawArc(yellow, 105f, 55f, false, Offset(4f, 4f), Size(92f, 92f), style = Stroke(1.6f))
                translate(wave * 1.5f, wave * 1.3f) {
                    when (art) {
                        SoundArt.DOOR -> {
                            stroke(cyan, 27f, 79f, 27f, 24f, 66f, 24f, 66f, 78f)
                            stroke(yellow, 32f, 74f, 53f, 81f, 53f, 20f, 32f, 29f, 32f, 74f)
                            drawCircle(yellow, 2.2f, Offset(47f, 54f))
                            stroke(magenta, 72f, 39f, 80f, 34f)
                            stroke(magenta, 73f, 49f, 84f, 48f)
                        }
                        SoundArt.DOG -> {
                            stroke(yellow, 24f, 48f, 18f, 29f, 29f, 21f, 42f, 35f, 56f, 33f, 66f, 24f, 72f, 38f, 65f, 53f,
                                62f, 67f, 52f, 76f, 34f, 71f, 24f, 58f, 24f, 48f)
                            drawCircle(cyan, 2.8f, Offset(35f, 49f))
                            drawCircle(cyan, 2.8f, Offset(53f, 48f))
                            stroke(magenta, 36f, 59f, 44f, 56f, 51f, 59f, 45f, 65f, 36f, 59f)
                            stroke(cyan, 73f, 54f, 82f, 48f, 78f, 56f, 87f, 54f)
                        }
                        SoundArt.SIREN -> {
                            stroke(yellow, 23f, 72f, 77f, 72f, 77f, 79f, 23f, 79f, 23f, 72f)
                            stroke(magenta, 30f, 67f, 34f, 49f, 42f, 42f, 57f, 42f, 65f, 49f, 70f, 67f, 30f, 67f)
                            stroke(cyan, 50f, 32f, 50f, 20f)
                            stroke(cyan, 35f, 35f, 27f, 25f)
                            stroke(cyan, 65f, 35f, 73f, 25f)
                        }
                        SoundArt.BELL -> {
                            stroke(yellow, 24f, 64f, 32f, 55f, 34f, 40f, 40f, 33f, 50f, 30f, 60f, 33f, 66f, 40f,
                                68f, 55f, 76f, 64f, 24f, 64f)
                            drawCircle(magenta, 4.5f, Offset(50f, 73f))
                            stroke(cyan, 25f, 34f, 20f, 41f, 18f, 51f)
                            stroke(cyan, 75f, 34f, 80f, 41f, 82f, 51f)
                        }
                        SoundArt.HORN -> {
                            stroke(yellow, 27f, 43f, 41f, 43f, 62f, 30f, 62f, 70f, 41f, 56f, 27f, 56f, 27f, 43f)
                            stroke(magenta, 36f, 56f, 43f, 68f, 37f, 72f, 30f, 58f)
                            stroke(cyan, 69f, 42f, 79f, 35f)
                            stroke(cyan, 70f, 50f, 83f, 50f)
                            stroke(cyan, 69f, 58f, 79f, 65f)
                        }
                        SoundArt.CRY -> {
                            drawCircle(yellow, 23f, Offset(50f, 52f), style = Stroke(3f))
                            stroke(yellow, 40f, 30f, 45f, 23f, 53f, 21f, 57f, 27f, 53f, 35f)
                            stroke(magenta, 36f, 47f, 43f, 50f, 46f, 46f)
                            stroke(magenta, 55f, 46f, 60f, 50f, 66f, 47f)
                            drawOval(magenta, Offset(44f, 57f), Size(12f, 9f), style = Stroke(2f))
                            stroke(cyan, 36f, 56f, 31f, 62f)
                            stroke(cyan, 64f, 56f, 69f, 62f)
                        }
                        SoundArt.PHONE -> {
                            drawRoundRect(yellow, Offset(34f, 22f), Size(33f, 56f), CornerRadius(6f), style = Stroke(3f))
                            stroke(cyan, 44f, 29f, 57f, 29f)
                            drawCircle(cyan, 2f, Offset(50f, 70f))
                            stroke(magenta, 26f, 34f, 20f, 47f, 25f, 62f)
                            stroke(magenta, 73f, 34f, 81f, 47f, 76f, 62f)
                        }
                        SoundArt.PEOPLE -> {
                            stroke(cyan, 23f, 31f, 56f, 31f, 56f, 56f, 40f, 56f, 29f, 66f, 29f, 56f, 23f, 56f, 23f, 31f)
                            stroke(magenta, 62f, 44f, 77f, 44f, 77f, 68f, 71f, 68f, 71f, 78f, 59f, 68f, 47f, 68f)
                            listOf(31f, 39f, 47f).forEach { drawCircle(yellow, 1.6f, Offset(it, 43f)) }
                        }
                        SoundArt.UNKNOWN -> {
                            stroke(yellow, 34f, 35f, 40f, 27f, 51f, 24f, 64f, 28f, 70f, 37f, 67f, 46f,
                                53f, 55f, 50f, 63f, width = 4f)
                            drawCircle(magenta, 3.5f, Offset(49f, 75f))
                        }
                        SoundArt.IDLE -> {
                            stroke(magenta, 18f, 50f, 32f, 50f, 40f, 41f, 47f, 57f, 54f, 44f, 61f, 51f, 82f, 51f)
                            drawCircle(cyan, 25f, Offset(50f, 50f), style = Stroke(1.4f))
                        }
                    }
                }
            }
        }
    }
}

internal fun DrawScope.speakerIcon(tint: Color, muted: Boolean) {
    val unit = size.minDimension / 100f
    scale(unit, unit, pivot = Offset.Zero) {
        stroke(tint, 17f, 40f, 33f, 40f, 52f, 24f, 52f, 76f, 33f, 60f, 17f, 60f, 17f, 40f, width = 7f)
        if (muted) stroke(tint, 13f, 15f, 86f, 86f, width = 8f)
        else {
            stroke(tint, 62f, 37f, 69f, 50f, 62f, 63f, width = 6f)
            stroke(tint, 75f, 27f, 85f, 50f, 75f, 73f, width = 6f)
        }
    }
}

private fun DrawScope.stroke(color: Color, vararg xy: Float, width: Float = 2.8f) {
    val path = Path().apply {
        moveTo(xy[0], xy[1])
        for (index in 2 until xy.size step 2) lineTo(xy[index], xy[index + 1])
    }
    drawPath(path, color.copy(alpha = .20f), style = Stroke(width * 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
