package com.despertadorflash

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class NeonWelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val catalog = NeonIdentity.catalog(this)
        if (catalog == null || !NeonIdentity.shouldWelcome(this)) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            NeonWelcome(catalog, NeonIdentity.preferences(this).getString("name", "").orEmpty()) { draft, complete ->
                if (draft.validFor(catalog)) {
                    NeonIdentity.preferences(this).edit()
                        .putString("name", draft.name.trim())
                        .putString("neon_gender", requireNotNull(draft.gender).name)
                        .putString("neon_avatar", draft.avatar)
                        .remove("neon_legacy_portrait")
                        .putBoolean("neon_welcome_complete", true)
                        .apply()
                    if (complete) startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                }
            }
        }
    }
}

private val yellow = Color(0xFFFFFF48)
private val cyan = Color(0xFF39EDFF)
private val magenta = Color(0xFFFF59DA)
private val nearBlack = Color(0xFF07090D)

@Composable
internal fun NeonWelcome(catalog: NeonCatalog, initialName: String, onFinish: (NeonDraft, Boolean) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var gender by rememberSaveable { mutableStateOf<NeonGender?>(null) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val draft = NeonDraft(gender, name, selected)

    BackHandler(step > 0) { step-- }
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { step-- }) { Text("Atrás", color = cyan) }
                        Text("AURORA", color = cyan, fontWeight = FontWeight.Bold, letterSpacing = 5.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(end = 56.dp))
                    }
                }
                when (step) {
                    0 -> {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            NeonLogo(catalog.logo)
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
                        NeonAction("COMPLETAR PERFIL") { onFinish(draft, true) }
                        TextButton(onClick = { onFinish(draft, false) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("DEJARLO PARA DESPUÉS", color = cyan)
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
            pager, modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 18.dp), pageSpacing = 12.dp
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
            }) { Text("Anterior") }
            Text("${pager.currentPage + 1} de ${portraits.size}", color = cyan)
            TextButton(enabled = pager.currentPage < portraits.lastIndex, onClick = {
                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }) { Text("Siguiente") }
        }
        NeonAction("CONTINUAR", enabled = !pager.isScrollInProgress) { onContinue(portraits[pager.currentPage]) }
    }
}

@Composable
private fun NeonLogo(asset: String) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(1450)) }
    Box(Modifier.fillMaxWidth().heightIn(max = 300.dp).aspectRatio(1.3f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0x5039EDFF), Color(0x26FF59DA), Color.Transparent)),
                radius = size.minDimension / 2f
            )
        }
        NeonImage(asset, "AURORA", Modifier.fillMaxSize().padding(16.dp).graphicsLayer {
            val scale = 1f - .025f * (1f - sweep.value)
            scaleX = scale
            scaleY = scale
        })
        Canvas(Modifier.matchParentSize()) {
            val progress = sweep.value
            val opacity = (1f - abs(progress * 2f - 1f)).coerceIn(0f, 1f)
            if (opacity > 0f) {
                val x = size.width * progress
                drawRect(Brush.linearGradient(
                    listOf(Color.Transparent, cyan.copy(alpha = opacity * .16f), Color.Transparent),
                    start = Offset(x - size.width * .18f, 0f), end = Offset(x + size.width * .18f, size.height * .2f)
                ))
                drawCircle(yellow.copy(alpha = opacity * .65f), 2.dp.toPx(), Offset(size.width * .24f, size.height * .30f))
                drawCircle(magenta.copy(alpha = opacity * .65f), 2.dp.toPx(), Offset(size.width * .74f, size.height * .64f))
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
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}
