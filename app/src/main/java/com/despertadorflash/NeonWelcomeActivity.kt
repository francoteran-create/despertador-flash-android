package com.despertadorflash

import android.animation.ValueAnimator
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.animation.LinearInterpolator
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class NeonWelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val catalog = NeonIdentity.catalog(this)
        val editing = intent.getBooleanExtra("edit_identity", false)
        if (catalog == null || (!editing && !NeonIdentity.shouldWelcome(this))) {
            finish()
            return
        }
        val initial = NeonIdentity.savedDraft(this)
        enableEdgeToEdge()
        setContent {
            NeonWelcome(catalog, initial, editing, onCancel = { finish() }) { draft, complete ->
                if (draft.validFor(catalog)) {
                    NeonIdentity.save(this, draft)
                    if (editing) setResult(RESULT_OK)
                    else if (complete) startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                }
            }
        }
    }
}

internal val yellow = Color(0xFFFFFF48)
internal val cyan = Color(0xFF39EDFF)
internal val magenta = Color(0xFFFF59DA)
internal val nearBlack = Color(0xFF07090D)

@Composable
internal fun NeonWelcome(
    catalog: NeonCatalog, initial: NeonDraft, editing: Boolean = false, onCancel: () -> Unit = {},
    onFinish: (NeonDraft, Boolean) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(if (editing) 1 else 0) }
    var gender by rememberSaveable { mutableStateOf(initial.gender) }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var selected by rememberSaveable { mutableStateOf(initial.avatar) }
    val firstStep = if (editing) 1 else 0
    val draft = NeonDraft(gender, name, selected)

    BackHandler(step > firstStep) { step-- }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = yellow, onPrimary = Color.Black, secondary = cyan, tertiary = magenta,
        background = Color.Black, surface = nearBlack, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step > 0) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                        NeonLogo(catalog.logo, Modifier.widthIn(max = 184.dp).fillMaxWidth(.48f))
                        TextButton(
                            onClick = { if (step == firstStep) onCancel() else step-- },
                            modifier = Modifier.align(Alignment.CenterStart).heightIn(min = 48.dp)
                        ) { Text(if (step == firstStep) "Cancelar" else "Atrás", color = cyan) }
                    }
                }
                when (step) {
                    0 -> {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            NeonLogo(catalog.logo, Modifier.widthIn(max = 460.dp).fillMaxWidth())
                        }
                        NeonAction("EMPEZAR", onClick = { step = 1 })
                    }
                    1 -> {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeonHeading("Personalizá AURORA")
                            NeonGender.entries.forEach { choice ->
                                FilterChip(
                                    selected = gender == choice,
                                    onClick = {
                                        val changed = draft.selectGender(choice)
                                        gender = changed.gender
                                        selected = changed.avatar
                                    },
                                    label = { Text(choice.label) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                )
                            }
                            OutlinedTextField(
                                value = name, onValueChange = { name = it }, label = { Text("Nombre visible") },
                                singleLine = true, isError = name.trim().length > 100,
                                supportingText = { if (name.trim().length > 100) Text("Máximo 100 caracteres") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        NeonAction("CONTINUAR", enabled = draft.canContinue()) { step = 2 }
                    }
                    2 -> {
                        NeonHeading("Elegí tu avatar")
                        AvatarCarousel(
                            catalog.forGender(requireNotNull(gender)), selected, Modifier.weight(1f),
                            onContinue = { portrait -> selected = portrait.id; step = 3 }
                        )
                    }
                    3 -> {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeonHeading("Todo listo, ${name.trim()}")
                            val portrait = catalog.portraits.first { it.id == selected }
                            NeonImage(portrait.asset, portrait.label, Modifier.fillMaxWidth().height(240.dp))
                        }
                        NeonAction(if (editing) "GUARDAR CAMBIOS" else "COMPLETAR PERFIL") { onFinish(draft, true) }
                        TextButton(onClick = { if (editing) onCancel() else onFinish(draft, false) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (editing) "CANCELAR" else "DEJARLO PARA DESPUÉS", color = cyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarCarousel(
    portraits: List<NeonPortrait>, selected: String?, modifier: Modifier, onContinue: (NeonPortrait) -> Unit
) {
    val pager = rememberPagerState(initialPage = portraits.indexOfFirst { it.id == selected }.coerceAtLeast(0)) {
        portraits.size
    }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            pager, modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp).testTag("neonAvatarPager"),
            contentPadding = PaddingValues(horizontal = 18.dp), pageSpacing = 12.dp, beyondViewportPageCount = 1
        ) { page ->
            val portrait = portraits[page]
            val frame = RoundedCornerShape(24.dp)
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val offset = abs((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                    scaleX = 1f - offset * .05f
                    scaleY = scaleX
                    alpha = 1f - offset * .20f
                }.background(nearBlack, frame)
                    .border(1.dp, Brush.linearGradient(listOf(cyan, magenta, yellow)), frame).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                NeonImage(portrait.asset, portrait.label, Modifier.fillMaxSize())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = pager.currentPage > 0, onClick = {
                scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Anterior") }
            Text("${pager.currentPage + 1} de ${portraits.size}", color = cyan,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(enabled = pager.currentPage < portraits.lastIndex, onClick = {
                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Siguiente") }
        }
        NeonAction("CONTINUAR", enabled = !pager.isScrollInProgress) { onContinue(portraits[pager.settledPage]) }
    }
}

@Composable
internal fun neonPhase(active: Boolean = true): FloatState {
    val owner = LocalLifecycleOwner.current
    val resolver = LocalContext.current.contentResolver
    val phase = remember { mutableFloatStateOf(0f) }
    DisposableEffect(owner, resolver, active) {
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4400
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { phase.floatValue = it.animatedValue as Float }
        }
        fun update() {
            val enabled = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
            if (active && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && enabled) {
                if (!animator.isStarted) animator.start()
            } else {
                animator.cancel()
                phase.floatValue = 0f
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, _ -> update() }
        val scaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = update()
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, scaleObserver
        )
        owner.lifecycle.addObserver(lifecycleObserver)
        update()
        onDispose {
            animator.cancel()
            owner.lifecycle.removeObserver(lifecycleObserver)
            resolver.unregisterContentObserver(scaleObserver)
        }
    }
    return phase
}

private data class LogoLayers(val original: Bitmap, val luminance: Bitmap)

private fun logoMask(bitmap: Bitmap): Bitmap {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = pixel shr 16 and 255
        val g = pixel shr 8 and 255
        val b = pixel and 255
        val brightness = (r * 54 + g * 183 + b * 19) / 256
        val alpha = ((brightness - 18) * 2).coerceIn(0, 255) * (pixel ushr 24) / 255
        pixels[i] = (alpha shl 24) or 0x00FFFFFF
    }
    return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
}

@Composable
internal fun NeonLogo(asset: String, modifier: Modifier, opening: Boolean = false) {
    val context = LocalContext.current
    val phase = neonPhase()
    BoxWithConstraints(modifier.aspectRatio(1312f / 468f), contentAlignment = Alignment.Center) {
        val pixels = with(LocalDensity.current) { maxWidth.roundToPx() }.coerceIn(96, 1000)
        val layers by produceState<LogoLayers?>(null, asset, pixels) {
            value = withContext(Dispatchers.IO) {
                NeonIdentity.bitmap(context, asset, pixels)?.let { LogoLayers(it, logoMask(it)) }
            }
        }
        layers?.let { logo ->
            Box(Modifier.fillMaxSize().graphicsLayer {
                val wave = sin(phase.floatValue * (2 * PI).toFloat())
                translationY = 1.5.dp.toPx() * wave
                rotationZ = .5f * wave
            }) {
                Image(remember(logo) { logo.original.asImageBitmap() }, "AURORA",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Image(remember(logo) { logo.luminance.asImageBitmap() }, null,
                    Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val progress = if (opening) .16f + phase.floatValue * 2.85f else phase.floatValue
                            val center = size.width * (-.20f + 1.40f * progress)
                            drawRect(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, cyan.copy(alpha = .34f), yellow.copy(alpha = .38f), Color.Transparent),
                                    Offset(center - size.width * .14f, 0f),
                                    Offset(center + size.width * .14f, size.height * .52f)
                                ),
                                blendMode = BlendMode.SrcIn
                            )
                        }, contentScale = ContentScale.Fit)
            }
            Canvas(Modifier.matchParentSize()) {
                val alpha = .05f + .04f * (1f + sin(phase.floatValue * (2 * PI).toFloat()))
                drawRect(Brush.radialGradient(
                    listOf(cyan.copy(alpha = alpha), yellow.copy(alpha = alpha * .4f), Color.Transparent),
                    radius = size.width * .53f
                ))
            }
        }
    }
}

@Composable
private fun NeonImage(asset: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val pixels = with(LocalDensity.current) { maxOf(maxWidth, maxHeight).roundToPx() }.coerceIn(64, 1440)
        val bitmap by produceState<Bitmap?>(null, asset, pixels) {
            value = withContext(Dispatchers.IO) { NeonIdentity.bitmap(context, asset, pixels) }
        }
        bitmap?.let {
            Image(remember(it) { it.asImageBitmap() }, label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun NeonHeading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() })
}

@Composable
private fun NeonAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val phase = neonPhase()
    Box(Modifier.fillMaxWidth().drawWithContent {
        val glow = .11f + .07f * (1f + sin(phase.floatValue * (2 * PI).toFloat()))
        if (enabled) {
            drawRoundRect(
                Brush.linearGradient(listOf(cyan.copy(alpha = glow), yellow.copy(alpha = glow))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
            )
        }
        drawContent()
    }.padding(3.dp)) {
        Button(
            onClick = onClick, enabled = enabled, interactionSource = interactions, shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (pressed) Color(0xFF384626) else Color(0xFF141B13),
                contentColor = yellow, disabledContainerColor = Color(0xFF141617), disabledContentColor = Color(0xFFABB0AD)
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).border(
                if (focused) 3.dp else 1.5.dp,
                Brush.linearGradient(if (enabled) listOf(cyan, yellow) else listOf(Color.DarkGray, Color.Gray)),
                shape
            )
        ) { Text(label, fontWeight = FontWeight.Bold) }
    }
}
