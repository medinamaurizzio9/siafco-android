package bo.org.siafco.app.feature.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.data.payment.PendingPaymentStore
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.data.repository.PaymentGateway
import bo.org.siafco.app.data.repository.PaymentRepositoryResult
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.PaymentForm
import bo.org.siafco.app.domain.PaymentValidator
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.feature.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class PaymentViewModel(
    private val paymentRepository: PaymentGateway,
    private val authRepository: AuthGateway,
    private val pendingPaymentStore: PendingPaymentStore
) : ViewModel() {
    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    private var idempotencyKey: String? = null
    private var payloadSignature: String? = null

    fun loadRequest() {
        val current = _state.value
        if (current.request != null || current.loadingRequest) return
        _state.value = current.copy(loadingRequest = true, message = null)
        viewModelScope.launch {
            when (val result = authRepository.affiliationRequest()) {
                is bo.org.siafco.app.core.network.ApiResult.Success -> _state.value = _state.value.copy(
                    loadingRequest = false,
                    request = result.value
                )
                is bo.org.siafco.app.core.network.ApiResult.HttpError -> _state.value = _state.value.copy(
                    loadingRequest = false,
                    loggedOut = result.code == 401,
                    message = when (result.code) {
                        401 -> UiMessage.Unauthorized
                        403 -> UiMessage.Forbidden
                        404 -> UiMessage.RequestNotFound
                        429 -> UiMessage.RateLimited
                        else -> UiMessage.Unknown
                    }
                )
                bo.org.siafco.app.core.network.ApiResult.NetworkError -> _state.value = _state.value.copy(
                    loadingRequest = false,
                    message = UiMessage.Network
                )
                bo.org.siafco.app.core.network.ApiResult.UnknownError -> _state.value = _state.value.copy(
                    loadingRequest = false,
                    message = UiMessage.Unknown
                )
            }
        }
    }

    fun updateTransactionNumber(value: String) = updateForm { copy(transactionNumber = value) }
    fun updatePaymentDate(value: String) = updateForm { copy(paymentDate = value) }
    fun updatePaidAmount(value: String) = updateForm { copy(paidAmount = value) }
    fun updatePayerName(value: String) = updateForm { copy(payerName = value) }
    fun updateBankName(value: String) = updateForm { copy(bankName = value) }

    fun setReceipt(receipt: PreparedReceipt?) {
        updateForm {
            this.receipt?.file?.takeIf { it != receipt?.file }?.delete()
            copy(receipt = receipt)
        }
    }

    fun clearSensitiveDraft() {
        _state.value.form.receipt?.file?.delete()
        resetAttempt()
        _state.value = _state.value.copy(form = PaymentForm(), fieldErrors = emptyMap(), message = null)
    }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val validation = PaymentValidator.validate(current.form, LocalDate.now())
        if (!validation.isValid) {
            _state.value = current.copy(fieldErrors = validation.errors, message = UiMessage.Validation)
            return
        }
        val payload = requireNotNull(validation.payload)
        val receipt = requireNotNull(current.form.receipt)
        val key = if (payloadSignature == payload.signature && idempotencyKey != null) {
            requireNotNull(idempotencyKey)
        } else {
            UUID.randomUUID().toString().also {
                idempotencyKey = it
                payloadSignature = payload.signature
            }
        }

        _state.value = current.copy(
            submitting = true,
            fieldErrors = emptyMap(),
            message = null,
            networkRetryAvailable = false
        )
        viewModelScope.launch {
            when (val result = paymentRepository.submit(key, payload, receipt)) {
                is PaymentRepositoryResult.Success -> {
                    receipt.file.delete()
                    pendingPaymentStore.clear()
                    resetAttempt()
                    _state.value = _state.value.copy(
                        submitting = false,
                        request = result.request,
                        submitted = true,
                        message = null,
                        form = PaymentForm()
                    )
                }
                PaymentRepositoryResult.NetworkError -> {
                    pendingPaymentStore.save(key, payload.signature, _state.value.form)
                    _state.value = _state.value.copy(
                        submitting = false,
                        message = UiMessage.Network,
                        networkRetryAvailable = true
                    )
                }
                is PaymentRepositoryResult.ValidationError -> _state.value = _state.value.copy(
                    submitting = false,
                    fieldErrors = result.errors.mapValues { it.value.firstOrNull().orEmpty() },
                    message = UiMessage.Validation
                )
                is PaymentRepositoryResult.Conflict -> _state.value = _state.value.copy(
                    submitting = false,
                    message = UiMessage.Unknown,
                    blockingMessage = result.message ?: "La clave de idempotencia ya fue utilizada."
                )
                is PaymentRepositoryResult.Forbidden -> _state.value = _state.value.copy(
                    submitting = false,
                    message = UiMessage.Forbidden,
                    blockingMessage = result.message
                )
                is PaymentRepositoryResult.RateLimited -> _state.value = _state.value.copy(
                    submitting = false,
                    message = UiMessage.RateLimited,
                    blockingMessage = result.message
                )
                PaymentRepositoryResult.Unauthorized -> {
                    authRepository.clearLocalSession()
                    receipt.file.delete()
                    pendingPaymentStore.clear()
                    resetAttempt()
                    _state.value = _state.value.copy(submitting = false, loggedOut = true)
                }
                is PaymentRepositoryResult.HttpError -> _state.value = _state.value.copy(
                    submitting = false,
                    message = UiMessage.Unknown,
                    blockingMessage = result.message
                )
                PaymentRepositoryResult.UnknownError -> _state.value = _state.value.copy(
                    submitting = false,
                    message = UiMessage.Unknown
                )
            }
        }
    }

    private fun updateForm(block: PaymentForm.() -> PaymentForm) {
        val nextForm = _state.value.form.block()
        val nextSignature = PaymentValidator.validate(nextForm, LocalDate.now()).payload?.signature
        if (nextSignature != null && nextSignature != payloadSignature) resetAttempt()
        _state.value = _state.value.copy(
            form = nextForm,
            fieldErrors = emptyMap(),
            message = null,
            blockingMessage = null,
            networkRetryAvailable = false
        )
    }

    private fun resetAttempt() {
        idempotencyKey = null
        payloadSignature = null
    }
}

data class PaymentUiState(
    val request: AffiliationRequestSummary? = null,
    val form: PaymentForm = PaymentForm(),
    val loadingRequest: Boolean = false,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val loggedOut: Boolean = false,
    val networkRetryAvailable: Boolean = false,
    val message: UiMessage? = null,
    val blockingMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)
