package bo.org.siafco.app

import bo.org.siafco.app.feature.payment.PaymentSupportWhatsapp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentSupportWhatsappTest {
    @Test
    fun normalizesBolivianSupportNumbersWithoutDuplicatingCountryCode() {
        assertEquals("59170000000", PaymentSupportWhatsapp.normalizedBolivianNumber("70000000"))
        assertEquals("59170000000", PaymentSupportWhatsapp.normalizedBolivianNumber("+59170000000"))
        assertEquals("59170000000", PaymentSupportWhatsapp.normalizedBolivianNumber("59170000000"))
        assertEquals("59170000000", PaymentSupportWhatsapp.normalizedBolivianNumber("7000-0000"))
    }

    @Test
    fun rejectsMissingOrUnsupportedPhoneNumbersCleanly() {
        assertNull(PaymentSupportWhatsapp.normalizedBolivianNumber(null))
        assertNull(PaymentSupportWhatsapp.normalizedBolivianNumber(""))
        assertNull(PaymentSupportWhatsapp.normalizedBolivianNumber("123"))
        assertNull(PaymentSupportWhatsapp.url(null, "SOL-1"))
    }

    @Test
    fun buildsWaMeUrlWithPrefilledPaymentMessage() {
        val url = PaymentSupportWhatsapp.url("70000000", "SOL-20260807-POBMML")

        assertTrue(url!!.startsWith("https://wa.me/59170000000?text="))
        assertTrue(url.contains("SOL-20260807-POBMML"))
        assertTrue(url.contains("Pago+en+revisi"))
    }
}
