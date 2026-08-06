package bo.org.siafco.app.feature.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.ui.BrandHeader
import bo.org.siafco.app.core.ui.CooperativeGold
import bo.org.siafco.app.core.ui.CooperativeNavy

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onAuthenticated: () -> Unit,
    onLoginRequired: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.validateSession() }
    LaunchedEffect(state.finished, state.authenticated) {
        if (state.finished) {
            if (state.authenticated) onAuthenticated() else onLoginRequired()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = CooperativeNavy) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandHeader(logoSize = 132.dp, titleColor = Color.White, subtitleColor = CooperativeGold)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(Modifier.size(36.dp), color = CooperativeGold)
            Spacer(Modifier.height(16.dp))
            Text(text = stringResource(R.string.splash_loading), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f))
        }
    }
}
