package bo.org.siafco.app.feature.register

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import bo.org.siafco.app.domain.PreparedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object PhotoPreparer {
    suspend fun prepare(context: Context, uri: Uri): Result<PreparedPhoto> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri).orEmpty()
            require(mime.startsWith("image/")) { "El archivo seleccionado no es una imagen." }

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("No se pudo leer la fotografia.")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("El archivo seleccionado no es una imagen valida.")
            val rotated = rotateIfNeeded(bytes, bitmap)
            val resized = resize(rotated, 1280)
            if (rotated !== bitmap) bitmap.recycle()
            if (resized !== rotated) rotated.recycle()

            val dir = File(context.cacheDir, "affiliation-photos").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val output = File(dir, "photo-${System.currentTimeMillis()}.jpg")
            FileOutputStream(output).use { stream ->
                check(resized.compress(Bitmap.CompressFormat.JPEG, 82, stream)) {
                    "No se pudo comprimir la fotografia."
                }
            }
            resized.recycle()
            PreparedPhoto(file = output, displayName = "Fotografia seleccionada")
        }
    }

    fun clear(photo: PreparedPhoto?) {
        photo?.file?.delete()
    }

    private fun resize(bitmap: Bitmap, maxSide: Int): Bitmap {
        val currentMax = maxOf(bitmap.width, bitmap.height)
        if (currentMax <= maxSide) return bitmap
        val scale = maxSide.toFloat() / currentMax
        val width = (bitmap.width * scale).roundToInt()
        val height = (bitmap.height * scale).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun rotateIfNeeded(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(bytes.inputStream()).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
