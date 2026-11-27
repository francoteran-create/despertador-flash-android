package com.despertadorflash

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
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
    private var celebrationUntil by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identity = NeonIdentity.savedDraft(this)
        displayName = identity.name
        gender = identity.gender
        celebrationUntil = savedInstanceState?.getLong("birthday_until") ?: 0L
        greeting = savedInstanceState?.getBoolean("greeting") ?: false
        setContent {
            val avatar by produceState<Bitmap?>(null) {
                if (displayName.isNotBlank()) value = withContext(Dispatchers.IO) { NeonIdentity.menuPortrait(this@AuroraEntryActivity, 900) }
            }
            Surface(Modifier.fillMaxSize(), color = Color.Black) {
                Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                    if (greeting) {
                        if (celebrationUntil != 0L) BirthdayMotion(Modifier.fillMaxSize())
                        FullAvatarGreeting(avatar, displayName, gender, celebrationUntil != 0L)
                    } else {
                        val phase = neonPhase()
                        Canvas(Modifier.fillMaxSize()) {
                            val glow = .1f + .07f * phase.floatValue
                            drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = glow), Color.Transparent),
                                center = center, radius = size.minDimension * .46f), radius = size.minDimension * .46f)
                        }
                        NeonLogo("interfaz/logo_aurora.png", Modifier.widthIn(max = 460.dp).fillMaxWidth(.9f), opening = true)
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
                if (celebrationUntil != 0L) {
                    delay((celebrationUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
                } else {
                    delay(GREETING_MS)
                    val today = Calendar.getInstance()
                    val annual = getSharedPreferences("aurora_annual_celebrations", MODE_PRIVATE)
                    val years = annual.getStringSet("years", emptySet()).orEmpty().toSet()
                    val year = today.get(Calendar.YEAR)
                    if (eligibleBirthday(NeonIdentity.preferences(this@AuroraEntryActivity).getString("birth", "").orEmpty(),
                            today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.MONTH) + 1, year, years) &&
                        annual.edit().putStringSet("years", years + year.toString()).commit()
                    ) {
                        celebrationUntil = SystemClock.elapsedRealtime() + BIRTHDAY_MS
                        delay(BIRTHDAY_MS)
                    }
                }
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
        outState.putLong("birthday_until", celebrationUntil)
        super.onSaveInstanceState(outState)
    }

    companion object {
        internal const val ENTRY_MS = 2000L
        internal const val GREETING_MS = 1450L
        internal const val BIRTHDAY_MS = 4000L
    }
}


@Composable
internal fun FullAvatarGreeting(avatar: Bitmap?, name: String, gender: NeonGender?, birthday: Boolean) {
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(Unit) { arrival.animateTo(1f, tween(340)) }
    var captionPixels by remember { mutableIntStateOf(0) }
    val captionHeight = if (captionPixels == 0) 75.dp else with(LocalDensity.current) { captionPixels.toDp() }
    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer {
        alpha = arrival.value
    }) {
        val pictureRoom = centeredWelcomeHeight(maxHeight.value, captionHeight.value).dp
        if (avatar != null) {
            Image(remember(avatar) { avatar.asImageBitmap() }, "Avatar del perfil",
                Modifier.align(Alignment.Center).fillMaxWidth(.90f).height(pictureRoom)
                    .testTag("fullWelcomeAvatar"), contentScale = ContentScale.Fit)
        }
        Column(Modifier.align(if (avatar == null) Alignment.Center else Alignment.BottomCenter)
            .fillMaxWidth(.92f).onSizeChanged { captionPixels = it.height }.padding(top = 3.dp, bottom = 12.dp)
            .testTag("welcomeCaption"), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(openingSalutation(gender), color = cyan, fontFamily = graffiti, fontSize = 23.sp,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text(name, color = yellow, fontFamily = graffiti, fontSize = 27.sp, lineHeight = 31.sp,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (birthday) Text("¡Feliz cumpleaños!", color = Color.White, fontSize = 18.sp)
        }
    }
}
