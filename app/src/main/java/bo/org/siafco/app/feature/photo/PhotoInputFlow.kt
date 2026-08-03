package bo.org.siafco.app.feature.photo

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.domain.PreparedPhoto
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PhotoInputFlow(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPrepared: (PreparedPhoto) -> Unit,
    onError: (String) -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cropUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var captureFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var captureUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var sourceFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var waitingExternalResult by rememberSaveable { mutableStateOf(false) }
    var processing by rememberSaveable { mutableStateOf(false) }
    val cropUri = cropUriText?.let(Uri::parse)
    val sourceFile = sourceFilePath?.let(::File)
    val captureTarget = remember(captureFilePath, captureUriText) {
        val filePath = captureFilePath
        val uriText = captureUriText
        if (filePath != null && uriText != null) {
            CameraCaptureTarget(File(filePath), Uri.parse(uriText))
        } else {
            null
        }
    }

    fun cancelFlow() {
        captureTarget?.let { context.revokePhotoUri(it.uri) }
        PhotoFiles.clear(captureTarget?.file)
        PhotoFiles.clear(sourceFile)
        captureFilePath = null
        captureUriText = null
        sourceFilePath = null
        cropUriText = null
        waitingExternalResult = false
        processing = false
        onDismiss()
    }

    BackHandler(enabled = visible && !processing) {
        cancelFlow()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        when (PhotoInputPolicy.nextPhaseAfterExternalResult(uri?.toString())) {
            PhotoInputPhase.Cropping -> {
                scope.launch {
                    runCatching { PhotoFiles.copyExternalSource(context, requireNotNull(uri)) }
                        .onSuccess { source ->
                            PhotoFiles.clear(sourceFile)
                            sourceFilePath = source.file.absolutePath
                            cropUriText = source.uri.toString()
                            waitingExternalResult = false
                        }
                        .onFailure {
                            waitingExternalResult = false
                            PhotoFiles.clear(sourceFile)
                            sourceFilePath = null
                            cropUriText = null
                            onError(it.message ?: "No se pudo leer la fotografía.")
                            onDismiss()
                        }
                }
            }
            PhotoInputPhase.Cancelled -> cancelFlow()
            PhotoInputPhase.Source, PhotoInputPhase.WaitingExternal -> Unit
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        waitingExternalResult = false
        when (PhotoInputPolicy.nextPhaseAfterExternalResult(captureUriText.takeIf { success })) {
            PhotoInputPhase.Cropping -> cropUriText = captureUriText
            PhotoInputPhase.Cancelled -> cancelFlow()
            PhotoInputPhase.Source, PhotoInputPhase.WaitingExternal -> Unit
        }
    }

    when {
        cropUri != null -> {
            PhotoCropDialog(
                uri = cropUri,
                processing = processing,
                onCancel = ::cancelFlow,
                onConfirm = { transform ->
                    if (processing) return@PhotoCropDialog
                    processing = true
                    scope.launch {
                        PhotoProcessor.prepare(context, cropUri, transform)
                            .onSuccess {
                                captureTarget?.let { context.revokePhotoUri(it.uri) }
                                PhotoFiles.clear(captureTarget?.file)
                                PhotoFiles.clear(sourceFile)
                                captureFilePath = null
                                captureUriText = null
                                sourceFilePath = null
                                cropUriText = null
                                waitingExternalResult = false
                                processing = false
                                onPrepared(it)
                                onDismiss()
                            }
                            .onFailure {
                                captureTarget?.let { target -> context.revokePhotoUri(target.uri) }
                                PhotoFiles.clear(captureTarget?.file)
                                PhotoFiles.clear(sourceFile)
                                captureFilePath = null
                                captureUriText = null
                                sourceFilePath = null
                                cropUriText = null
                                waitingExternalResult = false
                                processing = false
                                onError(it.message ?: "No se pudo procesar la imagen seleccionada.")
                                onDismiss()
                            }
                    }
                }
            )
        }
        !waitingExternalResult -> {
        PhotoSourceDialog(
            onCamera = {
                val target = PhotoFiles.createCapture(context)
                captureFilePath = target.file.absolutePath
                captureUriText = target.uri.toString()
                waitingExternalResult = true
                cameraLauncher.launch(target.uri)
            },
            onGallery = {
                waitingExternalResult = true
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCancel = ::cancelFlow
        )
        }
    }
}

private fun android.content.Context.revokePhotoUri(uri: Uri) {
    revokeUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
}

@Composable
private fun PhotoSourceDialog(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Seleccionar fotografía") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Centra tu rostro y procura tener buena iluminación.")
                Button(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                    Text("Tomar fotografía")
                }
                OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                    Text("Elegir de galería")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PhotoCropDialog(
    uri: Uri,
    processing: Boolean,
    onCancel: () -> Unit,
    onConfirm: (PhotoCropTransform) -> Unit
) {
    var scale by rememberSaveable(uri.toString()) { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable(uri.toString()) { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable(uri.toString()) { mutableFloatStateOf(0f) }
    val offset = Offset(offsetX, offsetY)
    var viewportSize by remember(uri) { mutableIntStateOf(0) }
    @Suppress("DEPRECATION")
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    AlertDialog(
        onDismissRequest = { if (!processing) onCancel() },
        title = { Text("Ajustar fotografía") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.Black)
                        .onSizeChanged { viewportSize = minOf(it.width, it.height) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Fotografía para recortar",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(transformState),
                        contentScale = ContentScale.Fit
                    )
                    ThirdsGrid()
                }
                Text(
                    "Mueve la imagen y ajusta el zoom. El recorte final será cuadrado 1:1.",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = scale,
                    onValueChange = { scale = it.coerceIn(1f, 5f) },
                    valueRange = 1f..5f
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !processing && viewportSize > 0,
                onClick = {
                    onConfirm(PhotoCropTransform(viewportSize, scale, offset.x, offset.y))
                }
            ) {
                Text(if (processing) "Procesando..." else "Confirmar recorte")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !processing, onClick = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                }) {
                    Text("Restablecer")
                }
                TextButton(enabled = !processing, onClick = onCancel) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun ThirdsGrid() {
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val stroke = 1.dp.toPx()
        drawRect(
            color = Color.White.copy(alpha = 0.8f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        listOf(size.width / 3f, size.width * 2f / 3f).forEach { x ->
            drawLine(Color.White.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, size.height), stroke)
        }
        listOf(size.height / 3f, size.height * 2f / 3f).forEach { y ->
            drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
}
