package bo.org.siafco.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.feature.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val authRepository: AuthGateway) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.loaded || _state.value.sessionLoading) return
        _state.value = _state.value.copy(sessionLoading = true, message = null)
        viewModelScope.launch {
            when (val result = authRepository.validateSession()) {
                is ApiResult.Success -> {
                    _state.value = HomeUiState(profile = result.value, loaded = true, sessionLoading = false)
                    if (result.value.accessLevel == AccessLevel.Pending) {
                        loadAffiliationRequest(force = true)
                    }
                }
                is ApiResult.HttpError -> _state.value = HomeUiState(
                    loaded = true,
                    sessionLoading = false,
                    loggedOut = result.code == 401
                )
                ApiResult.NetworkError -> _state.value = HomeUiState(
                    loaded = true,
                    sessionLoading = false,
                    message = UiMessage.Network
                )
                ApiResult.UnknownError -> _state.value = HomeUiState(
                    loaded = true,
                    sessionLoading = false,
                    message = UiMessage.Unknown
                )
            }
        }
    }

    fun refreshAffiliationRequest() {
        loadAffiliationRequest(force = true)
    }

    private fun loadAffiliationRequest(force: Boolean = false) {
        val current = _state.value
        if (current.requestLoading || (!force && current.affiliationRequest != null)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(requestLoading = true, requestMessage = null)
            when (val result = authRepository.affiliationRequest()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    requestLoading = false,
                    affiliationRequest = result.value,
                    requestMessage = null
                )
                is ApiResult.HttpError -> _state.value = _state.value.copy(
                    requestLoading = false,
                    loggedOut = result.code == 401,
                    requestMessage = when (result.code) {
                        401 -> null
                        403 -> UiMessage.Forbidden
                        404 -> UiMessage.RequestNotFound
                        429 -> UiMessage.RateLimited
                        else -> UiMessage.Unknown
                    }
                )
                ApiResult.NetworkError -> _state.value = _state.value.copy(
                    requestLoading = false,
                    requestMessage = UiMessage.Network
                )
                ApiResult.UnknownError -> _state.value = _state.value.copy(
                    requestLoading = false,
                    requestMessage = UiMessage.Unknown
                )
            }
        }
    }

    fun logout() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            when (authRepository.logout()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    loading = false,
                    loggedOut = true,
                    message = UiMessage.LogoutLocalSuccess
                )
                else -> {
                    authRepository.clearLocalSession()
                    _state.value = _state.value.copy(
                        loading = false,
                        loggedOut = true,
                        message = UiMessage.LogoutRemoteFailed
                    )
                }
            }
        }
    }
}

data class HomeUiState(
    val profile: SessionProfile? = null,
    val affiliationRequest: AffiliationRequestSummary? = null,
    val loaded: Boolean = false,
    val sessionLoading: Boolean = false,
    val loading: Boolean = false,
    val requestLoading: Boolean = false,
    val loggedOut: Boolean = false,
    val message: UiMessage? = null,
    val requestMessage: UiMessage? = null
)
