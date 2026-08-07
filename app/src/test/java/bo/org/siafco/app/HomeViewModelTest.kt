package bo.org.siafco.app

import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.repository.AuthGateway
import bo.org.siafco.app.data.repository.StoreCatalogFilters
import bo.org.siafco.app.data.repository.StoreGateway
import bo.org.siafco.app.data.repository.StoreOrderFilters
import bo.org.siafco.app.data.repository.StoreOrderList
import bo.org.siafco.app.data.repository.StoreResult
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreCatalog
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.domain.StoreOrderCapabilities
import bo.org.siafco.app.domain.StorePagination
import bo.org.siafco.app.domain.StoreProduct
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.domain.StoreQuoteRequestData
import bo.org.siafco.app.domain.StoreWhatsapp
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
import org.junit.Assert.assertNull
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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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

        val visible = viewModel(visibleGateway)
        visible.load()
        advanceUntilIdle()
        val hidden = viewModel(hiddenGateway)
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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

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
        val viewModel = viewModel(gateway)

        viewModel.load()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.profile)
        assertEquals(UiMessage.Network, viewModel.state.value.requestMessage)
        assertFalse(gateway.clearLocalSessionCalled)
        assertFalse(viewModel.state.value.loggedOut)
    }

    @Test
    fun attentionOrdersShowOnlyUnpaidAndReviewStatusesLimitedToThree() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary(canViewCredential = true)))
        )
        val store = FakeStoreGateway(
            ordersResult = StoreResult.Success(
                StoreOrderList(
                    orders = listOf(
                        order("PED-1", "pendiente"),
                        order("PED-2", "reservado"),
                        order("PED-3", "esperando_pago"),
                        order("PED-4", "pago_en_revision"),
                        order("PED-5", "confirmado"),
                        order("PED-6", "cancelado"),
                        order("PED-7", "enviado"),
                        order("PED-8", "entregado")
                    ),
                    pagination = pagination()
                )
            )
        )
        val viewModel = viewModel(gateway, store)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, store.ordersCalls)
        assertEquals(true, store.lastFilters?.attentionOnly)
        assertEquals(3, store.lastFilters?.perPage)
        assertNull(store.lastFilters?.status)
        assertEquals(listOf("PED-1", "PED-2", "PED-3"), viewModel.state.value.attentionOrders.map { it.code })
        assertFalse(viewModel.state.value.attentionOrders.any { it.status in setOf("confirmado", "cancelado", "enviado", "entregado") })
    }

    @Test
    fun attentionSectionDisappearsWhenThereAreNoPendingOrders() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary()))
        )
        val store = FakeStoreGateway(
            ordersResult = StoreResult.Success(StoreOrderList(listOf(order("PED-1", "entregado")), pagination()))
        )
        val viewModel = viewModel(gateway, store)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.ordersLoaded)
        assertTrue(viewModel.state.value.attentionOrders.isEmpty())
    }

    @Test
    fun orderNetworkErrorKeepsProfileAndServices() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary()))
        )
        val viewModel = viewModel(gateway, FakeStoreGateway(StoreResult.NetworkError))

        viewModel.load()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.profile)
        assertTrue(viewModel.state.value.capabilities.canViewProfile)
        assertEquals(UiMessage.Network, viewModel.state.value.ordersMessage)
        assertFalse(viewModel.state.value.loggedOut)
    }

    @Test
    fun loadDoesNotDuplicateOrderRequestsByRecomposition() = runTest(dispatcher) {
        val gateway = FakeAuthGateway(
            profileResult = ApiResult.Success(activeProfile()),
            requestResults = mutableListOf(ApiResult.Success(requestSummary()))
        )
        val store = FakeStoreGateway(StoreResult.Success(StoreOrderList(listOf(order("PED-1", "pago_en_revision")), pagination())))
        val viewModel = viewModel(gateway, store)

        viewModel.load()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, store.ordersCalls)
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

    private class FakeStoreGateway(
        private val ordersResult: StoreResult<StoreOrderList> = StoreResult.Success(StoreOrderList(emptyList(), pagination()))
    ) : StoreGateway {
        var ordersCalls = 0
        var lastFilters: StoreOrderFilters? = null

        override suspend fun catalog(filters: StoreCatalogFilters): StoreResult<StoreCatalog> = error("No se usa en estas pruebas.")
        override suspend fun product(publicCode: String): StoreResult<StoreProduct> = error("No se usa en estas pruebas.")
        override suspend fun deliveryDestinations(): StoreResult<List<bo.org.siafco.app.domain.StoreDeliveryDestination>> = StoreResult.Success(emptyList())
        override suspend fun quote(request: StoreQuoteRequestData): StoreResult<StoreQuote> = error("No se usa en estas pruebas.")
        override suspend fun createOrder(idempotencyKey: String, request: StoreQuoteRequestData): StoreResult<StoreOrder> = error("No se usa en estas pruebas.")
        override suspend fun orders(filters: StoreOrderFilters): StoreResult<StoreOrderList> {
            ordersCalls++
            lastFilters = filters
            return ordersResult
        }
        override suspend fun order(code: String): StoreResult<StoreOrder> = error("No se usa en estas pruebas.")
        override suspend fun submitReceipt(orderCode: String, idempotencyKey: String, receipt: PreparedReceipt): StoreResult<StoreOrder> = error("No se usa en estas pruebas.")
        override suspend fun whatsapp(orderCode: String): StoreResult<StoreWhatsapp> = error("No se usa en estas pruebas.")
    }

    private fun viewModel(gateway: FakeAuthGateway, store: FakeStoreGateway = FakeStoreGateway()) = HomeViewModel(gateway, store)

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
        allowedProfileFields = allowedProfileFields,
        photoUrl = "http://127.0.0.1:8000/storage/affiliates/photos/demo.jpg?v=123",
        registrationNumber = "AF-001"
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

    private fun order(code: String, status: String) = StoreOrder(
        code = code,
        date = "03/08/2026",
        status = status,
        statusLabel = status,
        createdAt = "2026-08-03T10:00:00Z",
        updatedAt = "2026-08-03T10:00:00Z",
        total = "250.00",
        currency = "BOB",
        deliveryMethod = "pickup",
        itemSummary = "1 producto",
        capabilities = StoreOrderCapabilities(
            canUploadReceipt = status in setOf("pendiente", "reservado", "esperando_pago"),
            canOpenWhatsapp = false,
            canCancel = false,
            canViewReceipt = false
        ),
        delivery = null,
        items = emptyList(),
        subtotal = "250.00",
        discountTotal = "0.00",
        shippingTotal = "0.00",
        payment = null,
        receipts = emptyList(),
        statusHistory = emptyList()
    )

    private companion object {
        fun pagination() = StorePagination(currentPage = 1, perPage = 15, lastPage = 1, total = 0)
    }
}
