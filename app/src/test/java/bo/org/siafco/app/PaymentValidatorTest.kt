package bo.org.siafco.app

import bo.org.siafco.app.domain.PaymentForm
import bo.org.siafco.app.domain.PaymentValidator
import bo.org.siafco.app.domain.PreparedReceipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class PaymentValidatorTest {
    @Test
    fun acceptsDotAndCommaAmountsWithTwoDecimals() {
        assertEquals("250.00", PaymentValidator.normalizeAmount("250")?.toPlainString())
        assertEquals("250.50", PaymentValidator.normalizeAmount("250.50")?.toPlainString())
        assertEquals("250.50", PaymentValidator.normalizeAmount("250,50")?.toPlainString())
    }

    @Test
    fun rejectsZeroNegativeScientificAndUnexpectedChars() {
        assertEquals(null, PaymentValidator.normalizeAmount("0"))
        assertEquals(null, PaymentValidator.normalizeAmount("-1"))
        assertEquals(null, PaymentValidator.normalizeAmount("1e3"))
        assertEquals(null, PaymentValidator.normalizeAmount("250 Bs"))
        assertEquals(null, PaymentValidator.normalizeAmount("10.999"))
    }

    @Test
    fun validatesReceiptMimeAndSize() {
        val valid = validForm(receipt = receipt(mime = "image/jpeg", size = 1024))
        assertTrue(PaymentValidator.validate(valid, today()).isValid)

        val invalidMime = PaymentValidator.validate(validForm(receipt = receipt(mime = "image/svg+xml")), today())
        assertFalse(invalidMime.isValid)
        assertNotNull(invalidMime.errors["receipt"])

        val invalidSize = PaymentValidator.validate(validForm(receipt = receipt(size = 6144L * 1024L + 1)), today())
        assertFalse(invalidSize.isValid)
        assertNotNull(invalidSize.errors["receipt"])
    }

    @Test
    fun rejectsFutureDateAndMissingReceipt() {
        val result = PaymentValidator.validate(validForm(paymentDate = "2026-08-02", receipt = null), today())

        assertFalse(result.isValid)
        assertNotNull(result.errors["payment_date"])
        assertNotNull(result.errors["receipt"])
    }

    private fun validForm(
        paymentDate: String = "2026-08-01",
        receipt: PreparedReceipt? = receipt()
    ) = PaymentForm(
        transactionNumber = "TRX-123",
        paymentDate = paymentDate,
        paidAmount = "250,00",
        payerName = "Ana Demo",
        bankName = "Banco",
        receipt = receipt
    )

    private fun receipt(mime: String = "image/jpeg", size: Long = 1024): PreparedReceipt {
        val file = File.createTempFile("receipt", ".bin").apply {
            writeBytes(ByteArray(size.toInt().coerceAtMost(4096)) { 1 })
            deleteOnExit()
        }
        return PreparedReceipt(file, "receipt.jpg", mime, size, "abc123", mime.startsWith("image/"))
    }

    private fun today() = LocalDate.parse("2026-08-01")
}
