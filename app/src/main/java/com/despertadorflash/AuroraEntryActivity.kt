package com.despertadorflash

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Launcher-only entry. Alarm, timer and in-app intents go directly to their destinations. */
class AuroraEntryActivity : ComponentActivity() {
    private var handoff: Job? = null
    private var greeting by mutableStateOf(false)
    private lateinit var displayName: String
    private var gender: NeonGender? = null
    private var birthday = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = NeonIdentity.preferences(this)
        val identity = NeonIdentity.savedDraft(this)
        displayName = prefs.getString("nickname", "").orEmpty().trim().ifBlank { identity.name.trim() }
        gender = identity.gender
        val today = Calendar.getInstance()
        birthday = birthdayToday(prefs.getString("birth", "").orEmpty(), today.get(Calendar.DAY_OF_MONTH),
            today.get(Calendar.MONTH) + 1)
        greeting = savedInstanceState?.getBoolean("greeting") ?: false
        setContent {
            val avatar by produceState<Bitmap?>(null) {
                if (displayName.isNotBlank()) value = withContext(Dispatchers.IO) { NeonIdentity.menuPortrait(this@AuroraEntryActivity, 480) }
            }
            Surface(Modifier.fillMaxSize(), color = Color.Black) {
                Box(Modifier.fillMaxSize().safeDrawingPadding().padding(22.dp), contentAlignment = Alignment.Center) {
                    if (greeting) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            avatar?.let {
                                Image(remember(it) { it.asImageBitmap() }, "Avatar del perfil",
                                    Modifier.fillMaxWidth().heightIn(max = 230.dp).aspectRatio(1f),
                                    contentScale = ContentScale.Fit)
                            }
                            Text(openingSalutation(gender), color = cyan, fontFamily = graffiti, fontSize = 25.sp,
                                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                            Text(displayName, color = Color.White, fontSize = 29.sp, textAlign = TextAlign.Center)
                            if (birthday) Text("¡Feliz cumpleaños!", color = yellow, fontSize = 20.sp)
                        }
                    } else {
                        val phase = neonPhase()
                        Canvas(Modifier.fillMaxSize()) {
                            val glow = .1f + .07f * phase.floatValue
                            drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = glow), Color.Transparent),
                                center = center, radius = size.minDimension * .46f), radius = size.minDimension * .46f)
                        }
                        NeonLogo("interfaz/logo_aurora.png", Modifier.widthIn(max = 460.dp).fillMaxWidth(), opening = true)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handoff = lifecycleScope.launch {
            if (!greeting) delay(ENTRY_MS)
            if (displayName.isNotBlank()) {
                greeting = true
                delay(GREETING_MS)
            }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("greeting", greeting)
        super.onSaveInstanceState(outState)
    }

    companion object {
        internal const val ENTRY_MS = 2000L
        internal const val GREETING_MS = 1450L
    }
}
