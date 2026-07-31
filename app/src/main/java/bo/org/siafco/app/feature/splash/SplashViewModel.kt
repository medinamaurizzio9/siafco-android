package bo.org.siafco.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthRepository
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.toUiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    fun validateSession() {
        if (_state.value.finished) return
        viewModelScope.launch {
            when (val result = authRepository.validateSession()) {
                is ApiResult.Success -> _state.value = SplashUiState(profile = result.value, authenticated = true, finished = true)
                is ApiResult.HttpError -> _state.value = SplashUiState(message = result.toUiMessage(), authenticated = false, finished = true)
                ApiResult.NetworkError -> _state.value = SplashUiState(message = UiMessage.Network, authenticated = false, finished = true)
                ApiResult.UnknownError -> _state.value = SplashUiState(message = UiMessage.Unknown, authenticated = false, finished = true)
            }
        }
    }
}

data class SplashUiState(
    val profile: SessionProfile? = null,
    val message: UiMessage? = null,
    val authenticated: Boolean = false,
    val finished: Boolean = false
)
