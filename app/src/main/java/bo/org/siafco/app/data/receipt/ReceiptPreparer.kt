package bo.org.siafco.app.data.receipt

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.domain.sha256
import java.io.File
import java.util.UUID

class ReceiptPreparer(private val context: Context) {
    fun prepare(uri: Uri): PreparedReceipt? {
        val resolver = context.contentResolver
        val originalName = resolver.displayName(uri) ?: "comprobante"
        val extension = originalName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { it in allowedExtensions }
            ?: return null
        val target = File(context.cacheDir, "payment-receipts/${UUID.randomUUID()}.$extension")
        target.parentFile?.mkdirs()
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        val mimeType = detectMime(target) ?: run {
            target.delete()
            return null
        }
        if (mimeType !in allowedMimeTypes) {
            target.delete()
            return null
        }

        return PreparedReceipt(
            file = target,
            displayName = originalName.take(80),
            mimeType = mimeType,
            sizeBytes = target.length(),
            sha256 = target.sha256(),
            canPreviewImage = mimeType.startsWith("image/")
        )
    }

    private fun detectMime(file: File): String? {
        val bytes = file.inputStream().use { input ->
            ByteArray(16).also { input.read(it) }
        }
        return when {
            bytes.size >= 4 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte() -> "image/jpeg"
            bytes.take(8) == listOf(
                0x89.toByte(),
                0x50.toByte(),
                0x4E.toByte(),
                0x47.toByte(),
                0x0D.toByte(),
                0x0A.toByte(),
                0x1A.toByte(),
                0x0A.toByte()
            ) -> "image/png"
            bytes.take(4).toByteArray().toString(Charsets.US_ASCII) == "RIFF" &&
                bytes.drop(8).take(4).toByteArray().toString(Charsets.US_ASCII) == "WEBP" -> "image/webp"
            bytes.take(5).toByteArray().toString(Charsets.US_ASCII) == "%PDF-" -> "application/pdf"
            else -> null
        }
    }

    private fun android.content.ContentResolver.displayName(uri: Uri): String? {
        return query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }
    }

    private companion object {
        private val allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "pdf")
        private val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "application/pdf")
    }
}
