package bo.org.siafco.app.feature.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import bo.org.siafco.app.domain.PreparedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoProcessor {
    private const val TARGET_SIZE = 800
    private const val MIN_INPUT_SIZE = 300
    private const val MIN_OUTPUT_SIZE = 600
    private const val MAX_BYTES = 500 * 1024
    private val QUALITIES = intArrayOf(88, 82, 76, 72)

    suspend fun prepare(context: Context, uri: Uri, crop: PhotoCropTransform? = null): Result<PreparedPhoto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceFile = fileFromUri(uri)
                require(detectFormat(context, uri, sourceFile) != null) { "No se pudo leer la fotografia." }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val boundsOpened = openInput(context, uri, sourceFile)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                    true
                } ?: false
                require(boundsOpened && bounds.outWidth > 0 && bounds.outHeight > 0) { "No se pudo leer la fotografia." }
                require(bounds.outWidth >= MIN_INPUT_SIZE && bounds.outHeight >= MIN_INPUT_SIZE) {
                    "La fotografia debe tener al menos 300 x 300 pixeles."
                }

                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = openInput(context, uri, sourceFile)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: error("No se pudo procesar la imagen seleccionada.")
                require(decoded.width > 0 && decoded.height > 0) { "No se pudo procesar la imagen seleccionada." }

                val oriented = rotateIfNeeded(context, uri, sourceFile, decoded)
                if (oriented !== decoded) decoded.recycle()

                val cropRect = crop?.let {
                    PhotoCropCalculator.transformedSquare(oriented.width, oriented.height, it)
                } ?: PhotoCropCalculator.centeredSquare(oriented.width, oriented.height)
                require(isValidCrop(cropRect, oriented.width, oriented.height)) { "El recorte no es valido." }

                val cropped = Bitmap.createBitmap(oriented, cropRect.left, cropRect.top, cropRect.size, cropRect.size)
                if (cropped !== oriented) oriented.recycle()

                val output = compressAdaptive(cropped)
                cropped.recycle()

                val file = PhotoFiles.createProcessed(context)
                FileOutputStream(file).use { it.write(output.bytes) }
                verifyOutput(file)

                PreparedPhoto(
                    file = file,
                    displayName = "Fotografia optimizada",
                    width = output.size,
                    height = output.size,
                    sizeBytes = file.length(),
                    mimeType = "image/jpeg"
                )
            }
        }

    private fun detectFormat(context: Context, uri: Uri, sourceFile: File?): PhotoFormat? =
        openInput(context, uri, sourceFile)?.use { PhotoFormatDetector.detect(it) }

    private fun isValidCrop(crop: SourceCropRect, width: Int, height: Int): Boolean =
        crop.size > 0 &&
            crop.left >= 0 &&
            crop.top >= 0 &&
            crop.left + crop.size <= width &&
            crop.top + crop.size <= height

    private fun verifyOutput(file: File) {
        require(file.exists() && file.canRead() && file.length() > 0) {
            "No se pudo procesar la imagen seleccionada."
        }
        require(file.length() <= MAX_BYTES) {
            "No se pudo procesar la imagen seleccionada."
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        file.inputStream().use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth in MIN_OUTPUT_SIZE..TARGET_SIZE && options.outHeight in MIN_OUTPUT_SIZE..TARGET_SIZE) {
            "No se pudo procesar la imagen seleccionada."
        }
    }

    private fun compressAdaptive(bitmap: Bitmap): EncodedPhoto {
        var size = TARGET_SIZE
        while (size >= MIN_OUTPUT_SIZE) {
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val jpegReady = scaled.withWhiteBackground()
            if (jpegReady !== scaled) scaled.recycle()

            for (quality in QUALITIES) {
                val bytes = jpegReady.toJpeg(quality)
                if (bytes.size <= MAX_BYTES) {
                    jpegReady.recycle()
                    return EncodedPhoto(bytes = bytes, size = size)
                }
            }
            jpegReady.recycle()
            size = (size * 0.9f).roundToInt().coerceAtLeast(MIN_OUTPUT_SIZE).let {
                if (it == size) size - 1 else it
            }
        }

        val fallback = Bitmap.createScaledBitmap(bitmap, MIN_OUTPUT_SIZE, MIN_OUTPUT_SIZE, true).withWhiteBackground()
        val bytes = fallback.toJpeg(QUALITIES.last())
        fallback.recycle()
        if (bytes.size > MAX_BYTES) {
            error("No se pudo procesar la imagen seleccionada.")
        }
        return EncodedPhoto(bytes = bytes, size = MIN_OUTPUT_SIZE)
    }

    private fun Bitmap.withWhiteBackground(): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(this@withWhiteBackground, 0f, 0f, null)
        }
        return output
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            check(compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                "No se pudo comprimir la fotografia."
            }
            stream.toByteArray()
        }

    private fun calculateSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        var currentMax = max(width, height)
        while (currentMax / 2 >= maxSide) {
            sample *= 2
            currentMax /= 2
        }
        return sample
    }

    private fun rotateIfNeeded(context: Context, uri: Uri, sourceFile: File?, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            openInput(context, uri, sourceFile)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

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

    private fun fileFromUri(uri: Uri): File? =
        if (uri.scheme == "file") {
            uri.path?.let(::File)
        } else {
            null
        }

    private fun openInput(context: Context, uri: Uri, sourceFile: File?): InputStream? =
        sourceFile?.inputStream() ?: context.contentResolver.openInputStream(uri)

    private data class EncodedPhoto(
        val bytes: ByteArray,
        val size: Int
    )
}
