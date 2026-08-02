package bo.org.siafco.app.feature.photo

import java.io.File
import java.io.InputStream

enum class PhotoFormat(val extension: String, val mimeType: String) {
    Jpeg("jpg", "image/jpeg"),
    Png("png", "image/png"),
    Webp("webp", "image/webp")
}

object PhotoFormatDetector {
    fun detect(bytes: ByteArray): PhotoFormat? = when {
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte() -> PhotoFormat.Jpeg
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte() -> PhotoFormat.Png
        bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() &&
            bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() &&
            bytes[11] == 0x50.toByte() -> PhotoFormat.Webp
        else -> null
    }

    fun detect(file: File): PhotoFormat? =
        file.inputStream().use { input ->
            detect(input)
        }

    fun detect(input: InputStream): PhotoFormat? {
        val header = ByteArray(16)
        var offset = 0
        while (offset < header.size) {
            val read = input.read(header, offset, header.size - offset)
            if (read <= 0) break
            offset += read
        }
        return detect(header.copyOf(offset))
    }
}
