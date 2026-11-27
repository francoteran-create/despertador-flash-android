package com.despertadorflash

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class PrivateCertificateTest {
    @Test fun privateCopyAcceptsImageAndPdfAndCanBeRemoved() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val staging = Files.createTempDirectory(target.cacheDir.toPath(), "cud-test").toFile()
        val sandbox = object : ContextWrapper(target) {
            override fun getFilesDir() = File(staging, "private").apply { mkdirs() }
            override fun getCacheDir() = staging
        }
        try {
            val storage = PrivateCertificate(sandbox)
            val image = File(staging, "source.png")
            val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.CYAN)
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            storage.import(Uri.fromFile(image))
            image.delete()
            assertTrue(storage.exists())
            assertEquals(32, storage.preview()?.width)
            val pdf = File(staging, "source.pdf")
            val document = PdfDocument()
            try {
                val page = document.startPage(PdfDocument.PageInfo.Builder(200, 300, 1).create())
                page.canvas.drawColor(Color.WHITE)
                document.finishPage(page)
                pdf.outputStream().use { document.writeTo(it) }
            } finally {
                document.close()
            }
            storage.import(Uri.fromFile(pdf))
            assertNotNull(storage.preview())
            val invalid = File(staging, "invalid").apply { writeText("no es un certificado") }
            assertTrue(runCatching { storage.import(Uri.fromFile(invalid)) }.isFailure)
            assertNotNull(storage.preview())
            storage.clear()
            assertFalse(storage.exists())
            assertNull(storage.preview())
        } finally {
            staging.deleteRecursively()
        }
    }
}
