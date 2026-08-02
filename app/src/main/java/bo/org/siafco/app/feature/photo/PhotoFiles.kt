package bo.org.siafco.app.feature.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import bo.org.siafco.app.BuildConfig
import bo.org.siafco.app.domain.PreparedPhoto
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PhotoFiles {
    private const val MAX_TEMP_AGE_MS = 24L * 60L * 60L * 1000L

    fun createCapture(context: Context): CameraCaptureTarget {
        pruneAbandoned(context)
        val file = File(captureDir(context), "siafco-capture-${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        return CameraCaptureTarget(file = file, uri = uri)
    }

    fun createProcessed(context: Context): File {
        pruneAbandoned(context)
        return File(processedDir(context), "siafco-photo-${UUID.randomUUID()}.jpg")
    }

    fun copyExternalSource(context: Context, uri: Uri): SourcePhoto {
        pruneAbandoned(context)
        val tempFile = File(sourceDir(context), "siafco-source-${UUID.randomUUID()}.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: error("No se pudo leer la fotografia.")

            val format = PhotoFormatDetector.detect(tempFile)
                ?: error("No se pudo leer la fotografia.")
            val file = File(sourceDir(context), "siafco-source-${UUID.randomUUID()}.${format.extension}")
            check(tempFile.renameTo(file)) { "No se pudo preparar la fotografia." }
            return SourcePhoto(file = file, uri = Uri.fromFile(file), mimeType = format.mimeType)
        } catch (exception: Exception) {
            tempFile.delete()
            throw exception
        }
    }

    fun clear(photo: PreparedPhoto?) {
        photo?.file?.delete()
    }

    fun clear(file: File?) {
        file?.delete()
    }

    private fun captureDir(context: Context): File =
        File(context.cacheDir, "siafco-photo/capture").apply { mkdirs() }

    private fun sourceDir(context: Context): File =
        File(context.cacheDir, "siafco-photo/source").apply { mkdirs() }

    private fun processedDir(context: Context): File =
        File(context.cacheDir, "siafco-photo/processed").apply { mkdirs() }

    private fun pruneAbandoned(context: Context) {
        val root = File(context.cacheDir, "siafco-photo")
        val now = System.currentTimeMillis()
        root.walkTopDown()
            .filter { it.isFile && now - it.lastModified() > MAX_TEMP_AGE_MS }
            .forEach { it.delete() }
    }
}

data class CameraCaptureTarget(
    val file: File,
    val uri: Uri
)

data class SourcePhoto(
    val file: File,
    val uri: Uri,
    val mimeType: String
)
