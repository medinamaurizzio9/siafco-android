package bo.org.siafco.app.feature.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.feature.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AffiliationRequestViewModel(private val authRepository: AuthGateway) : ViewModel() {
    private val _state = MutableStateFlow(AffiliationRequestUiState())
    val state: StateFlow<AffiliationRequestUiState> = _state.asStateFlow()

    fun load(force: Boolean = false) {
        if (_state.value.loading || (_state.value.loaded && !force)) return
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            when (val result = authRepository.affiliationRequest()) {
                is ApiResult.Success -> _state.value = AffiliationRequestUiState(
                    loaded = true,
                    loading = false,
                    request = result.value
                )
                is ApiResult.HttpError -> _state.value = _state.value.copy(
                    loaded = true,
                    loading = false,
                    message = when (result.code) {
                        401 -> UiMessage.Unauthorized
                        403 -> UiMessage.Forbidden
                        404 -> UiMessage.RequestNotFound
                        429 -> UiMessage.RateLimited
                        else -> UiMessage.Unknown
                    }
                )
                ApiResult.NetworkError -> _state.value = _state.value.copy(loaded = true, loading = false, message = UiMessage.Network)
                ApiResult.UnknownError -> _state.value = _state.value.copy(loaded = true, loading = false, message = UiMessage.Unknown)
            }
        }
    }
}

data class AffiliationRequestUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val request: AffiliationRequestSummary? = null,
    val message: UiMessage? = null
)
