package bo.org.siafco.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.ui.BrandHeader
import bo.org.siafco.app.core.ui.CooperativeSpacing
import bo.org.siafco.app.core.ui.CooperativeTextSecondary
import bo.org.siafco.app.core.ui.InstitutionalCard
import bo.org.siafco.app.core.ui.PrimaryButton
import bo.org.siafco.app.core.ui.SecondaryButton
import bo.org.siafco.app.core.ui.SecureTextField

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandHeader(logoSize = 112.dp)
            InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(text = stringResource(R.string.login_intro), style = MaterialTheme.typography.bodyMedium, color = CooperativeTextSecondary)
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.login_email)) },
                        singleLine = true,
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
                    SecureTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = stringResource(R.string.login_password),
                        visible = state.passwordVisible,
                        isError = state.passwordError,
                        trailingIcon = {
                            TextButton(onClick = viewModel::togglePasswordVisibility) {
                                Text(
                                    stringResource(
                                        if (state.passwordVisible) R.string.login_hide_password else R.string.login_show_password
                                    )
                                )
                            }
                        },
                        supportingText = {
                            if (state.passwordError) Text(stringResource(R.string.login_password_required))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    state.message?.let {
                        Text(
                            text = stringResource(it.resId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    PrimaryButton(text = stringResource(R.string.login_submit), onClick = viewModel::login, enabled = !state.loading, modifier = Modifier.fillMaxWidth())
                    SecondaryButton(text = stringResource(R.string.login_register), onClick = onRegister, enabled = !state.loading, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
