package bo.org.siafco.app.feature.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.canStartPaymentSubmission
import java.util.Locale

@Composable
fun AffiliationRequestScreen(
    viewModel: AffiliationRequestViewModel,
    onBack: () -> Unit,
    onSubmitPayment: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.register_back)) }
            Text(
                stringResource(R.string.home_request_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (state.loading && state.request == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.request?.let { request ->
                RequestStatusCard(request)
                RequestDetailCard(request)
                RequestPaymentInstructions(request)
                if (request.canStartPaymentSubmission()) {
                    Button(onClick = onSubmitPayment, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_payment_submit))
                    }
                }
            }
            state.message?.let { Text(stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = { viewModel.load(force = true) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_refresh_request))
            }
        }
    }
}

@Composable
private fun RequestStatusCard(request: AffiliationRequestSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(statusTitle(request), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(statusMessage(request), style = MaterialTheme.typography.bodyMedium)
            if (request.paymentStatus == "confirmed") {
                Text(stringResource(R.string.request_payment_confirmed), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RequestDetailCard(request: AffiliationRequestSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoLine(stringResource(R.string.request_code), request.requestCode)
            InfoLine(stringResource(R.string.request_status), request.statusLabel)
            InfoLine(stringResource(R.string.request_description), request.statusDescription)
            InfoLine(stringResource(R.string.request_plan), request.planName)
            if (request.amountDue != null && !request.currency.isNullOrBlank()) {
                InfoLine(stringResource(R.string.request_amount), "${request.currency} ${String.format(Locale.US, "%.2f", request.amountDue)}")
            }
            InfoLine(stringResource(R.string.request_payment_status), request.paymentStatusLabel)
            InfoLine(stringResource(R.string.request_transaction_number), request.transactionNumber)
            InfoLine(stringResource(R.string.request_payment_date), request.paymentDate)
            if (request.paidAmount != null && !request.currency.isNullOrBlank()) {
                InfoLine(stringResource(R.string.request_paid_amount), "${request.currency} ${String.format(Locale.US, "%.2f", request.paidAmount)}")
            }
            InfoLine(stringResource(R.string.request_observations), request.observations ?: request.rejectionReason)
        }
    }
}

@Composable
private fun RequestPaymentInstructions(request: AffiliationRequestSummary) {
    if (!request.canStartPaymentSubmission()) return
    val hasInstructions = !request.paymentBank.isNullOrBlank() ||
        !request.paymentHolder.isNullOrBlank() ||
        !request.paymentAccount.isNullOrBlank() ||
        !request.paymentInstructions.isNullOrBlank() ||
        !request.planPaymentInstructions.isNullOrBlank()
    if (!hasInstructions) return

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.request_payment_instructions_title), style = MaterialTheme.typography.titleMedium)
            InfoLine(stringResource(R.string.payment_bank), request.paymentBank)
            InfoLine(stringResource(R.string.payment_holder), request.paymentHolder)
            InfoLine(stringResource(R.string.payment_account), request.paymentAccount)
            InfoLine(stringResource(R.string.request_payment_instructions), request.paymentInstructions ?: request.planPaymentInstructions)
        }
    }
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

@Composable
private fun InfoLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Text("$label: $value")
    }
}
