package bo.org.siafco.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.toUiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthGateway) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, emailError = false, message = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, passwordError = false, message = null)
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(passwordVisible = !_state.value.passwordVisible)
    }

    fun login() {
        val current = _state.value
        if (current.loading) return
        val emailError = current.email.isBlank() || !current.email.contains("@")
        val passwordError = current.password.isBlank()
        if (emailError || passwordError) {
            _state.value = current.copy(emailError = emailError, passwordError = passwordError)
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(loading = true, message = null)
            when (val result = authRepository.login(current.email.trim(), current.password)) {
                is ApiResult.Success -> _state.value = LoginUiState(profile = result.value, authenticated = true)
                else -> _state.value = current.copy(loading = false, message = result.toUiMessage())
            }
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val loading: Boolean = false,
    val message: UiMessage? = null,
    val authenticated: Boolean = false,
    val profile: SessionProfile? = null
)
