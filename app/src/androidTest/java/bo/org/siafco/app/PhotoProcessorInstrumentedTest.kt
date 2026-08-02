package bo.org.siafco.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import bo.org.siafco.app.feature.photo.PhotoCropTransform
import bo.org.siafco.app.feature.photo.PhotoFormat
import bo.org.siafco.app.feature.photo.PhotoFormatDetector
import bo.org.siafco.app.feature.photo.PhotoProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class PhotoProcessorInstrumentedTest {
    @Test
    fun processesPrivateFileUriWithoutMimeOrExtensionIntoReadableJpeg() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "siafco-photo-test-source").apply {
            parentFile?.mkdirs()
            writeBitmap(format = Bitmap.CompressFormat.PNG)
        }
        assertTrue("source length=${source.length()}", source.length() > 0)
        assertEquals(PhotoFormat.Png, PhotoFormatDetector.detect(source))
        val sourceBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        source.inputStream().use { BitmapFactory.decodeStream(it, null, sourceBounds) }
        assertEquals(900, sourceBounds.outWidth)
        assertEquals(1200, sourceBounds.outHeight)

        val result = PhotoProcessor.prepare(
            context = context,
            uri = Uri.fromFile(source),
            crop = PhotoCropTransform(viewportSizePx = 800, scale = 1f, offsetX = 0f, offsetY = 0f)
        )

            assertTrue(result.exceptionOrNull()?.javaClass?.simpleName + ": " + result.exceptionOrNull()?.message, result.isSuccess)
        val photo = result.getOrThrow()
        assertTrue(photo.file.exists())
        assertTrue(photo.file.canRead())
        assertTrue(photo.file.length() in 1..(500 * 1024))

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        photo.file.inputStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        assertEquals(photo.width, bounds.outWidth)
        assertEquals(photo.height, bounds.outHeight)
        assertTrue(bounds.outWidth in 600..800)
        assertTrue(bounds.outHeight in 600..800)

            source.delete()
            photo.file.delete()
        }
    }

    @Test
    fun rejectsInvalidMagicBytesBeforeDeliveringToProfile() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "siafco-photo-test-invalid").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        }

        val result = PhotoProcessor.prepare(context, Uri.fromFile(source))

            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
            source.delete()
        }
    }

    private fun File.writeBitmap(format: Bitmap.CompressFormat) {
        val bitmap = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
        FileOutputStream(this).use { output ->
            check(bitmap.compress(format, 100, output))
        }
        bitmap.recycle()
    }
}
