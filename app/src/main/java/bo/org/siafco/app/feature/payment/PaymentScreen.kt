package bo.org.siafco.app.feature.payment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.text.HumanTextInputNormalizer
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaMuted
import bo.org.siafco.app.core.ui.FigmaNavy
import bo.org.siafco.app.core.ui.FigmaNavyDeep
import bo.org.siafco.app.core.ui.FigmaDateField
import bo.org.siafco.app.core.ui.FigmaPrimaryButton
import bo.org.siafco.app.core.ui.FigmaSecondaryButton
import bo.org.siafco.app.core.text.TextInputNormalization
import bo.org.siafco.app.data.payment.PaymentQrDownloadResult
import bo.org.siafco.app.data.payment.PaymentQrDownloader
import bo.org.siafco.app.data.receipt.ReceiptPreparer
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.canStartPaymentSubmission
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val receiptPreparer = remember(context) { ReceiptPreparer(context) }
    val scope = rememberCoroutineScope()
    val qrDownloadStartedMessage = stringResource(R.string.payment_qr_download_started, PaymentQrDownloader.FILE_NAME)
    var confirmBack by remember { mutableStateOf(false) }

    fun requestBack() {
        when (PaymentBackPolicy.decide(state.submitting, state.networkRetryAvailable)) {
            PaymentBackDecision.ConfirmAndPreserveDraft -> confirmBack = true
            PaymentBackDecision.LeaveAndClearDraft -> {
                viewModel.clearSensitiveDraft()
                onBack()
            }
        }
    }

    BackHandler {
        requestBack()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.setReceipt(receiptPreparer.prepare(uri))
        }
    }

    LaunchedEffect(Unit) { viewModel.loadRequest() }
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PaymentHeader(onBack = { requestBack() })

            if (state.loadingRequest && state.request == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.request?.let { request ->
                    if (state.submitted) {
                        PaymentSuccess(
                            request = request,
                            onWhatsapp = {
                                val url = PaymentSupportWhatsapp.url(request.supportPhone, request.requestCode) ?: return@PaymentSuccess
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure {
                                    if (it is ActivityNotFoundException) {
                                        Toast.makeText(context, R.string.payment_whatsapp_unavailable, Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, R.string.payment_whatsapp_error, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onHome = onSubmitted
                        )
                    } else {
                        PaymentRequestSummary(
                            request = request,
                            onDownloadQr = {
                                scope.launch {
                                    when (PaymentQrDownloader.save(context, request.paymentQrUrl)) {
                                        is PaymentQrDownloadResult.Saved -> Toast.makeText(
                                            context,
                                            qrDownloadStartedMessage,
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
                        PaymentForm(
                            state = state,
                            onTransactionNumber = viewModel::updateTransactionNumber,
                            onPaymentDate = viewModel::updatePaymentDate,
                            onPaidAmount = viewModel::updatePaidAmount,
                            onPayerName = viewModel::updatePayerName,
                            onBankName = viewModel::updateBankName,
                            onPickReceipt = { launcher.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }
                        )
                        FigmaPrimaryButton(
                            text = if (state.networkRetryAvailable) {
                                stringResource(R.string.payment_retry_same)
                            } else {
                                stringResource(R.string.payment_submit)
                            },
                            onClick = viewModel::submit,
                            enabled = !state.submitting && request.canStartPaymentSubmission(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.submitting) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }

                state.message?.let { Text(text = stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
                state.blockingMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

                if (!state.submitted) {
                    FigmaSecondaryButton(
                        text = stringResource(R.string.register_back),
                        onClick = { requestBack() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    if (confirmBack) {
        AlertDialog(
            onDismissRequest = { confirmBack = false },
            title = { Text("Volver a Home") },
            text = {
                Text("Existe un envío pendiente o incierto. Se conservará el borrador para reintentar.")
            },
            confirmButton = {
                Button(onClick = {
                    confirmBack = false
                    onBack()
                }) {
                    Text("Volver")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBack = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PaymentHeader(onBack: () -> Unit) {
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
                text = stringResource(R.string.payment_title),
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
private fun PaymentRequestSummary(request: AffiliationRequestSummary, onDownloadQr: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.home_request_code, request.requestCode), color = FigmaMuted, fontWeight = FontWeight.Bold)
            request.planName?.let {
                Text(it, color = FigmaNavyDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            if (request.amountDue != null && !request.currency.isNullOrBlank()) {
                Text("${request.currency} ${formatAmount(request.amountDue)}", color = FigmaNavyDeep, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
            if (!request.paymentQrUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(214.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(3.dp, FigmaGold, RoundedCornerShape(24.dp))
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
                    onClick = onDownloadQr,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(stringResource(R.string.payment_qr_unavailable), color = FigmaMuted)
            }
            Text(stringResource(R.string.payment_bank_details), color = FigmaNavyDeep, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            PaymentInstructionLine(R.string.payment_bank, request.paymentBank)
            PaymentInstructionLine(R.string.payment_holder, request.paymentHolder)
            PaymentInstructionLine(R.string.payment_account, request.paymentAccount)
            (request.paymentInstructions ?: request.planPaymentInstructions)
                ?.takeIf(String::isNotBlank)
                ?.let { Text(stringResource(R.string.payment_instructions, it), color = FigmaMuted) }
            request.paymentStatusLabel?.let { Text(stringResource(R.string.home_request_payment, it), color = FigmaMuted) }
        }
    }
}

@Composable
private fun PaymentInstructionLine(label: Int, value: String?) {
    value?.takeIf(String::isNotBlank)?.let {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(stringResource(label), color = FigmaMuted, modifier = Modifier.weight(0.42f))
            Text(it, color = FigmaNavyDeep, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
        }
    }
}

@Composable
private fun PaymentSuccess(
    request: AffiliationRequestSummary,
    onWhatsapp: () -> Unit,
    onHome: () -> Unit
) {
    val displayPhone = PaymentSupportWhatsapp.displayPhone(request.supportPhone)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(FigmaGold.copy(alpha = 0.18f), CircleShape)
                    .border(2.dp, FigmaGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = FigmaNavyDeep, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            }
            Text(
                text = stringResource(R.string.payment_success_title),
                color = FigmaNavyDeep,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.payment_success_status),
                modifier = Modifier
                    .background(FigmaGold.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                    .border(1.dp, FigmaGold.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = FigmaNavyDeep,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.payment_success_ticket, request.requestCode),
                color = FigmaNavyDeep,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (displayPhone != null) {
                    stringResource(R.string.payment_success_body_with_phone, displayPhone)
                } else {
                    stringResource(R.string.payment_success_body_without_phone)
                },
                color = FigmaMuted,
                textAlign = TextAlign.Center
            )
            if (displayPhone != null) {
                FigmaSecondaryButton(
                    text = stringResource(R.string.payment_contact_whatsapp),
                    onClick = onWhatsapp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FigmaPrimaryButton(
                text = stringResource(R.string.payment_back_home),
                onClick = onHome,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PaymentForm(
    state: PaymentUiState,
    onTransactionNumber: (String) -> Unit,
    onPaymentDate: (String) -> Unit,
    onPaidAmount: (String) -> Unit,
    onPayerName: (String) -> Unit,
    onBankName: (String) -> Unit,
    onPickReceipt: () -> Unit
) {
    val form = state.form
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PaymentTextField(
            value = form.transactionNumber,
            onValueChange = onTransactionNumber,
            label = { Text(stringResource(R.string.payment_transaction_number)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("transaction_number"),
            supportingText = { FieldError(state, "transaction_number") },
            singleLine = true
        )
        FigmaDateField(
            value = form.paymentDate,
            onValueChange = onPaymentDate,
            label = stringResource(R.string.payment_date),
            modifier = Modifier.fillMaxWidth(),
            error = state.fieldErrors["payment_date"]
        )
        PaymentTextField(
            value = form.paidAmount,
            onValueChange = onPaidAmount,
            label = { Text(stringResource(R.string.payment_amount)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("paid_amount"),
            supportingText = { FieldError(state, "paid_amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        PaymentHumanTextField(
            value = form.payerName,
            onValueChange = onPayerName,
            label = { Text(stringResource(R.string.payment_payer)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("payer_name"),
            supportingText = { FieldError(state, "payer_name") }
        )
        PaymentHumanTextField(
            value = form.bankName,
            onValueChange = onBankName,
            label = { Text(stringResource(R.string.payment_bank_name)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("bank_name"),
            supportingText = { FieldError(state, "bank_name") }
        )
        FigmaSecondaryButton(
            text = stringResource(R.string.payment_pick_receipt),
            onClick = onPickReceipt,
            modifier = Modifier.fillMaxWidth()
        )
        state.fieldErrors["receipt"]?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
        form.receipt?.let { receipt ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (receipt.canPreviewImage) {
                    Image(
                        painter = rememberAsyncImagePainter(receipt.file),
                        contentDescription = stringResource(R.string.payment_receipt_preview),
                        modifier = Modifier.size(72.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receipt.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "${receipt.mimeType} - ${receipt.sizeBytes / 1024} KB")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.payment_summary),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = stringResource(R.string.payment_summary_amount, form.paidAmount.ifBlank { "-" }))
        Text(text = stringResource(R.string.payment_summary_receipt, form.receipt?.displayName ?: "-"))
    }
}

@Composable
private fun PaymentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(22.dp),
        colors = paymentTextFieldColors()
    )
}

@Composable
private fun PaymentHumanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { incoming ->
            val normalized = HumanTextInputNormalizer.visual(incoming, TextInputNormalization.Human)
            fieldValue = normalized
            onValueChange(normalized.text)
        },
        label = label,
        modifier = modifier,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Text
        ),
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        colors = paymentTextFieldColors()
    )
}

@Composable
private fun paymentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF2F4F7),
    errorContainerColor = Color.White,
    focusedIndicatorColor = FigmaGold,
    unfocusedIndicatorColor = FigmaGold.copy(alpha = 0.72f),
    disabledIndicatorColor = FigmaMuted.copy(alpha = 0.35f),
    errorIndicatorColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = FigmaNavy,
    unfocusedLabelColor = FigmaMuted,
    cursorColor = FigmaNavy,
    errorCursorColor = MaterialTheme.colorScheme.error
)

@Composable
private fun FieldError(state: PaymentUiState, field: String) {
    state.fieldErrors[field]?.let { Text(it) }
}

private fun formatAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)
