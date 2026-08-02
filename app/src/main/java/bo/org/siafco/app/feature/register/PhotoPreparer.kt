package bo.org.siafco.app.feature.register

import android.content.Context
import android.net.Uri
import bo.org.siafco.app.domain.PreparedPhoto
import bo.org.siafco.app.feature.photo.PhotoCropTransform
import bo.org.siafco.app.feature.photo.PhotoFiles
import bo.org.siafco.app.feature.photo.PhotoProcessor

object PhotoPreparer {
    suspend fun prepare(context: Context, uri: Uri, crop: PhotoCropTransform? = null): Result<PreparedPhoto> =
        PhotoProcessor.prepare(context, uri, crop)

    fun clear(photo: PreparedPhoto?) {
        PhotoFiles.clear(photo)
    }
}
