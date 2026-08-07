package bo.org.siafco.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.ui.FigmaBrandRow
import bo.org.siafco.app.core.ui.FigmaDisabledRememberRow
import bo.org.siafco.app.core.ui.FigmaDividerOr
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaHeaderPanel
import bo.org.siafco.app.core.ui.FigmaMuted
import bo.org.siafco.app.core.ui.FigmaNavy
import bo.org.siafco.app.core.ui.FigmaPasswordField
import bo.org.siafco.app.core.ui.FigmaPrimaryButton
import bo.org.siafco.app.core.ui.FigmaSecondaryButton
import bo.org.siafco.app.core.ui.FigmaTextField

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.authenticated) {
        if (state.authenticated) onLoginSuccess()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
        ) {
            FigmaHeaderPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            ) {
                FigmaBrandRow(modifier = Modifier.fillMaxWidth(), logoSize = 76.dp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = FigmaNavy
                    )
                    Text(
                        text = stringResource(R.string.login_intro),
                        style = MaterialTheme.typography.titleMedium,
                        color = FigmaMuted
                    )
                }
                FigmaTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = stringResource(R.string.login_email),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_mail),
                            contentDescription = null,
                            tint = FigmaNavy
                        )
                    },
                    isError = state.emailError,
                    supportingText = {
                        if (state.emailError) {
                            Text(
                                stringResource(
                                    if (state.email.isBlank()) R.string.login_email_required else R.string.login_email_invalid
                                )
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                FigmaPasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.login_password),
                    visible = state.passwordVisible,
                    onToggleVisible = viewModel::togglePasswordVisibility,
                    isError = state.passwordError,
                    supportingText = {
                        if (state.passwordError) Text(stringResource(R.string.login_password_required))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                state.message?.let {
                    Text(
                        text = stringResource(it.resId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FigmaPrimaryButton(
                    text = stringResource(R.string.login_submit),
                    onClick = viewModel::login,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
                FigmaDisabledRememberRow(
                    text = stringResource(R.string.login_remember),
                    trailingText = stringResource(R.string.login_forgot_password)
                )
                FigmaDividerOr()
                FigmaSecondaryButton(
                    text = stringResource(R.string.login_biometric),
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_fingerprint),
                            contentDescription = null,
                            tint = FigmaGold.copy(alpha = 0.56f)
                        )
                    }
                )
                Text(
                    text = stringResource(R.string.login_future_features),
                    style = MaterialTheme.typography.bodySmall,
                    color = FigmaMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
