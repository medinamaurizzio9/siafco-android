package bo.org.siafco.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
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
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) {
                        Text(stringResource(R.string.home_profile))
                    }
                    OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) {
                        Text(stringResource(R.string.home_request))
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
