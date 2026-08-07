package bo.org.siafco.app

import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreDeliveryCity
import bo.org.siafco.app.domain.StoreDeliveryDestination
import bo.org.siafco.app.domain.StoreDeliveryZone
import bo.org.siafco.app.feature.store.StoreCheckoutForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun changingDepartmentClearsCityAndZone() {
        val previous = StoreCheckoutForm(
            deliveryMethod = "shipping",
            department = "LA PAZ",
            city = "LA PAZ",
            zone = "SOPOCACHI",
            deliveryAddress = "CALLE 3"
        )

        val next = previous.copy(department = "COCHABAMBA").cleanAfter(previous)

        assertEquals("COCHABAMBA", next.department)
        assertEquals("", next.city)
        assertEquals("", next.zone)
        assertEquals("CALLE 3", next.deliveryAddress)
    }

    @Test
    fun changingCityClearsZoneOnly() {
        val previous = StoreCheckoutForm(
            deliveryMethod = "shipping",
            department = "LA PAZ",
            city = "LA PAZ",
            zone = "SOPOCACHI",
            deliveryAddress = "CALLE 3"
        )

        val next = previous.copy(city = "EL ALTO").cleanAfter(previous)

        assertEquals("LA PAZ", next.department)
        assertEquals("EL ALTO", next.city)
        assertEquals("", next.zone)
        assertEquals("CALLE 3", next.deliveryAddress)
    }

    @Test
    fun departmentRatesUseTextFallbackForCityAndZone() {
        val destinations = listOf(StoreDeliveryDestination("LA PAZ", emptyList()))
        val form = StoreCheckoutForm(deliveryMethod = "shipping", department = "LA PAZ")

        assertFalse(form.hasConfiguredCities(destinations))
        assertFalse(form.hasConfiguredZones(destinations))
    }

    @Test
    fun cityAndZoneRatesUseSelectorsWhenConfigured() {
        val destinations = listOf(
            StoreDeliveryDestination(
                department = "LA PAZ",
                cities = listOf(
                    StoreDeliveryCity(
                        city = "LA PAZ",
                        zones = listOf(StoreDeliveryZone("SOPOCACHI"))
                    )
                )
            )
        )
        val form = StoreCheckoutForm(deliveryMethod = "shipping", department = "LA PAZ", city = "LA PAZ")

        assertTrue(form.hasConfiguredCities(destinations))
        assertTrue(form.hasConfiguredZones(destinations))
        assertEquals("SOPOCACHI", form.selectedCity(destinations)?.zones?.single()?.zone)
    }

    @Test
    fun shippingQuoteRequiresDepartmentCityAndAddressButZoneRemainsOptional() {
        assertFalse(StoreCheckoutForm(deliveryMethod = "shipping").isReadyForQuote())
        assertFalse(StoreCheckoutForm(deliveryMethod = "shipping", department = "LA PAZ", city = "LA PAZ").isReadyForQuote())
        assertTrue(
            StoreCheckoutForm(
                deliveryMethod = "shipping",
                department = "LA PAZ",
                city = "LA PAZ",
                zone = "",
                deliveryAddress = "CALLE 3"
            ).isReadyForQuote()
        )
    }
}
