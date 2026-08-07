package bo.org.siafco.app.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaNavy
import bo.org.siafco.app.core.ui.FigmaNavyDeep

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

    Surface(modifier = Modifier.fillMaxSize(), color = FigmaNavyDeep) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FigmaNavyDeep)
                .padding(horizontal = 36.dp, vertical = 42.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_logo),
                    contentDescription = stringResource(R.string.brand_logo_content_description),
                    modifier = Modifier.size(190.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = stringResource(R.string.splash_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = FigmaGold,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.splash_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = FigmaGold,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.splash_message),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.72f)
                    .height(7.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(7.dp)
                        .background(FigmaGold, RoundedCornerShape(999.dp))
                )
            }
            Text(
                text = stringResource(R.string.splash_loading),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}
