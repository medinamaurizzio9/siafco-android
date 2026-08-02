package bo.org.siafco.app.feature.photo

import kotlin.math.roundToInt

data class PhotoCropTransform(
    val viewportSizePx: Int,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

data class SourceCropRect(
    val left: Int,
    val top: Int,
    val size: Int
)

object PhotoCropCalculator {
    fun centeredSquare(width: Int, height: Int): SourceCropRect {
        val size = minOf(width, height)
        return SourceCropRect(
            left = ((width - size) / 2f).roundToInt(),
            top = ((height - size) / 2f).roundToInt(),
            size = size
        )
    }

    fun transformedSquare(width: Int, height: Int, transform: PhotoCropTransform): SourceCropRect {
        if (width <= 0 || height <= 0 || transform.viewportSizePx <= 0) return centeredSquare(width, height)

        val viewport = transform.viewportSizePx.toFloat()
        val fitScale = minOf(viewport / width, viewport / height)
        val renderScale = (fitScale * transform.scale.coerceAtLeast(1f)).takeIf { it > 0f }
            ?: return centeredSquare(width, height)

        val renderedWidth = width * renderScale
        val renderedHeight = height * renderScale
        val imageLeft = (viewport - renderedWidth) / 2f + transform.offsetX
        val imageTop = (viewport - renderedHeight) / 2f + transform.offsetY
        val centerX = ((viewport / 2f - imageLeft) / renderScale).coerceIn(0f, width.toFloat())
        val centerY = ((viewport / 2f - imageTop) / renderScale).coerceIn(0f, height.toFloat())
        val requestedSize = (viewport / renderScale).roundToInt().coerceAtLeast(1)
        val size = minOf(requestedSize, width, height)
        val left = (centerX - size / 2f).roundToInt().coerceIn(0, width - size)
        val top = (centerY - size / 2f).roundToInt().coerceIn(0, height - size)

        return SourceCropRect(left = left, top = top, size = size)
    }
}

object PhotoInputPolicy {
    fun nextPhaseAfterExternalResult(uri: String?): PhotoInputPhase =
        if (uri.isNullOrBlank()) PhotoInputPhase.Cancelled else PhotoInputPhase.Cropping
}

enum class PhotoInputPhase {
    Source,
    WaitingExternal,
    Cropping,
    Cancelled
}
