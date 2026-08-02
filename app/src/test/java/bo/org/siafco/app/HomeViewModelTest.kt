package bo.org.siafco.app

import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.home.HomeViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
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
    fun pendingUserLoadsAffiliationRequestAfterReopen() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(pendingProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary()))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, gateway.validateCalls)
        assertEquals(1, gateway.requestCalls)
        assertEquals("SOL-1", viewModel.state.value.affiliationRequest?.requestCode)
        assertEquals("pending_payment", viewModel.state.value.affiliationRequest?.status)
    }

    @Test
    fun activeAffiliateEnablesProfileWithoutDependingOnPaymentStatus() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary(paymentStatus = "confirmed", canSubmitPayment = false, canViewCredential = true)))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, gateway.requestCalls)
        assertTrue(viewModel.state.value.capabilities.canViewProfile)
        assertTrue(viewModel.state.value.capabilities.canEditProfile)
        assertTrue(viewModel.state.value.capabilities.canViewAffiliationRequest)
        assertFalse(viewModel.state.value.capabilities.canSubmitPayment)
        assertTrue(viewModel.state.value.capabilities.canViewCredential)
    }

    @Test
    fun credentialCapabilityFollowsBackendFlag() = runTest(dispatcher) {
        val visibleGateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary(canViewCredential = true)))
        )
        val hiddenGateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary(canViewCredential = false)))
        )

        val visible = HomeViewModel(visibleGateway)
        visible.load()
        advanceUntilIdle()
        val hidden = HomeViewModel(hiddenGateway)
        hidden.load()
        advanceUntilIdle()

        assertTrue(visible.state.value.capabilities.canViewCredential)
        assertFalse(hidden.state.value.capabilities.canViewCredential)
    }

    @Test
    fun limitedPendingAffiliateCanOpenProfileWhenContractAllowsMobileAccess() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(pendingProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary()))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.capabilities.canViewProfile)
        assertTrue(viewModel.state.value.capabilities.canEditProfile)
    }

    @Test
    fun emptyAllowedProfileFieldsKeepsProfileViewButDisablesEditingCapability() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile(allowedProfileFields = emptySet())),
            requestResults = mutableListOf(ApiResult.Success(requestSummary()))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.capabilities.canViewProfile)
        assertFalse(viewModel.state.value.capabilities.canEditProfile)
    }

    @Test
    fun profileViewDoesNotDependOnDerivedAffiliateFlagDefault() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(
                SessionProfile(
                    name = "Ana Movil Demo",
                    email = "ana@example.test",
                    affiliateStatus = "activo",
                    affiliateStatusLabel = "Afiliado activo",
                    accessLevel = AccessLevel.Active,
                    allowedProfileFields = setOf("phone", "email")
                )
            ),
            requestResults = mutableListOf(ApiResult.Success(requestSummary(canSubmitPayment = false)))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.capabilities.canViewProfile)
        assertTrue(viewModel.state.value.capabilities.canEditProfile)
    }

    @Test
    fun failedMeKeepsProfileButtonDisabled() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.HttpError(500, null),
            requestResults = mutableListOf()
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.capabilities.canViewProfile)
        assertFalse(viewModel.state.value.capabilities.canEditProfile)
    }

    @Test
    fun loadIsNotDuplicatedByRecomposition() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(pendingProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary()))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, gateway.validateCalls)
        assertEquals(1, gateway.requestCalls)
    }

    @Test
    fun manualRefreshLoadsAffiliationRequestAgain() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(pendingProfile()),
            requestResults = mutableListOf(
                ApiResult.Success(requestSummary("SOL-1")),
                ApiResult.Success(requestSummary("SOL-2"))
            )
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()
        viewModel.refreshAffiliationRequest()
        advanceUntilIdle()

        assertEquals(2, gateway.requestCalls)
        assertEquals("SOL-2", viewModel.state.value.affiliationRequest?.requestCode)
    }

    @Test
    fun notFoundKeepsProfileAndShowsRequestMessage() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(pendingProfile()),
            requestResults = mutableListOf(ApiResult.HttpError(404, null))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.profile)
        assertEquals(UiMessage.RequestNotFound, viewModel.state.value.requestMessage)
        assertFalse(viewModel.state.value.loggedOut)
    }

    @Test
    fun unauthorizedRequestLogsOut() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(pendingProfile()),
            requestResults = mutableListOf(ApiResult.HttpError(401, null))
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loggedOut)
    }

    @Test
    fun networkErrorKeepsProfileAndDoesNotClearSession() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(pendingProfile()),
            requestResults = mutableListOf(ApiResult.NetworkError)
        )
        val viewModel = HomeViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.profile)
        assertEquals(UiMessage.Network, viewModel.state.value.requestMessage)
        assertFalse(gateway.clearLocalSessionCalled)
        assertFalse(viewModel.state.value.loggedOut)
    }

    private class FakeAuthGateway(
        private val profileResult: ApiResult<SessionProfile>,
        private val requestResults: MutableList<ApiResult<AffiliationRequestSummary>>
    ) : AuthGateway {
        var validateCalls = 0
        var requestCalls = 0
        var clearLocalSessionCalled = false

        val token: Flow<String?> = emptyFlow()

        override suspend fun login(email: String, password: String): ApiResult<SessionProfile> {
            error("No se usa en estas pruebas.")
        }

        override suspend fun validateSession(): ApiResult<SessionProfile> {
            validateCalls++
            return profileResult
        }

        override suspend fun affiliationRequest(): ApiResult<AffiliationRequestSummary> {
            requestCalls++
            return requestResults.removeFirst()
        }

        override suspend fun logout(): ApiResult<Unit> = ApiResult.Success(Unit)

        override suspend fun clearLocalSession() {
            clearLocalSessionCalled = true
        }
    }

    private fun pendingProfile() = SessionProfile(
        name = "Ana Movil Demo",
        email = "ana@example.test",
        affiliateStatus = "pendiente_pago",
        affiliateStatusLabel = "Pendiente de pago",
        accessLevel = AccessLevel.Pending,
        hasAffiliateProfile = true,
        allowedProfileFields = setOf("phone", "email", "address", "birth_date", "marital_status")
    )

    private fun activeProfile(allowedProfileFields: Set<String> = setOf("phone", "email")) = SessionProfile(
        name = "Ana Movil Demo",
        email = "ana@example.test",
        affiliateStatus = "activo",
        affiliateStatusLabel = "Afiliado activo",
        accessLevel = AccessLevel.Active,
        hasAffiliateProfile = true,
        allowedProfileFields = allowedProfileFields
    )

    private fun requestSummary(
        code: String = "SOL-1",
        paymentStatus: String? = null,
        canSubmitPayment: Boolean = true,
        canViewCredential: Boolean = false
    ) = AffiliationRequestSummary(
        requestCode = code,
        status = "pending_payment",
        statusLabel = "Pendiente de pago",
        statusDescription = "Pendiente de pago.",
        planName = "AFILIACION INICIAL",
        amountDue = 250.0,
        currency = "BOB",
        observations = null,
        paymentStatus = paymentStatus,
        paymentStatusLabel = null,
        canSubmitPayment = canSubmitPayment,
        canLogin = true,
        canViewCredential = canViewCredential
    )
}
