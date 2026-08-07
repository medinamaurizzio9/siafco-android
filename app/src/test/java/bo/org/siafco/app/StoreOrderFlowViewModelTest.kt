package bo.org.siafco.app

import bo.org.siafco.app.data.repository.StoreCatalogFilters
import bo.org.siafco.app.data.repository.StoreGateway
import bo.org.siafco.app.data.repository.StoreOrderFilters
import bo.org.siafco.app.data.repository.StoreOrderList
import bo.org.siafco.app.data.repository.StoreResult
import bo.org.siafco.app.data.store.StoreCartStore
import bo.org.siafco.app.data.store.StorePendingOrderStore
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreCatalog
import bo.org.siafco.app.domain.StoreDeliveryDestination
import bo.org.siafco.app.domain.StoreDelivery
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.domain.StoreOrderCapabilities
import bo.org.siafco.app.domain.StoreOrderPayment
import bo.org.siafco.app.domain.StorePagination
import bo.org.siafco.app.domain.StoreProduct
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.domain.StoreQuoteItem
import bo.org.siafco.app.domain.StoreReceipt
import bo.org.siafco.app.domain.StoreSettings
import bo.org.siafco.app.domain.StoreWhatsapp
import bo.org.siafco.app.feature.store.StoreCheckoutForm
import bo.org.siafco.app.feature.store.StoreCheckoutViewModel
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.store.StoreReceiptViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreOrderFlowViewModelTest {
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
    fun checkoutDebouncesFormQuotesAndAvoidsDuplicateRequests() = runTest(dispatcher) {
        val gateway = FakeStoreGateway()
        val viewModel = StoreCheckoutViewModel(gateway, FakeCartStore(), FakePendingOrderStore())
        advanceUntilIdle()

        assertEquals(1, gateway.quoteRequests)

        viewModel.updateForm { copy(couponCode = "A") }
        viewModel.updateForm { copy(couponCode = "AB") }
        viewModel.updateForm { copy(couponCode = "ABC") }
        advanceTimeBy(449)
        assertEquals(1, gateway.quoteRequests)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(2, gateway.quoteRequests)
    }

    @Test
    fun checkoutDoesNotQuoteWhenDeliverySelectionIsUnchanged() = runTest(dispatcher) {
        val gateway = FakeStoreGateway()
        val viewModel = StoreCheckoutViewModel(gateway, FakeCartStore(), FakePendingOrderStore())
        advanceUntilIdle()

        assertEquals(1, gateway.quoteRequests)

        viewModel.updateForm { copy(deliveryMethod = "pickup") }
        advanceTimeBy(500)
        advanceUntilIdle()

        assertEquals(1, gateway.quoteRequests)
    }

    @Test
    fun checkoutLoadsDeliveryDestinationsAndQuotesOnlyWhenShippingDataIsComplete() = runTest(dispatcher) {
        val gateway = FakeStoreGateway(
            destinations = listOf(
                StoreDeliveryDestination("LA PAZ", emptyList())
            )
        )
        val viewModel = StoreCheckoutViewModel(gateway, FakeCartStore(), FakePendingOrderStore())
        advanceUntilIdle()

        assertEquals(listOf("LA PAZ"), viewModel.state.value.deliveryDestinations.map { it.department })
        assertEquals(1, gateway.quoteRequests)

        viewModel.updateForm { copy(deliveryMethod = "shipping") }
        advanceTimeBy(500)
        advanceUntilIdle()
        assertEquals(1, gateway.quoteRequests)

        viewModel.updateForm { copy(department = "LA PAZ") }
        viewModel.updateForm { copy(city = "LA PAZ") }
        advanceTimeBy(500)
        advanceUntilIdle()
        assertEquals(1, gateway.quoteRequests)

        viewModel.updateForm { copy(deliveryAddress = "CALLE 3") }
        advanceTimeBy(450)
        advanceUntilIdle()

        assertEquals(2, gateway.quoteRequests)
    }

    @Test
    fun checkoutClearsDependentDestinationFieldsAndInvalidatesPreviousQuote() = runTest(dispatcher) {
        val gateway = FakeStoreGateway()
        val viewModel = StoreCheckoutViewModel(gateway, FakeCartStore(), FakePendingOrderStore())
        advanceUntilIdle()
        assertTrue(viewModel.state.value.quote != null)

        viewModel.updateForm { copy(deliveryMethod = "shipping") }
        viewModel.updateForm { copy(department = "LA PAZ") }
        viewModel.updateForm { copy(city = "LA PAZ") }
        viewModel.updateForm { copy(zone = "SOPOCACHI") }
        viewModel.updateForm { copy(deliveryAddress = "CALLE 3") }
        advanceTimeBy(450)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.quote != null)

        viewModel.updateForm { copy(department = "COCHABAMBA") }
        advanceUntilIdle()

        assertEquals("COCHABAMBA", viewModel.state.value.form.department)
        assertEquals("", viewModel.state.value.form.city)
        assertEquals("", viewModel.state.value.form.zone)
        assertEquals(null, viewModel.state.value.quote)
    }

    @Test
    fun successfulQuoteClearsPreviousRateLimitMessage() = runTest(dispatcher) {
        val gateway = FakeStoreGateway()
        gateway.quoteResults.add(StoreResult.RateLimited("wait"))
        gateway.quoteResults.add(StoreResult.Success(sampleQuote()))
        val viewModel = StoreCheckoutViewModel(gateway, FakeCartStore(), FakePendingOrderStore())
        advanceUntilIdle()

        assertEquals(UiMessage.RateLimited, viewModel.state.value.message)

        viewModel.updateForm { copy(couponCode = "OK") }
        advanceTimeBy(450)
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.message)
        assertEquals(2, gateway.quoteRequests)
    }

    @Test
    fun checkoutSubmitBlocksSecondTapBeforeNetworkRuns() = runTest(dispatcher) {
        val gateway = FakeStoreGateway()
        val viewModel = StoreCheckoutViewModel(gateway, FakeCartStore(), FakePendingOrderStore())
        advanceUntilIdle()

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, gateway.createOrderRequests)
    }

    @Test
    fun receiptSubmitBlocksSecondTapAndKeepsPaymentReviewOrder() = runTest(dispatcher) {
        val gateway = FakeStoreGateway()
        val viewModel = StoreReceiptViewModel(gateway)
        val receipt = receipt()

        viewModel.setReceipt(receipt)
        viewModel.submit("PED-1")
        viewModel.submit("PED-1")
        advanceUntilIdle()

        assertEquals(1, gateway.receiptRequests)
        assertFalse(receipt.file.exists())
        assertEquals("pago_en_revision", viewModel.state.value.submittedOrder?.status)
        assertFalse(viewModel.state.value.submittedOrder?.capabilities?.canUploadReceipt ?: true)
    }

    private inner class FakeStoreGateway(
        private val destinations: List<StoreDeliveryDestination> = emptyList()
    ) : StoreGateway {
        var quoteRequests = 0
        var createOrderRequests = 0
        var receiptRequests = 0
        val quoteResults = ArrayDeque<StoreResult<StoreQuote>>()

        override suspend fun catalog(filters: StoreCatalogFilters): StoreResult<StoreCatalog> = StoreResult.NetworkError
        override suspend fun product(publicCode: String): StoreResult<StoreProduct> = StoreResult.NetworkError
        override suspend fun deliveryDestinations(): StoreResult<List<StoreDeliveryDestination>> = StoreResult.Success(destinations)
        override suspend fun quote(request: bo.org.siafco.app.domain.StoreQuoteRequestData): StoreResult<StoreQuote> {
            quoteRequests += 1
            return quoteResults.removeFirstOrNull() ?: StoreResult.Success(sampleQuote())
        }

        override suspend fun createOrder(idempotencyKey: String, request: bo.org.siafco.app.domain.StoreQuoteRequestData): StoreResult<StoreOrder> {
            createOrderRequests += 1
            return StoreResult.Success(sampleOrder("pendiente", canUploadReceipt = true))
        }

        override suspend fun orders(filters: StoreOrderFilters): StoreResult<StoreOrderList> =
            StoreResult.Success(StoreOrderList(emptyList(), StorePagination(1, 15, 1, 0)))

        override suspend fun order(code: String): StoreResult<StoreOrder> = StoreResult.Success(sampleOrder("pago_en_revision", canUploadReceipt = false))

        override suspend fun submitReceipt(orderCode: String, idempotencyKey: String, receipt: PreparedReceipt): StoreResult<StoreOrder> {
            receiptRequests += 1
            return StoreResult.Success(sampleOrder("pago_en_revision", canUploadReceipt = false))
        }

        override suspend fun whatsapp(orderCode: String): StoreResult<StoreWhatsapp> = StoreResult.NetworkError
    }

    private class FakeCartStore : StoreCartStore {
        override val lines = MutableStateFlow(listOf(StoreCartLine("PROD-1", null, 1)))
        override suspend fun add(line: StoreCartLine) = Unit
        override suspend fun updateQuantity(productPublicCode: String, variantPublicCode: String?, quantity: Int) = Unit
        override suspend fun remove(productPublicCode: String, variantPublicCode: String?) = Unit
        override suspend fun clear() {
            lines.value = emptyList()
        }
    }

    private class FakePendingOrderStore : StorePendingOrderStore {
        override suspend fun keyFor(payloadSignature: String): String = "idem-1"
        override suspend fun clear() = Unit
    }

    private fun sampleQuote() = StoreQuote(
        items = listOf(
            StoreQuoteItem(
                productPublicCode = "PROD-1",
                productName = "JOYA CONVENIO",
                variantPublicCode = null,
                variantName = null,
                variantType = null,
                quantity = 1,
                unitPrice = "250.00",
                lineTotal = "250.00",
                priceReason = "affiliate"
            )
        ),
        subtotal = "250.00",
        discountTotal = "0.00",
        shippingTotal = "0.00",
        total = "250.00",
        currency = "BOB",
        coupon = null,
        shipping = null,
        expiresAt = null
    )

    private fun sampleOrder(status: String, canUploadReceipt: Boolean) = StoreOrder(
        code = "PED-1",
        date = null,
        status = status,
        statusLabel = if (status == "pago_en_revision") "Pago en revisión" else "Pendiente",
        createdAt = null,
        updatedAt = null,
        total = "250.00",
        currency = "BOB",
        deliveryMethod = "pickup",
        itemSummary = "1 x JOYA CONVENIO",
        capabilities = StoreOrderCapabilities(
            canUploadReceipt = canUploadReceipt,
            canOpenWhatsapp = true,
            canCancel = false,
            canViewReceipt = !canUploadReceipt
        ),
        delivery = StoreDelivery("pickup", null, null, null, null),
        items = emptyList(),
        subtotal = "250.00",
        discountTotal = "0.00",
        shippingTotal = "0.00",
        payment = StoreOrderPayment("pending", "Pendiente"),
        receipts = if (canUploadReceipt) emptyList() else listOf(StoreReceipt("REC-1", "pending", null, null, null, "image/jpeg", 100)),
        statusHistory = emptyList()
    )

    private fun receipt(): PreparedReceipt {
        val file = File.createTempFile("store-receipt", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        return PreparedReceipt(file, "receipt.jpg", "image/jpeg", file.length(), "abc123", true)
    }
}
