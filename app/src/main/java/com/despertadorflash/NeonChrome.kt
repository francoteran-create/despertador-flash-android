package com.despertadorflash

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun NeonHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = "Volver a AURORA" }) { Text("‹", color = cyan, fontSize = 27.sp) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = cyan, fontFamily = graffiti, fontSize = 22.sp, modifier = Modifier.semantics { heading() })
        }
        Box(Modifier.widthIn(min = 48.dp), contentAlignment = Alignment.CenterEnd) { trailing?.invoke() }
    }
}

@Composable
internal fun NeonOverflow(label: String, actions: Map<String, String>, onAction: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = label }) { Text("•••", color = cyan, fontSize = 20.sp) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { (key, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = { open = false; onAction(key) })
            }
        }
    }
}

@Composable
internal fun NeonAvatar(portrait: Bitmap?, name: String, onProfile: () -> Unit) {
    val badge = Modifier.size(50.dp).border(1.dp, Brush.sweepGradient(listOf(cyan, magenta, yellow, cyan)), CircleShape)
    IconButton(onClick = onProfile, modifier = badge.semantics { contentDescription = "Abrir mi perfil" }) {
        if (portrait != null) {
            Image(remember(portrait) { portrait.asImageBitmap() }, "Avatar del perfil",
                Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
        } else {
            Text(name.trim().take(1).uppercase().ifEmpty { "A" }, color = cyan, fontFamily = graffiti)
        }
    }
}
