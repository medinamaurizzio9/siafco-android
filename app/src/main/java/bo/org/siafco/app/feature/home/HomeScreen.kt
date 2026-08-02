package bo.org.siafco.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.paymentDisabledReason
import bo.org.siafco.app.feature.UiMessage
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProfile: () -> Unit,
    onOpenAffiliationRequest: () -> Unit,
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val profile = state.profile
            if (!state.loaded && profile == null) {
                CircularProgressIndicator()
                return@Column
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.home_greeting), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = profile?.name.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = profile?.affiliateStatusLabel.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(
                            if (profile?.accessLevel == AccessLevel.Active) {
                                R.string.home_status_active
                            } else {
                                R.string.home_status_pending
                            }
                        ),
                        color = if (profile?.accessLevel == AccessLevel.Active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                    state.message?.let {
                        Text(text = stringResource(it.resId), color = MaterialTheme.colorScheme.secondary)
                    }
                    AffiliationRequestSection(
                        request = state.affiliationRequest,
                        loading = state.requestLoading,
                        message = state.requestMessage,
                        onRefresh = viewModel::refreshAffiliationRequest
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.capabilities.canViewProfile
                    ) {
                        Text(stringResource(R.string.home_profile))
                    }
                    OutlinedButton(
                        onClick = onOpenAffiliationRequest,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.capabilities.canViewAffiliationRequest
                    ) {
                        Text(stringResource(R.string.home_request))
                    }
                    OutlinedButton(
                        onClick = onSubmitPayment,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.capabilities.canSubmitPayment
                    ) {
                        Text(stringResource(R.string.home_payment_submit))
                    }
                    state.affiliationRequest?.paymentDisabledReason()?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.secondary)
                    }
                    Button(
                        onClick = viewModel::logout,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.home_logout))
                    }
                }
            }
        }
    }
}

@Composable
private fun AffiliationRequestSection(
    request: AffiliationRequestSummary?,
    loading: Boolean,
    message: UiMessage?,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.home_request_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (loading && request == null) {
            CircularProgressIndicator()
        }
        request?.let {
            Text(text = stringResource(R.string.home_request_code, it.requestCode))
            Text(text = stringResource(R.string.home_request_status, it.statusLabel))
            it.planName?.takeIf(String::isNotBlank)?.let { plan ->
                Text(text = stringResource(R.string.home_request_plan, plan))
            }
            if (it.amountDue != null && !it.currency.isNullOrBlank()) {
                Text(text = stringResource(R.string.home_request_amount, it.currency, formatAmount(it.amountDue)))
            }
            it.observations?.takeIf(String::isNotBlank)?.let { observations ->
                Text(text = stringResource(R.string.home_request_observations, observations))
            }
            Text(
                text = stringResource(
                    R.string.home_request_payment,
                    it.paymentStatusLabel ?: stringResource(R.string.home_request_payment_pending)
                )
            )
            Text(text = stringResource(R.string.home_request_capabilities, capabilitiesText(it)))
        }
        message?.let {
            Text(text = stringResource(it.resId), color = MaterialTheme.colorScheme.secondary)
        }
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
            Text(stringResource(R.string.home_refresh_request))
        }
    }
}

private fun formatAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)

private fun capabilitiesText(request: AffiliationRequestSummary): String {
    val capabilities = buildList {
        if (request.canSubmitPayment) add("registrar pago")
        if (request.canLogin) add("iniciar sesion")
        if (request.canViewCredential) add("ver credencial")
    }
    return capabilities.ifEmpty { listOf("sin acciones disponibles") }.joinToString()
}
