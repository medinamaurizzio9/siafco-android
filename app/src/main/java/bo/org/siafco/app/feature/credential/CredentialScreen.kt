package bo.org.siafco.app.feature.credential

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import bo.org.siafco.app.R
import bo.org.siafco.app.data.repository.MobileCredential
import coil3.compose.AsyncImage

private val Navy = Color(0xFF0B1F3A)
private val Gold = Color(0xFFD8A928)
private val Ink = Color(0xFF101827)

@Composable
fun CredentialScreen(
    viewModel: CredentialViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var openUrlError by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
            if (window != null) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F6F8)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                Text(stringResource(R.string.register_back))
            }

            Button(
                onClick = { viewModel.load(force = true) },
                enabled = !state.loading,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(stringResource(R.string.credential_reload))
            }

            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.credential != null -> {
                    LandscapeCredentialCard(
                        credential = state.credential!!,
                        onVerify = {
                            openUrlError = !openVerificationUrl(context, state.credential!!.verificationUrl)
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(0.88f)
                            .aspectRatio(1.586f, matchHeightConstraintsFirst = true)
                    )
                }
                state.message != null -> Text(
                    stringResource(state.message!!.resId),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.messageText != null -> Text(
                    state.messageText.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (openUrlError) {
                Text(
                    stringResource(R.string.credential_open_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun LandscapeCredentialCard(
    credential: MobileCredential,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(credential.qrPngBytes) {
        BitmapFactory.decodeByteArray(credential.qrPngBytes, 0, credential.qrPngBytes.size)?.asImageBitmap()
    }

    Card(
        modifier = modifier.border(2.dp, Gold, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Navy, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = credential.institutionName.ifBlank { "COOPERATIVA TIERRA BENDITA" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.credential_card_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = Gold
                    )
                }
                Text(
                    text = credential.statusLabel.ifBlank { credential.status },
                    style = MaterialTheme.typography.titleSmall,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        credential.affiliateName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    CredentialLine(stringResource(R.string.credential_registration), credential.registrationNumber)
                    CredentialLine(stringResource(R.string.credential_sector), credential.sector)
                    CredentialLine(stringResource(R.string.credential_regional), credential.regional)
                    CredentialLine(stringResource(R.string.credential_issued_at), credential.issuedAt)
                }

                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Gold, RoundedCornerShape(10.dp))
                        .background(Color(0xFFE7EBF0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (credential.photoUrl != null) {
                        AsyncImage(
                            model = credential.photoUrl,
                            contentDescription = stringResource(R.string.profile_photo),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = credential.affiliateName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Navy
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Gold, RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = stringResource(R.string.credential_qr_content_description),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.credential_invalid_qr),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Button(onClick = onVerify) {
                        Text(stringResource(R.string.credential_verify))
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF596579))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
    }
}

private fun openVerificationUrl(context: Context, url: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: RuntimeException) {
        false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
