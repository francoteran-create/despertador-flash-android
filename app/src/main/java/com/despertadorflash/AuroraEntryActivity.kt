package com.despertadorflash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Launcher-only entry. Alarm, timer and in-app intents go directly to their destinations. */
class AuroraEntryActivity : ComponentActivity() {
    private var handoff: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(Modifier.fillMaxSize(), color = Color.Black) {
                Box(Modifier.fillMaxSize().safeDrawingPadding().padding(22.dp),
                    contentAlignment = Alignment.Center) {
                    val phase = neonPhase()
                    Canvas(Modifier.fillMaxSize()) {
                        val glow = .1f + .07f * phase.floatValue
                        drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = glow), Color.Transparent),
                            center = center, radius = size.minDimension * .46f),
                            radius = size.minDimension * .46f)
                    }
                    NeonLogo("interfaz/logo_aurora.png", Modifier.widthIn(max = 460.dp).fillMaxWidth(), opening = true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handoff = lifecycleScope.launch {
            delay(ENTRY_MS)
            if (!isFinishing && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                startActivity(Intent(this@AuroraEntryActivity, MainActivity::class.java)
                    .setAction(MainActivity.ACTION_OPEN_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            }
        }
    }

    override fun onPause() {
        handoff?.cancel()
        super.onPause()
    }

    companion object { internal const val ENTRY_MS = 1050L }
}
