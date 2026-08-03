package bo.org.siafco.app

import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.domain.StoreQuoteItem
import bo.org.siafco.app.feature.store.StoreCheckoutUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCheckoutUiStateTest {
    @Test
    fun createOrderRequiresAValidQuote() {
        val lines = listOf(StoreCartLine("PROD-1", "VAR-1", 1))

        assertFalse(StoreCheckoutUiState(lines = lines, quote = null).canCreateOrder)
        assertFalse(StoreCheckoutUiState(lines = lines, quote = quote(), loadingQuote = true).canCreateOrder)
        assertFalse(StoreCheckoutUiState(lines = lines, quote = quote(), submitting = true).canCreateOrder)
        assertTrue(StoreCheckoutUiState(lines = lines, quote = quote()).canCreateOrder)
    }

    private fun quote() = StoreQuote(
        items = listOf(
            StoreQuoteItem(
                productPublicCode = "PROD-1",
                productName = "JOYA CONVENIO",
                variantPublicCode = "VAR-1",
                variantName = "DORADO",
                variantType = "color",
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
}
