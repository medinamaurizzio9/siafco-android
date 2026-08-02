package bo.org.siafco.app

import bo.org.siafco.app.data.store.StoreCartLogic
import bo.org.siafco.app.data.store.StoreCartSerializer
import bo.org.siafco.app.domain.StoreCartLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCartLogicTest {
    @Test
    fun addConsolidatesSameProductAndVariantWithoutPrices() {
        val first = StoreCartLine("PROD-1", "VAR-1", 1)
        val second = StoreCartLine("PROD-1", "VAR-1", 2)

        val result = StoreCartLogic.add(listOf(first), second)

        assertEquals(1, result.size)
        assertEquals(3, result.first().quantity)
        assertEquals("PROD-1", result.first().productPublicCode)
        assertEquals("VAR-1", result.first().variantPublicCode)
    }

    @Test
    fun differentVariantCreatesIndependentLine() {
        val result = StoreCartLogic.add(
            listOf(StoreCartLine("PROD-1", "VAR-1", 1)),
            StoreCartLine("PROD-1", "VAR-2", 1)
        )

        assertEquals(2, result.size)
    }

    @Test
    fun quantityIsLimitedAndZeroRemovesLine() {
        val line = StoreCartLine("PROD-1", null, 1)
        val updated = StoreCartLogic.updateQuantity(listOf(line), "PROD-1", null, 150)

        assertEquals(99, updated.first().quantity)
        assertTrue(StoreCartLogic.updateQuantity(updated, "PROD-1", null, 0).isEmpty())
    }

    @Test
    fun serializedCartContainsOnlyPublicCodesVariantAndQuantity() {
        val raw = StoreCartSerializer.encode(listOf(StoreCartLine("PROD-1", "VAR-1", 2)))

        assertTrue(raw.contains("productPublicCode"))
        assertTrue(raw.contains("variantPublicCode"))
        assertTrue(raw.contains("quantity"))
        assertTrue(!raw.contains("price", ignoreCase = true))
        assertTrue(!raw.contains("coupon", ignoreCase = true))
        assertTrue(!raw.contains("address", ignoreCase = true))
        assertTrue(!raw.contains("total", ignoreCase = true))
        assertEquals(listOf(StoreCartLine("PROD-1", "VAR-1", 2)), StoreCartSerializer.decode(raw))
    }
}
