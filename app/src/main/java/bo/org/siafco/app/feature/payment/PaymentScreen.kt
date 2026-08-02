package bo.org.siafco.app.feature.payment

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.data.receipt.ReceiptPreparer
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.canStartPaymentSubmission
import coil3.compose.rememberAsyncImagePainter
import java.time.LocalDate
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.payment_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (state.loadingRequest && state.request == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            state.request?.let { request ->
                PaymentRequestSummary(request)
                PaymentForm(
                    state = state,
                    onTransactionNumber = viewModel::updateTransactionNumber,
                    onPaymentDate = viewModel::updatePaymentDate,
                    onPaidAmount = viewModel::updatePaidAmount,
                    onPayerName = viewModel::updatePayerName,
                    onBankName = viewModel::updateBankName,
                    onPickReceipt = { launcher.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }
                )
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.submitting && !state.submitted && request.canStartPaymentSubmission(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.networkRetryAvailable) {
                            stringResource(R.string.payment_retry_same)
                        } else {
                            stringResource(R.string.payment_submit)
                        }
                    )
                }
                if (state.submitting) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                if (state.submitted) {
                    Text(
                        text = stringResource(R.string.payment_submitted),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(onClick = onSubmitted, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.payment_back_home))
                    }
                }
            }

            state.message?.let { Text(text = stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
            state.blockingMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            OutlinedButton(
                onClick = { requestBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.register_back))
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
private fun PaymentRequestSummary(request: AffiliationRequestSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_request_code, request.requestCode), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.home_request_status, request.statusLabel))
            request.planName?.let { Text(stringResource(R.string.home_request_plan, it)) }
            if (request.amountDue != null && !request.currency.isNullOrBlank()) {
                Text(stringResource(R.string.home_request_amount, request.currency, formatAmount(request.amountDue)))
            }
            PaymentInstructionLine(R.string.payment_bank, request.paymentBank)
            PaymentInstructionLine(R.string.payment_holder, request.paymentHolder)
            PaymentInstructionLine(R.string.payment_account, request.paymentAccount)
            (request.paymentInstructions ?: request.planPaymentInstructions)
                ?.takeIf(String::isNotBlank)
                ?.let { Text(stringResource(R.string.payment_instructions, it)) }
            request.paymentStatusLabel?.let { Text(stringResource(R.string.home_request_payment, it)) }
        }
    }
}

@Composable
private fun PaymentInstructionLine(label: Int, value: String?) {
    value?.takeIf(String::isNotBlank)?.let {
        Text("${stringResource(label)}: $it")
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
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = form.transactionNumber,
            onValueChange = onTransactionNumber,
            label = { Text(stringResource(R.string.payment_transaction_number)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("transaction_number"),
            supportingText = { FieldError(state, "transaction_number") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = form.paymentDate,
                onValueChange = onPaymentDate,
                label = { Text(stringResource(R.string.payment_date)) },
                modifier = Modifier.weight(1f),
                isError = state.fieldErrors.containsKey("payment_date"),
                supportingText = { FieldError(state, "payment_date") }
            )
            OutlinedButton(
                onClick = {
                    val today = LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onPaymentDate(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                        },
                        today.year,
                        today.monthValue - 1,
                        today.dayOfMonth
                    ).show()
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(stringResource(R.string.payment_pick_date))
            }
        }
        OutlinedTextField(
            value = form.paidAmount,
            onValueChange = onPaidAmount,
            label = { Text(stringResource(R.string.payment_amount)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("paid_amount"),
            supportingText = { FieldError(state, "paid_amount") }
        )
        OutlinedTextField(
            value = form.payerName,
            onValueChange = onPayerName,
            label = { Text(stringResource(R.string.payment_payer)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("payer_name"),
            supportingText = { FieldError(state, "payer_name") }
        )
        OutlinedTextField(
            value = form.bankName,
            onValueChange = onBankName,
            label = { Text(stringResource(R.string.payment_bank_name)) },
            modifier = Modifier.fillMaxWidth(),
            isError = state.fieldErrors.containsKey("bank_name"),
            supportingText = { FieldError(state, "bank_name") }
        )
        OutlinedButton(onClick = onPickReceipt, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.payment_pick_receipt))
        }
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
private fun FieldError(state: PaymentUiState, field: String) {
    state.fieldErrors[field]?.let { Text(it) }
}

private fun formatAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)
