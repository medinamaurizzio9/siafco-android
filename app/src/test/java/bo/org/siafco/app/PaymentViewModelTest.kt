package bo.org.siafco.app

import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.payment.PendingPaymentStore
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.data.repository.PaymentGateway
import bo.org.siafco.app.data.repository.PaymentRepositoryResult
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.NormalizedPaymentPayload
import bo.org.siafco.app.domain.PaymentForm
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.payment.PaymentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadRequestGetsRequestCodeFromEndpoint() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.loadRequest()
        advanceUntilIdle()

        assertEquals("SOL-1", viewModel.state.value.request?.requestCode)
        assertEquals("pending_payment", viewModel.state.value.request?.status)
    }

    @Test
    fun networkErrorKeepsSessionAndStoresEncryptedDraftIntent() = runTest(dispatcher) {
        val payment = FakePaymentGateway(mutableListOf(PaymentRepositoryResult.NetworkError))
        val auth = FakeAuthGateway()
        val store = FakePendingStore()
        val viewModel = viewModel(payment = payment, auth = auth, store = store)
        fillValidForm(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(UiMessage.Network, viewModel.state.value.message)
        assertTrue(viewModel.state.value.networkRetryAvailable)
        assertFalse(viewModel.state.value.loggedOut)
        assertFalse(auth.clearLocalSessionCalled)
        assertEquals(1, store.saveCalls)
    }

    @Test
    fun retrySamePayloadKeepsSameIdempotencyKeyAfterNetworkError() = runTest(dispatcher) {
        val payment = FakePaymentGateway(
            mutableListOf(
                PaymentRepositoryResult.NetworkError,
                PaymentRepositoryResult.Success(requestSummary(status = "payment_submitted"))
            )
        )
        val store = FakePendingStore()
        val viewModel = viewModel(payment = payment, store = store)
        fillValidForm(viewModel)

        viewModel.submit()
        advanceUntilIdle()
        val firstKey = payment.keys.single()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(firstKey, payment.keys[1])
        assertTrue(viewModel.state.value.submitted)
        assertEquals(1, store.clearCalls)
    }

    @Test
    fun changingFieldAfterUncertainNetworkAttemptCreatesNewKey() = runTest(dispatcher) {
        val payment = FakePaymentGateway(
            mutableListOf(PaymentRepositoryResult.NetworkError, PaymentRepositoryResult.NetworkError)
        )
        val viewModel = viewModel(payment = payment)
        fillValidForm(viewModel)

        viewModel.submit()
        advanceUntilIdle()
        val firstKey = payment.keys.single()
        viewModel.updateTransactionNumber("TRX-CHANGED")
        viewModel.submit()
        advanceUntilIdle()

        assertNotEquals(firstKey, payment.keys[1])
    }

    @Test
    fun changingFileAfterUncertainNetworkAttemptCreatesNewKey() = runTest(dispatcher) {
        val payment = FakePaymentGateway(
            mutableListOf(PaymentRepositoryResult.NetworkError, PaymentRepositoryResult.NetworkError)
        )
        val viewModel = viewModel(payment = payment)
        fillValidForm(viewModel)

        viewModel.submit()
        advanceUntilIdle()
        val firstKey = payment.keys.single()
        viewModel.setReceipt(receipt(hash = "different"))
        viewModel.submit()
        advanceUntilIdle()

        assertNotEquals(firstKey, payment.keys[1])
    }

    @Test
    fun doubleTapDoesNotSendDuplicateRequest() = runTest(dispatcher) {
        val payment = FakePaymentGateway(mutableListOf(PaymentRepositoryResult.NetworkError))
        val viewModel = viewModel(payment = payment)
        fillValidForm(viewModel)

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, payment.keys.size)
    }

    @Test
    fun validationErrorsAreAssignedToFields() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(UiMessage.Validation, viewModel.state.value.message)
        assertTrue(viewModel.state.value.fieldErrors.containsKey("paid_amount"))
        assertTrue(viewModel.state.value.fieldErrors.containsKey("receipt"))
    }

    @Test
    fun unauthorizedClearsLocalSessionAndReceipt() = runTest(dispatcher) {
        val receipt = receipt()
        val auth = FakeAuthGateway()
        val viewModel = viewModel(
            payment = FakePaymentGateway(mutableListOf(PaymentRepositoryResult.Unauthorized)),
            auth = auth
        )
        fillValidForm(viewModel, receipt)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loggedOut)
        assertTrue(auth.clearLocalSessionCalled)
        assertFalse(receipt.file.exists())
    }

    @Test
    fun conflictValidationAndRateLimitAreMapped() = runTest(dispatcher) {
        val cases = listOf(
            PaymentRepositoryResult.Conflict("conflict") to "conflict",
            PaymentRepositoryResult.ValidationError(mapOf("paid_amount" to listOf("Monto invalido."))) to null,
            PaymentRepositoryResult.RateLimited("wait") to "wait"
        )
        for ((result, blocking) in cases) {
            val viewModel = viewModel(payment = FakePaymentGateway(mutableListOf(result)))
            fillValidForm(viewModel)
            viewModel.submit()
            advanceUntilIdle()
            if (blocking != null) {
                assertEquals(blocking, viewModel.state.value.blockingMessage)
            }
        }
    }

    @Test
    fun successClearsDraftAndTemporaryFile() = runTest(dispatcher) {
        val receipt = receipt()
        val store = FakePendingStore()
        val viewModel = viewModel(
            payment = FakePaymentGateway(mutableListOf(PaymentRepositoryResult.Success(requestSummary("SOL-2", "payment_submitted")))),
            store = store
        )
        fillValidForm(viewModel, receipt)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.submitted)
        assertEquals("payment_submitted", viewModel.state.value.request?.status)
        assertTrue(store.clearCalls > 0)
        assertFalse(receipt.file.exists())
        assertNull(viewModel.state.value.form.receipt)
    }

    private fun viewModel(
        payment: FakePaymentGateway = FakePaymentGateway(),
        auth: FakeAuthGateway = FakeAuthGateway(),
        store: FakePendingStore = FakePendingStore()
    ) = PaymentViewModel(payment, auth, store)

    private fun fillValidForm(viewModel: PaymentViewModel, receipt: PreparedReceipt = receipt()) {
        viewModel.updateTransactionNumber("TRX-123")
        viewModel.updatePaymentDate("2026-07-30")
        viewModel.updatePaidAmount("250,00")
        viewModel.updatePayerName("Ana Demo")
        viewModel.updateBankName("Banco")
        viewModel.setReceipt(receipt)
    }

    private class FakePaymentGateway(
        private val results: MutableList<PaymentRepositoryResult> = mutableListOf(PaymentRepositoryResult.Success(requestSummary()))
    ) : PaymentGateway {
        val keys = mutableListOf<String>()
        val payloads = mutableListOf<NormalizedPaymentPayload>()

        override suspend fun submit(
            idempotencyKey: String,
            payload: NormalizedPaymentPayload,
            receipt: PreparedReceipt
        ): PaymentRepositoryResult {
            keys.add(idempotencyKey)
            payloads.add(payload)
            return results.removeFirst()
        }
    }

    private class FakePendingStore : PendingPaymentStore {
        var saveCalls = 0
        var clearCalls = 0
        override suspend fun save(idempotencyKey: String, payloadSignature: String, form: PaymentForm) {
            saveCalls++
        }
        override suspend fun clear() {
            clearCalls++
        }
    }

    private class FakeAuthGateway : AuthGateway {
        var clearLocalSessionCalled = false
        override suspend fun login(email: String, password: String): ApiResult<SessionProfile> = error("No usado.")
        override suspend fun validateSession(): ApiResult<SessionProfile> = error("No usado.")
        override suspend fun affiliationRequest(): ApiResult<AffiliationRequestSummary> = ApiResult.Success(requestSummary())
        override suspend fun logout(): ApiResult<Unit> = ApiResult.Success(Unit)
        override suspend fun clearLocalSession() {
            clearLocalSessionCalled = true
        }
        val token: Flow<String?> = emptyFlow()
    }

    companion object {
        fun requestSummary(code: String = "SOL-1", status: String = "pending_payment") = AffiliationRequestSummary(
            requestCode = code,
            status = status,
            statusLabel = "Pendiente de pago",
            statusDescription = "Pendiente.",
            planName = "AFILIACION INICIAL",
            amountDue = 250.0,
            currency = "BOB",
            observations = null,
            paymentStatus = null,
            paymentStatusLabel = null,
            canSubmitPayment = true,
            canLogin = true,
            canViewCredential = false
        )

        fun receipt(hash: String = "abc123"): PreparedReceipt {
            val file = File.createTempFile("receipt", ".jpg").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                deleteOnExit()
            }
            return PreparedReceipt(file, "receipt.jpg", "image/jpeg", file.length(), hash, true)
        }
    }
}
