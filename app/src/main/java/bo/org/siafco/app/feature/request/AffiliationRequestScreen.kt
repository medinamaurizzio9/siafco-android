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
import java.util.Locale

@Composable
fun AffiliationRequestScreen(
    viewModel: AffiliationRequestViewModel,
    onBack: () -> Unit,
    onOpenCredential: () -> Unit,
    onSubmitPayment: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.register_back)) }
            Text(stringResource(R.string.home_request_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (state.loading && state.request == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.request?.let { request ->
                RequestStatusCard(request)
                RequestDetailCard(request)
                if (request.canViewCredential) {
                    Button(onClick = onOpenCredential, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.request_view_credential))
                    }
                }
                if (request.canStartPayment()) {
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
            InfoLine(stringResource(R.string.request_plan), request.planName)
            if (request.amountDue != null && !request.currency.isNullOrBlank()) {
                InfoLine(stringResource(R.string.request_amount), "${request.currency} ${String.format(Locale.US, "%.2f", request.amountDue)}")
            }
            InfoLine(stringResource(R.string.request_payment_status), request.paymentStatusLabel)
            InfoLine(stringResource(R.string.request_transaction_number), request.transactionNumber)
            InfoLine(stringResource(R.string.request_payment_date), request.paymentDate)
            InfoLine(stringResource(R.string.request_observations), request.observations ?: request.rejectionReason)
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

private fun AffiliationRequestSummary.canStartPayment(): Boolean =
    canSubmitPayment && (status == "pending_payment" || status == "rejected")

@Composable
private fun InfoLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Text("$label: $value")
    }
}
