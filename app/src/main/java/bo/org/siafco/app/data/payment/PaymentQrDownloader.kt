package bo.org.siafco.app.data.payment

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object PaymentQrDownloader {
    const val FILE_NAME = "SIAFCO-QR-PAGO.png"
    private const val MAX_QR_BYTES = 2 * 1024 * 1024

    suspend fun save(context: Context, qrUrl: String?): PaymentQrDownloadResult = withContext(Dispatchers.IO) {
        val uri = qrUrl?.takeIf(String::isNotBlank)?.let(Uri::parse)
            ?: return@withContext PaymentQrDownloadResult.InvalidUrl
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") {
            return@withContext PaymentQrDownloadResult.InvalidUrl
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, uri)
        } else {
            enqueueWithDownloadManager(context, uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(context: Context, uri: Uri): PaymentQrDownloadResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return PaymentQrDownloadResult.Failed

        return runCatching {
            val bytes = downloadBytes(uri)
            resolver.openOutputStream(destination)?.use { output ->
                output.write(bytes)
            } ?: error("No output stream")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(destination, values, null, null)
            PaymentQrDownloadResult.Saved(FILE_NAME)
        }.getOrElse {
            resolver.delete(destination, null, null)
            PaymentQrDownloadResult.Failed
        }
    }

    private fun enqueueWithDownloadManager(context: Context, uri: Uri): PaymentQrDownloadResult {
        val downloadManager = context.getSystemService<DownloadManager>()
            ?: return PaymentQrDownloadResult.Failed

        return runCatching {
            val request = DownloadManager.Request(uri)
                .setTitle("QR de pago SIAFCO")
                .setDescription("QR institucional para pago de afiliacion")
                .setMimeType("image/png")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
            downloadManager.enqueue(request)
            PaymentQrDownloadResult.Saved(FILE_NAME)
        }.getOrElse {
            PaymentQrDownloadResult.Failed
        }
    }

    private fun downloadBytes(uri: Uri): ByteArray {
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
        }
        return connection.use {
            if (it.responseCode !in 200..299) error("HTTP ${it.responseCode}")
            val bytes = it.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > MAX_QR_BYTES) error("Invalid QR size")
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            if (bytes.isEmpty() || bytes.size > MAX_QR_BYTES) error("Invalid QR size")
            bytes
        }
    }
}

private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T {
    return try {
        block(this)
    } finally {
        disconnect()
    }
}

sealed interface PaymentQrDownloadResult {
    data class Saved(val fileName: String) : PaymentQrDownloadResult
    data object InvalidUrl : PaymentQrDownloadResult
    data object Failed : PaymentQrDownloadResult
}
