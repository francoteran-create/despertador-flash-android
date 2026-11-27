package com.despertadorflash

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun BirthdayMotion(modifier: Modifier) {
    val progress = neonPhase()
    Canvas(modifier) {
        val phase = progress.floatValue
        val tones = listOf(cyan, magenta, yellow)
        listOf(.11f, .22f, .78f, .89f).forEachIndexed { i, x ->
            val px = size.width * x
            val py = size.height * (.26f + i % 2 * .43f - phase * .11f)
            val r = size.minDimension * .047f
            val color = tones[i % tones.size]
            drawOval(color.copy(alpha = .46f), Offset(px - r, py - r), Size(2 * r, 2.56f * r))
            drawOval(color, Offset(px - r, py - r), Size(2 * r, 2.56f * r), style = Stroke(1.6f))
            drawCircle(Color.White.copy(alpha = .62f), r * .16f, Offset(px - r * .35f, py - r * .3f))
            drawLine(color.copy(alpha = .75f), Offset(px, py + r * 1.56f), Offset(px - r * .3f, py + r * 3.2f), 1.7f)
        }
        repeat(30) { index ->
            val x = size.width * ((index * 29 % 97) / 97f)
            val y = size.height * ((index * 43 % 101) / 101f + phase * (.08f + index % 4 * .02f))
            drawLine(tones[index % 3].copy(alpha = .52f), Offset(x, y), Offset(x + 4f, y + 6f), 2.7f)
        }
        listOf(Offset(size.width * .22f, size.height * .20f), Offset(size.width * .79f, size.height * .28f)).forEachIndexed { i, at ->
            val lead = if (i == 0) phase else phase * .8f
            val reach = size.minDimension * (.06f + lead * .11f)
            for (ray in 0..11) {
                val angle = ray * .523599f
                val direction = Offset(cos(angle), sin(angle))
                drawLine(tones[(ray + i) % 3].copy(alpha = .62f - lead * .32f), at + direction * (reach * .5f),
                    at + direction * reach, 2f)
            }
        }
    }
}
