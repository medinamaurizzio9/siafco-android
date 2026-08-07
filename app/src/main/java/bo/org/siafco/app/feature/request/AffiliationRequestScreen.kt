package bo.org.siafco.app.feature.request

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaMuted
import bo.org.siafco.app.core.ui.FigmaNavy
import bo.org.siafco.app.core.ui.FigmaNavyDeep
import bo.org.siafco.app.core.ui.FigmaPrimaryButton
import bo.org.siafco.app.core.ui.FigmaSecondaryButton
import bo.org.siafco.app.data.payment.PaymentQrDownloadResult
import bo.org.siafco.app.data.payment.PaymentQrDownloader
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.feature.payment.PaymentSupportWhatsapp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AffiliationRequestScreen(
    viewModel: AffiliationRequestViewModel,
    onBack: () -> Unit,
    onOpenCredential: () -> Unit,
    onSubmitPayment: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RequestHeader(onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (state.loading && state.request == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                state.request?.let { request ->
                    RequestStatusCard(request)
                    PaymentQrCard(
                        request = request,
                        onDownload = {
                            scope.launch {
                                when (val result = PaymentQrDownloader.save(context, request.paymentQrUrl)) {
                                    is PaymentQrDownloadResult.Saved -> Toast.makeText(
                                        context,
                                        context.getString(R.string.payment_qr_download_started, result.fileName),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    PaymentQrDownloadResult.InvalidUrl -> Toast.makeText(
                                        context,
                                        R.string.payment_qr_download_invalid,
                                        Toast.LENGTH_LONG
                                    ).show()
                                    PaymentQrDownloadResult.Failed -> Toast.makeText(
                                        context,
                                        R.string.payment_qr_download_failed,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                    RequestDetailCard(request)
                    if (request.isPaymentUnderReview()) {
                        PaymentReviewSupportCard(
                            request = request,
                            onWhatsapp = {
                                val url = PaymentSupportWhatsapp.url(request.supportPhone, request.requestCode) ?: return@PaymentReviewSupportCard
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure {
                                    if (it is ActivityNotFoundException) {
                                        Toast.makeText(context, R.string.payment_whatsapp_unavailable, Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, R.string.payment_whatsapp_error, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                    if (request.canViewCredential) {
                        FigmaSecondaryButton(
                            text = stringResource(R.string.request_view_credential),
                            onClick = onOpenCredential,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (request.canStartPayment()) {
                        FigmaPrimaryButton(
                            text = stringResource(R.string.payment_already_paid),
                            onClick = onSubmitPayment,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                state.message?.let { Text(stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
                FigmaSecondaryButton(
                    text = stringResource(R.string.home_refresh_request),
                    onClick = { viewModel.load(force = true) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RequestHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaNavyDeep, RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp))
            .padding(horizontal = 24.dp, vertical = 38.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Card(
                onClick = onBack,
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.register_back),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.home_request_title),
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun RequestStatusCard(request: AffiliationRequestSummary) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_request_code, request.requestCode), color = FigmaMuted, fontWeight = FontWeight.Bold)
            Text(statusTitle(request), color = FigmaNavyDeep, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(statusMessage(request), color = FigmaMuted, style = MaterialTheme.typography.bodyMedium)
            StatusPill(request.statusLabel)
            if (request.paymentStatus == "confirmed") {
                Text(stringResource(R.string.request_payment_confirmed), color = FigmaNavy, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PaymentQrCard(request: AffiliationRequestSummary, onDownload: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = FigmaNavyDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.payment_qr_title),
                color = FigmaGold,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            if (request.amountDue != null && !request.currency.isNullOrBlank()) {
                Text(
                    text = "${request.currency} ${formatAmount(request.amountDue)}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            }
            if (!request.paymentQrUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .border(3.dp, FigmaGold, RoundedCornerShape(26.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = request.paymentQrUrl,
                        contentDescription = stringResource(R.string.payment_qr_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                FigmaSecondaryButton(
                    text = stringResource(R.string.payment_qr_download),
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.payment_qr_unavailable),
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RequestDetailCard(request: AffiliationRequestSummary) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.payment_bank_details), color = FigmaNavyDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            InfoLine(stringResource(R.string.request_plan), request.planName)
            if (request.amountDue != null && !request.currency.isNullOrBlank()) {
                InfoLine(stringResource(R.string.request_amount), "${request.currency} ${formatAmount(request.amountDue)}")
            }
            InfoLine(stringResource(R.string.payment_bank), request.paymentBank)
            InfoLine(stringResource(R.string.payment_holder), request.paymentHolder)
            InfoLine(stringResource(R.string.payment_account), request.paymentAccount)
            InfoLine(stringResource(R.string.request_payment_status), request.paymentStatusLabel)
            InfoLine(stringResource(R.string.request_transaction_number), request.transactionNumber)
            InfoLine(stringResource(R.string.request_payment_date), request.paymentDate)
            InfoLine(stringResource(R.string.request_observations), request.observations ?: request.rejectionReason)
            (request.paymentInstructions ?: request.planPaymentInstructions)
                ?.takeIf(String::isNotBlank)
                ?.let { Text(stringResource(R.string.payment_instructions, it), color = FigmaMuted) }
        }
    }
}

@Composable
private fun PaymentReviewSupportCard(request: AffiliationRequestSummary, onWhatsapp: () -> Unit) {
    val displayPhone = PaymentSupportWhatsapp.displayPhone(request.supportPhone)
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAEA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.payment_success_status), color = FigmaNavyDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.payment_success_ticket, request.requestCode), color = FigmaNavyDeep, fontWeight = FontWeight.Bold)
            Text(
                text = if (displayPhone != null) {
                    stringResource(R.string.payment_success_body_with_phone, displayPhone)
                } else {
                    stringResource(R.string.payment_success_body_without_phone)
                },
                color = FigmaMuted
            )
            if (displayPhone != null) {
                FigmaSecondaryButton(
                    text = stringResource(R.string.payment_contact_whatsapp),
                    onClick = onWhatsapp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(FigmaGold.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .border(1.dp, FigmaGold.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        color = FigmaNavyDeep,
        fontWeight = FontWeight.Black
    )
}

private fun statusTitle(request: AffiliationRequestSummary): String = when (request.status) {
    "approved", "active", "payment_approved" -> "Solicitud aprobada"
    "observed", "observado" -> "Solicitud observada"
    else -> request.statusLabel
}

private fun statusMessage(request: AffiliationRequestSummary): String = when (request.status) {
    "approved", "active", "payment_approved" -> "Tu afiliacion fue aprobada correctamente."
    "observed", "observado" -> request.observations ?: "Revisa las observaciones para continuar."
    else -> request.statusDescription ?: request.statusLabel
}

private fun AffiliationRequestSummary.canStartPayment(): Boolean =
    canSubmitPayment && (status == "pending_payment" || status == "rejected")

private fun AffiliationRequestSummary.isPaymentUnderReview(): Boolean =
    status == "payment_submitted" || paymentStatus == "pending" || paymentStatus == "under_review"

@Composable
private fun InfoLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(label, color = FigmaMuted, modifier = Modifier.weight(0.42f))
            Text(value, color = FigmaNavyDeep, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
        }
    }
}

private fun formatAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)
