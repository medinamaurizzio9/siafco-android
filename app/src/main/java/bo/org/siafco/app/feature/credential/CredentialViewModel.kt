package bo.org.siafco.app.feature.credential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.data.repository.CredentialGateway
import bo.org.siafco.app.data.repository.CredentialResult
import bo.org.siafco.app.data.repository.MobileCredential
import bo.org.siafco.app.feature.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CredentialViewModel(private val repository: CredentialGateway) : ViewModel() {
    private val _state = MutableStateFlow(CredentialUiState())
    val state: StateFlow<CredentialUiState> = _state.asStateFlow()

    fun load(force: Boolean = false) {
        if (_state.value.loading || (_state.value.credential != null && !force)) return
        _state.value = _state.value.copy(loading = true, message = null, messageText = null)
        viewModelScope.launch {
            when (val result = repository.loadCredential()) {
                is CredentialResult.Success -> _state.value = CredentialUiState(credential = result.credential)
                CredentialResult.Unauthorized -> _state.value = CredentialUiState(loggedOut = true, message = UiMessage.Unauthorized)
                CredentialResult.Forbidden -> _state.value = CredentialUiState(messageText = "La credencial no esta disponible para tu estado actual.")
                CredentialResult.NotFound -> _state.value = CredentialUiState(messageText = "Aun no tienes una credencial generada.")
                CredentialResult.RateLimited -> _state.value = CredentialUiState(message = UiMessage.RateLimited)
                CredentialResult.NetworkError -> _state.value = _state.value.copy(loading = false, message = UiMessage.Network)
                CredentialResult.InvalidPayload -> _state.value = CredentialUiState(messageText = "La credencial recibida no es valida.")
                is CredentialResult.HttpError -> _state.value = CredentialUiState(message = UiMessage.Unknown)
            }
        }
    }
}

data class CredentialUiState(
    val loading: Boolean = false,
    val credential: MobileCredential? = null,
    val loggedOut: Boolean = false,
    val message: UiMessage? = null,
    val messageText: String? = null
)
