package bo.org.siafco.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthRepository
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.feature.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.loaded) return
        viewModelScope.launch {
            when (val result = authRepository.validateSession()) {
                is ApiResult.Success -> _state.value = HomeUiState(profile = result.value, loaded = true)
                is ApiResult.HttpError -> _state.value = HomeUiState(loaded = true, loggedOut = result.code == 401)
                ApiResult.NetworkError -> _state.value = HomeUiState(loaded = true, message = UiMessage.Network)
                ApiResult.UnknownError -> _state.value = HomeUiState(loaded = true, message = UiMessage.Unknown)
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
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val loggedOut: Boolean = false,
    val message: UiMessage? = null
)
