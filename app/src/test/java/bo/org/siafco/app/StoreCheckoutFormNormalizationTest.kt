package bo.org.siafco.app

import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.feature.store.StoreCheckoutForm
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreCheckoutFormNormalizationTest {
    @Test
    fun checkoutDeliveryHumanFieldsAreSubmittedUppercase() {
        val request = StoreCheckoutForm(
            deliveryMethod = "shipping",
            department = " la paz ",
            city = "el alto",
            zone = "zona 16 de julio",
            deliveryAddress = " avenida ñuñoa 123 ",
            couponCode = " verano "
        ).toRequest(listOf(StoreCartLine("PROD-1", null, 1)))

        assertEquals("LA PAZ", request.department)
        assertEquals("EL ALTO", request.city)
        assertEquals("ZONA 16 DE JULIO", request.zone)
        assertEquals("AVENIDA ÑUÑOA 123", request.deliveryAddress)
        assertEquals("VERANO", request.couponCode)
    }

    @Test
    fun checkoutDoesNotAlterCartTechnicalCodesOrQuantity() {
        val request = StoreCheckoutForm(department = "cochabamba").toRequest(
            listOf(StoreCartLine("prod-abc-123", "var-def-456", 2))
        )

        assertEquals("prod-abc-123", request.lines.single().productPublicCode)
        assertEquals("var-def-456", request.lines.single().variantPublicCode)
        assertEquals(2, request.lines.single().quantity)
    }
}
