package bo.org.siafco.app

import bo.org.siafco.app.data.repository.StoreCatalogFilters
import bo.org.siafco.app.data.repository.StoreGateway
import bo.org.siafco.app.data.repository.StoreOrderFilters
import bo.org.siafco.app.data.repository.StoreOrderList
import bo.org.siafco.app.data.repository.StoreResult
import bo.org.siafco.app.data.store.StoreCartLogic
import bo.org.siafco.app.data.store.StoreCartStore
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.domain.StoreAvailability
import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreCatalog
import bo.org.siafco.app.domain.StoreCategory
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.domain.StoreProduct
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.domain.StoreQuoteRequestData
import bo.org.siafco.app.domain.StoreVariant
import bo.org.siafco.app.domain.StoreWhatsapp
import bo.org.siafco.app.feature.store.StoreProductDisabledReason
import bo.org.siafco.app.feature.store.StoreProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreProductViewModelTest {
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
    fun availableProductWithoutVariantsCanBeAddedAndUpdatesCartCounter() = runTest(dispatcher) {
        val cart = FakeStoreCartStore()
        val viewModel = StoreProductViewModel(FakeStoreGateway(product = product()), cart)

        viewModel.load("PROD-1")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canAddToCart)
        assertNull(viewModel.state.value.disabledReason)

        viewModel.addToCart()
        viewModel.addToCart()
        advanceUntilIdle()

        assertEquals(listOf(StoreCartLine("PROD-1", null, 1)), cart.current)
        assertTrue(viewModel.state.value.added)
        assertEquals(1, viewModel.state.value.cartCount)
    }

    @Test
    fun productWithVariantsRequiresSelectionAndThenCanBeAdded() = runTest(dispatcher) {
        val cart = FakeStoreCartStore()
        val product = product(variants = listOf(StoreVariant("VAR-1", "M", "talla", "0.00", "250.00")))
        val viewModel = StoreProductViewModel(FakeStoreGateway(product = product), cart)

        viewModel.load("PROD-1")
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canAddToCart)
        assertEquals(StoreProductDisabledReason.SelectVariant, viewModel.state.value.disabledReason)

        viewModel.selectVariant("VAR-1")
        viewModel.addToCart()
        advanceUntilIdle()

        assertEquals(listOf(StoreCartLine("PROD-1", "VAR-1", 1)), cart.current)
    }

    @Test
    fun unavailableStatusesExplainWhyProductCannotBeAdded() = runTest(dispatcher) {
        val soldOut = StoreProductViewModel(FakeStoreGateway(product(StoreAvailability.SoldOut)), FakeStoreCartStore())
        soldOut.load("PROD-1")
        advanceUntilIdle()
        assertFalse(soldOut.state.value.canAddToCart)
        assertEquals(StoreProductDisabledReason.SoldOut, soldOut.state.value.disabledReason)

        val comingSoon = StoreProductViewModel(FakeStoreGateway(product(StoreAvailability.ComingSoon)), FakeStoreCartStore())
        comingSoon.load("PROD-1")
        advanceUntilIdle()
        assertFalse(comingSoon.state.value.canAddToCart)
        assertEquals(StoreProductDisabledReason.ComingSoon, comingSoon.state.value.disabledReason)
    }

    @Test
    fun quantityIsClampedToProductMaximum() = runTest(dispatcher) {
        val viewModel = StoreProductViewModel(FakeStoreGateway(product(maxQuantity = 2)), FakeStoreCartStore())

        viewModel.load("PROD-1")
        advanceUntilIdle()
        viewModel.setQuantity(5)

        assertEquals(2, viewModel.state.value.quantity)
        assertTrue(viewModel.state.value.canAddToCart)
        assertEquals(StoreProductDisabledReason.MaxQuantityReached, viewModel.state.value.disabledReason)
    }

    private fun product(
        availabilityStatus: String = StoreAvailability.Available,
        canOrder: Boolean = availabilityStatus == StoreAvailability.Available,
        maxQuantity: Int = 3,
        variants: List<StoreVariant> = emptyList()
    ) = StoreProduct(
        publicCode = "PROD-1",
        slug = "producto",
        sku = "SKU-1",
        name = "Producto",
        shortDescription = null,
        description = null,
        regularPrice = "250.00",
        affiliatePrice = "250.00",
        effectivePrice = "250.00",
        promoPrice = null,
        currency = "BOB",
        availabilityStatus = availabilityStatus,
        deliveryModes = listOf("pickup"),
        featured = false,
        maxQuantityPerOrder = maxQuantity,
        primaryImageUrl = null,
        category = StoreCategory("categoria", "Categoria"),
        canOrder = canOrder,
        images = emptyList(),
        variants = variants
    )

    private class FakeStoreGateway(private val product: StoreProduct) : StoreGateway {
        override suspend fun catalog(filters: StoreCatalogFilters): StoreResult<StoreCatalog> = error("Not used")
        override suspend fun product(publicCode: String): StoreResult<StoreProduct> = StoreResult.Success(product)
        override suspend fun deliveryDestinations(): StoreResult<List<bo.org.siafco.app.domain.StoreDeliveryDestination>> = StoreResult.Success(emptyList())
        override suspend fun quote(request: StoreQuoteRequestData): StoreResult<StoreQuote> = error("Not used")
        override suspend fun createOrder(idempotencyKey: String, request: StoreQuoteRequestData): StoreResult<StoreOrder> = error("Not used")
        override suspend fun orders(filters: StoreOrderFilters): StoreResult<StoreOrderList> = error("Not used")
        override suspend fun order(code: String): StoreResult<StoreOrder> = error("Not used")
        override suspend fun submitReceipt(orderCode: String, idempotencyKey: String, receipt: PreparedReceipt): StoreResult<StoreOrder> = error("Not used")
        override suspend fun whatsapp(orderCode: String): StoreResult<StoreWhatsapp> = error("Not used")
    }

    private class FakeStoreCartStore : StoreCartStore {
        private val state = MutableStateFlow<List<StoreCartLine>>(emptyList())
        override val lines = state
        val current: List<StoreCartLine> get() = state.value

        override suspend fun add(line: StoreCartLine) {
            state.value = StoreCartLogic.add(state.value, line)
        }

        override suspend fun updateQuantity(productPublicCode: String, variantPublicCode: String?, quantity: Int) = Unit
        override suspend fun remove(productPublicCode: String, variantPublicCode: String?) = Unit
        override suspend fun clear() {
            state.value = emptyList()
        }
    }
}
