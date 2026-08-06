package bo.org.siafco.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.data.repository.StoreGateway
import bo.org.siafco.app.data.repository.StoreOrderFilters
import bo.org.siafco.app.data.repository.StoreResult
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.AffiliateCapabilities
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.domain.canStartPaymentSubmission
import bo.org.siafco.app.feature.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthGateway,
    private val storeRepository: StoreGateway
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.loaded || _state.value.sessionLoading) return
        _state.value = _state.value.copy(sessionLoading = true, message = null)
        viewModelScope.launch {
            when (val result = authRepository.validateSession()) {
                is ApiResult.Success -> {
                    _state.value = HomeUiState(profile = result.value, loaded = true, sessionLoading = false)
                    loadAffiliationRequest(force = true)
                    if (result.value.accessLevel == AccessLevel.Active && result.value.affiliateStatus == "activo") {
                        loadAttentionOrders(force = true)
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

    fun retryAttentionOrders() {
        loadAttentionOrders(force = true)
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

    private fun loadAttentionOrders(force: Boolean = false) {
        val current = _state.value
        if (current.ordersLoading || (!force && current.ordersLoaded)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(ordersLoading = true, ordersMessage = null)
            when (val result = storeRepository.orders(StoreOrderFilters(attentionOnly = true, perPage = 3))) {
                is StoreResult.Success -> _state.value = _state.value.copy(
                    ordersLoading = false,
                    ordersLoaded = true,
                    attentionOrders = result.value.orders.filter(StoreOrder::requiresHomeAttention).take(3),
                    ordersMessage = null
                )
                StoreResult.Unauthorized -> _state.value = _state.value.copy(
                    ordersLoading = false,
                    ordersLoaded = true,
                    loggedOut = true
                )
                StoreResult.NetworkError -> _state.value = _state.value.copy(
                    ordersLoading = false,
                    ordersLoaded = true,
                    ordersMessage = UiMessage.Network
                )
                is StoreResult.Forbidden -> _state.value = _state.value.copy(ordersLoading = false, ordersLoaded = true, ordersMessage = UiMessage.Forbidden)
                is StoreResult.RateLimited -> _state.value = _state.value.copy(ordersLoading = false, ordersLoaded = true, ordersMessage = UiMessage.RateLimited)
                else -> _state.value = _state.value.copy(ordersLoading = false, ordersLoaded = true, ordersMessage = UiMessage.Unknown)
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
    val ordersLoading: Boolean = false,
    val ordersLoaded: Boolean = false,
    val attentionOrders: List<StoreOrder> = emptyList(),
    val loggedOut: Boolean = false,
    val message: UiMessage? = null,
    val requestMessage: UiMessage? = null,
    val ordersMessage: UiMessage? = null
) {
    val capabilities: AffiliateCapabilities
        get() = profile.toCapabilities(affiliationRequest)
}

private fun StoreOrder.requiresHomeAttention(): Boolean = status in AttentionOrderStatuses

private val AttentionOrderStatuses = setOf(
    "pendiente",
    "reservado",
    "esperando_pago",
    "pago_en_revision"
)

private fun SessionProfile?.toCapabilities(request: AffiliationRequestSummary?): AffiliateCapabilities {
    val profile = this
    val hasMobileProfileAccess = profile?.accessLevel == AccessLevel.Active || profile?.accessLevel == AccessLevel.Pending
    return AffiliateCapabilities(
        canViewProfile = profile != null && hasMobileProfileAccess,
        canEditProfile = profile != null && profile.allowedProfileFields.isNotEmpty(),
        canViewAffiliationRequest = request != null,
        canSubmitPayment = request?.canStartPaymentSubmission() == true,
        canViewCredential = request?.canViewCredential == true
    )
}
