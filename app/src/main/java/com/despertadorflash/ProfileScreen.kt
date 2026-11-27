package com.despertadorflash

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal data class ProfileDraft(
    val name: String = "",
    val nickname: String = "",
    val birth: String = "",
    val contact: String = "",
    val notes: String = "",
    val cud: Int = 0
)

internal data class ProfileAvatar(val color: Int = 0, val figure: Boolean = false, val photo: Bitmap? = null, val hdPortrait: Bitmap? = null)

private val mint = Color(0xFFA3F6DF)
private val lilac = Color(0xFFCDC1FF)
private val ink = Color(0xFFF1F4F7)
private val secondaryInk = Color(0xFFB5BEC8)
private val panel = Color(0xFF101416)
private val outline = Color(0xFF434C51)
private val palette = listOf(0xFF285472, 0xFF534087, 0xFF205D54, 0xFF6F4527, 0xFF723C5D, 0xFF455064).map { Color(it) }
private val cudOptions = listOf("Prefiero no indicarlo", "Sí, tengo CUD", "No tengo CUD", "En trámite")
private val avatarOptions = listOf(
    "Aurora azul", "Aurora violeta", "Aurora turquesa", "Aurora ámbar", "Aurora rosa", "Aurora gris",
    "Avatar con monograma", "Avatar con figura"
)

@Composable
internal fun ProfileScreen(
    draft: ProfileDraft,
    avatar: ProfileAvatar,
    message: String,
    importing: Boolean,
    onDraft: (ProfileDraft) -> Unit,
    onBack: () -> Unit,
    onPhoto: () -> Unit,
    onAvatar: (Int) -> Unit,
    onEditIdentity: (() -> Unit)? = null,
    onBirthday: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var avatarDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }
    var cudMenu by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = mint, onPrimary = Color(0xFF00382E), secondary = lilac,
        background = Color.Black, onBackground = ink, surface = panel, onSurface = ink,
        onSurfaceVariant = secondaryInk, outline = outline, error = Color(0xFFFFB4AD)
    )) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    NeonHeader("Mi perfil", onBack)
                    ProfileHeader(draft, avatar)
                    ProfileFold("Foto y avatar") {
                        OutlinedButton(onClick = onPhoto, enabled = !importing,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (importing) "Importando foto…" else "Foto del celular")
                        }
                        TextButton(onClick = { avatarDialog = true }, enabled = !importing,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Personalizar avatar") }
                    if (onEditIdentity != null) {
                        OutlinedButton(onClick = onEditIdentity, enabled = !importing,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Editar identidad y avatar HD") }
                    }
                    }
                    ProfileSection {
                        SectionTitle("Datos personales")
                        Text("Todos los campos son opcionales.", color = secondaryInk)
                        ProfileField(draft.name, "Nombre", "profileName") { onDraft(draft.copy(name = it)) }
                        ProfileField(draft.nickname, "Apodo", "profileNickname") { onDraft(draft.copy(nickname = it)) }
                    }
                    ProfileFold("Más datos personales") {
                        Text("Fecha de nacimiento", style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(onClick = onBirthday, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(draft.birth.ifBlank { "Elegir fecha" })
                        }
                        ProfileField(draft.contact, "Contacto de confianza", "profileContact") { onDraft(draft.copy(contact = it)) }
                    }
                    ProfileFold("Accesibilidad y preferencias") {
                        Text("Certificado Único de Discapacidad (CUD)", style = MaterialTheme.typography.titleSmall)
                        Text("Declaración opcional. No hace falta adjuntar el certificado.", color = secondaryInk)
                        Box {
                            OutlinedButton(
                                onClick = { cudMenu = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("profileCud")
                            ) { Text(cudOptions[draft.cud]) }
                            DropdownMenu(expanded = cudMenu, onDismissRequest = { cudMenu = false }) {
                                cudOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        onDraft(draft.copy(cud = index))
                                        cudMenu = false
                                    })
                                }
                            }
                        }
                        ProfileField(draft.notes, "Preferencias o información", "profileNotes", false) {
                            onDraft(draft.copy(notes = it))
                        }
                    }
                    Button(onClick = onSave, enabled = !importing, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                        Text("Guardar mi perfil")
                    }
                    Text(message, color = mint, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    ProfileFold("Privacidad") {
                    Text("Tus datos y tu foto se guardan en este dispositivo. Si desinstalás AURORA, se pueden perder.",
                        color = secondaryInk, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { deleteDialog = true }, enabled = !importing) {
                        Text("Borrar datos del perfil", color = MaterialTheme.colorScheme.error)
                    }
                    }
                }
            }
        }
        if (avatarDialog) {
            AlertDialog(
                onDismissRequest = { avatarDialog = false },
                title = { Text("Personalizar avatar") },
                text = {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        avatarOptions.forEachIndexed { index, label ->
                            TextButton(onClick = { onAvatar(index); avatarDialog = false },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { avatarDialog = false }) { Text("Listo") } }
            )
        }
        if (deleteDialog) {
            AlertDialog(
                onDismissRequest = { deleteDialog = false },
                title = { Text("Borrar perfil") },
                text = { Text("Se borrarán tus datos y la foto. Se conservarán tus alarmas e historiales.") },
                dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancelar") } },
                confirmButton = {
                    TextButton(onClick = { deleteDialog = false; onDelete() }) {
                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    draft: ProfileDraft, avatar: ProfileAvatar
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF12352F), Color(0xFF17142C), panel)))
            .border(1.dp, Color(0xFF354C49), shape).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedContent(
            targetState = avatar,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "Retrato"
        ) { portrait ->
            Box(
                Modifier.size(100.dp).border(2.dp, Brush.sweepGradient(listOf(mint, lilac, mint)), CircleShape)
                    .padding(5.dp).clip(CircleShape).background(palette[portrait.color]),
                contentAlignment = Alignment.Center
            ) {
                val photo = portrait.photo
                if (photo != null) {
                    Image(remember(photo) { photo.asImageBitmap() }, null, Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else if (portrait.hdPortrait != null) {
                    val hd = portrait.hdPortrait
                    Image(remember(hd) { hd.asImageBitmap() }, null, Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit)
                } else if (portrait.figure) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(ink, radius = size.width * .15f, center = Offset(center.x, size.height * .37f))
                        drawRoundRect(ink, topLeft = Offset(size.width * .23f, size.height * .56f),
                            size = Size(size.width * .54f, size.height * .22f),
                            cornerRadius = CornerRadius(size.width * .12f))
                    }
                } else {
                    val identity = draft.nickname.ifBlank { draft.name }.trim()
                    val initials = remember(identity) {
                        val words = identity.split(Regex("\\s+")).filter(String::isNotEmpty)
                        if (words.isEmpty()) "A" else listOf(words.first(), words.last())
                            .take(if (words.size == 1) 1 else 2)
                            .joinToString("") { String(Character.toChars(it.codePointAt(0))) }.uppercase(Locale.ROOT)
                    }
                    Text(initials, color = ink, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(if (avatar.photo == null) "Avatar personalizado" else "Foto personal",
            color = secondaryInk, style = MaterialTheme.typography.labelMedium)

    }
}

@Composable
private fun ProfileFold(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                stateDescription = if (expanded) "Expandido" else "Contraído"
            }) { Text(title) }
        if (expanded) ProfileSection(content)
    }
}

@Composable
private fun ProfileSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = panel) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun ProfileField(value: String, label: String, tag: String, singleLine: Boolean = true, edit: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = edit, label = { Text(label) }, singleLine = singleLine,
        minLines = if (singleLine) 1 else 2, modifier = Modifier.fillMaxWidth().testTag(tag))
}
