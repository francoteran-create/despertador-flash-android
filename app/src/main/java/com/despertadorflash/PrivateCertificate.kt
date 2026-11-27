package com.despertadorflash

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.AtomicFile
import java.io.File
import java.io.IOException

/** Private, bounded copy. The external URI and filename are never retained. */
internal class PrivateCertificate(private val context: Context) {
    private val file = AtomicFile(File(context.filesDir, "aurora_cud_attachment"))
    fun exists() = file.baseFile.isFile
    fun clear() = file.delete()

    fun import(uri: Uri) {
        val staging = File.createTempFile("cud-", ".pending", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { source ->
                staging.outputStream().use { target ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > 12 * 1024 * 1024) throw IOException("El archivo supera 12 MB")
                        target.write(buffer, 0, read)
                    }
                }
            } ?: throw IOException("No se pudo leer el archivo")
            if (isPdf(staging)) {
                openPdf(staging) { require(it.pageCount > 0) }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(staging.path, bounds)
                if (bounds.outWidth !in 1..16384 || bounds.outHeight !in 1..16384)
                    throw IOException("Elegí una imagen válida o un PDF legible")
            }
            val output = file.startWrite()
            try {
                staging.inputStream().use { it.copyTo(output) }
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
        } finally {
            staging.delete()
        }
    }

    fun preview(): Bitmap? {
        val stored = file.baseFile
        if (!stored.isFile) return null
        if (isPdf(stored)) return openPdf(stored) { document ->
            document.openPage(0).use { page ->
                val ratio = minOf(1.5f, 1050f / maxOf(page.width, page.height))
                val result = Bitmap.createBitmap((page.width * ratio).toInt().coerceAtLeast(1),
                    (page.height * ratio).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                result.eraseColor(android.graphics.Color.WHITE)
                page.render(result, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                result
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(stored.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1300) sample *= 2
        return BitmapFactory.decodeFile(stored.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun isPdf(file: File): Boolean = file.inputStream().use {
        val signature = ByteArray(5)
        it.read(signature) == signature.size && String(signature, Charsets.US_ASCII) == "%PDF-"
    }

    private fun <T> openPdf(file: File, read: (PdfRenderer) -> T): T {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val document = try { PdfRenderer(descriptor) } catch (error: Exception) {
            descriptor.close()
            throw error
        }
        return document.use(read)
    }
}
