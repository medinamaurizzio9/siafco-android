package bo.org.siafco.app

import bo.org.siafco.app.feature.photo.PhotoCropCalculator
import bo.org.siafco.app.feature.photo.PhotoCropTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCropCalculatorTest {
    @Test
    fun horizontalImageIsCroppedToCenteredSquare() {
        val crop = PhotoCropCalculator.centeredSquare(width = 1600, height = 900)

        assertEquals(350, crop.left)
        assertEquals(0, crop.top)
        assertEquals(900, crop.size)
    }

    @Test
    fun verticalImageIsCroppedToCenteredSquare() {
        val crop = PhotoCropCalculator.centeredSquare(width = 900, height = 1600)

        assertEquals(0, crop.left)
        assertEquals(350, crop.top)
        assertEquals(900, crop.size)
    }

    @Test
    fun transformedCropAlwaysStaysSquareAndInsideImage() {
        val crop = PhotoCropCalculator.transformedSquare(
            width = 1600,
            height = 1200,
            transform = PhotoCropTransform(
                viewportSizePx = 900,
                scale = 2.4f,
                offsetX = 180f,
                offsetY = -120f
            )
        )

        assertTrue(crop.size > 0)
        assertTrue(crop.left >= 0)
        assertTrue(crop.top >= 0)
        assertTrue(crop.left + crop.size <= 1600)
        assertTrue(crop.top + crop.size <= 1200)
    }

    @Test
    fun zoomedCropIsSmallerThanCenteredCrop() {
        val centered = PhotoCropCalculator.centeredSquare(width = 1600, height = 1200)
        val zoomed = PhotoCropCalculator.transformedSquare(
            width = 1600,
            height = 1200,
            transform = PhotoCropTransform(viewportSizePx = 900, scale = 2f, offsetX = 0f, offsetY = 0f)
        )

        assertTrue(zoomed.size < centered.size)
    }

    @Test
    fun cropIsSquareForLargeImageInputs() {
        val crop = PhotoCropCalculator.transformedSquare(
            width = 8000,
            height = 5000,
            transform = PhotoCropTransform(viewportSizePx = 1080, scale = 1.3f, offsetX = -250f, offsetY = 180f)
        )

        assertTrue(crop.size <= 5000)
        assertTrue(crop.left + crop.size <= 8000)
        assertTrue(crop.top + crop.size <= 5000)
    }

    @Test
    fun invalidViewportFallsBackToCenteredSquare() {
        val crop = PhotoCropCalculator.transformedSquare(
            width = 800,
            height = 600,
            transform = PhotoCropTransform(viewportSizePx = 0, scale = 1f, offsetX = 0f, offsetY = 0f)
        )

        assertEquals(PhotoCropCalculator.centeredSquare(800, 600), crop)
    }
}
