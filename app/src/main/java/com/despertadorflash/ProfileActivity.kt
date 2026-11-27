package com.despertadorflash

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.AtomicFile
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class ProfileActivity : ComponentActivity() {
    private val preferences by lazy { getSharedPreferences("aurora_profile", MODE_PRIVATE) }
    private val photoFile by lazy { AtomicFile(File(filesDir, "aurora_profile_photo.jpg")) }
    private var draft by mutableStateOf(ProfileDraft())
    private var avatar by mutableStateOf(ProfileAvatar())
    private var message by mutableStateOf("")
    private var importing by mutableStateOf(false)
    private val certificate by lazy { PrivateCertificate(this) }
    private var hasCertificate by mutableStateOf(false)
    private var certificatePreview by mutableStateOf<Bitmap?>(null)
    private var certificateBusy by mutableStateOf(false)
    private val pickCertificate = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !certificateBusy) {
            certificateBusy = true
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { certificate.import(uri) }
                    hasCertificate = true
                    message = "Certificado adjuntado en este dispositivo"
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    message = "No se pudo adjuntar el certificado. Elegí una imagen o PDF válido de hasta 12 MB."
                } finally {
                    certificateBusy = false
                }
            }
        }
    }

    private fun viewCertificate() {
        lifecycleScope.launch {
            try {
                certificatePreview = withContext(Dispatchers.IO) { certificate.preview() }
                if (certificatePreview == null) message = "No se pudo abrir el certificado"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "No se pudo abrir el certificado"
            }
        }
    }
    private val editIdentity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            draft = draft.copy(name = preferences.getString("name", "").orEmpty())
            avatar = avatar.copy(hdPortrait = NeonIdentity.menuPortrait(this, 640))
            message = "Identidad actualizada"
        }
    }
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importing = true
            lifecycleScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) { importPhoto(uri) }
                    preferences.edit().putBoolean("photo", true).apply()
                    avatar = avatar.copy(photo = bitmap)
                    message = "Foto actualizada"
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = "No se pudo importar la foto. Elegí otra imagen."
                } finally {
                    importing = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasCertificate = certificate.exists()
        draft = ProfileDraft(
            name = savedInstanceState?.getString("name") ?: preferences.getString("name", "").orEmpty(),
            nickname = savedInstanceState?.getString("nickname") ?: preferences.getString("nickname", "").orEmpty(),
            birth = savedInstanceState?.getString("birth") ?: preferences.getString("birth", "").orEmpty(),
            contact = savedInstanceState?.getString("contact") ?: preferences.getString("contact", "").orEmpty(),
            notes = savedInstanceState?.getString("notes") ?: preferences.getString("notes", "").orEmpty(),
            cud = (savedInstanceState?.getInt("cud") ?: preferences.getInt("cud", 0)).coerceIn(0, 3)
        )
        avatar = ProfileAvatar(
            color = preferences.getInt("avatar_color", 0).coerceIn(0, 5),
            figure = preferences.getInt("avatar_style", 0) != 0,
            hdPortrait = if (preferences.getBoolean("neon_legacy_portrait", false)) null else NeonIdentity.menuPortrait(this, 640),
            photo = if (preferences.getBoolean("photo", false)) {
                runCatching { photoFile.openRead().use { BitmapFactory.decodeStream(it) } }.getOrNull()
            } else null
        )
        setContent {
            ProfileScreen(
                draft = draft,
                avatar = avatar,
                message = message,
                importing = importing,
                onDraft = { draft = it },
                onBack = ::finish,
                onPhoto = {
                    try {
                        pickPhoto.launch(arrayOf("image/*"))
                    } catch (_: android.content.ActivityNotFoundException) {
                        message = "No se pudo abrir la galería."
                    }
                },
                onAvatar = ::selectAvatar,
                onEditIdentity = if (NeonIdentity.catalog(this) == null) null else ({
                    editIdentity.launch(Intent(this, NeonWelcomeActivity::class.java).putExtra("edit_identity", true))
                }),
                onBirthday = ::showBirthdayPicker,
                hasCertificate = hasCertificate,
                certificatePreview = certificatePreview,
                certificateBusy = certificateBusy,
                onAddCertificate = { pickCertificate.launch(arrayOf("image/*", "application/pdf")) },
                onViewCertificate = ::viewCertificate,
                onCloseCertificate = { certificatePreview = null },
                onRemoveCertificate = {
                    certificate.clear()
                    hasCertificate = certificate.exists()
                    certificatePreview = null
                    message = if (hasCertificate) "No se pudo eliminar el certificado" else "Certificado eliminado"
                },
                onSave = ::save,
                onDelete = ::delete
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", draft.name)
        outState.putString("nickname", draft.nickname)
        outState.putString("birth", draft.birth)
        outState.putString("contact", draft.contact)
        outState.putString("notes", draft.notes)
        outState.putInt("cud", draft.cud)
    }

    private fun importPhoto(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Imagen no compatible")
        var scale = 1
        while (bounds.outWidth / scale > 1000 || bounds.outHeight / scale > 1000) scale *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Imagen no compatible")
        val output = photoFile.startWrite()
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) throw IOException("Error al convertir la foto")
            photoFile.finishWrite(output)
        } catch (failure: Exception) {
            photoFile.failWrite(output)
            bitmap.recycle()
            throw failure
        }
        return bitmap
    }

    private fun selectAvatar(option: Int) {
        avatar = if (option < 6) avatar.copy(color = option, photo = null, hdPortrait = null)
                 else avatar.copy(figure = option == 7, photo = null, hdPortrait = null)
        preferences.edit().putInt("avatar_color", avatar.color)
            .putInt("avatar_style", if (avatar.figure) 1 else 0)
            .putBoolean("photo", false).putBoolean("neon_legacy_portrait", true).apply()
        message = "Avatar actualizado"
    }

    private fun showBirthdayPicker() {
        val date = Calendar.getInstance()
        val parts = draft.birth.split("/")
        if (parts.size == 3 && parts.all { it.toIntOrNull() != null }) {
            date.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
        }
        DatePickerDialog(this, { _, year, month, day ->
            draft = draft.copy(birth = String.format(Locale.ROOT, "%02d/%02d/%04d", day, month + 1, year))
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun save() {
        if (draft.name.trim().length > 100 || draft.nickname.trim().length > 100 ||
            draft.contact.length > 160 || draft.notes.length > 500
        ) {
            message = "Acortá los textos del perfil para poder guardarlos."
            return
        }
        preferences.edit().putString("name", draft.name.trim()).putString("nickname", draft.nickname.trim())
            .putString("birth", draft.birth).putString("contact", draft.contact.trim())
            .putString("notes", draft.notes.trim()).putInt("cud", draft.cud)
            .putBoolean("photo", avatar.photo != null).apply()
        message = "Perfil guardado en este teléfono"
    }

    private fun delete() {
        photoFile.delete()
        certificate.clear()
        hasCertificate = certificate.exists()
        certificatePreview = null
        val completed = preferences.getBoolean("neon_welcome_complete", false)
        preferences.edit().clear().apply {
            if (completed) putBoolean("neon_welcome_complete", true)
        }.apply()
        draft = ProfileDraft()
        avatar = ProfileAvatar()
        message = "Perfil borrado"
    }
}
