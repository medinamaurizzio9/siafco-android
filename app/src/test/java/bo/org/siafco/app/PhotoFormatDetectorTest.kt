package bo.org.siafco.app

import bo.org.siafco.app.feature.photo.PhotoFormat
import bo.org.siafco.app.feature.photo.PhotoFormatDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class PhotoFormatDetectorTest {
    @Test
    fun detectsJpegPngAndWebpFromMagicBytes() {
        assertEquals(PhotoFormat.Jpeg, PhotoFormatDetector.detect(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)))
        assertEquals(
            PhotoFormat.Png,
            PhotoFormatDetector.detect(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
        )
        assertEquals(
            PhotoFormat.Webp,
            PhotoFormatDetector.detect(
                byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)
            )
        )
    }

    @Test
    fun rejectsInvalidMagicBytesEvenWhenFileCouldHaveAnyExtension() {
        assertNull(PhotoFormatDetector.detect(byteArrayOf(0x25, 0x50, 0x44, 0x46)))
        assertNull(PhotoFormatDetector.detect(ByteArray(0)))
    }

    @Test
    fun detectsFormatFromInputStreamWithoutReadNBytesDependency() {
        val input = ByteArrayInputStream(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00))

        assertEquals(PhotoFormat.Jpeg, PhotoFormatDetector.detect(input))
    }
}
