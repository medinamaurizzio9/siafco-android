package bo.org.siafco.app

import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.canStartPaymentSubmission
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentCapabilityTest {
    @Test
    fun enablesPaymentOnlyWhenNoPaymentIsAlreadyUnderReview() {
        assertTrue(summary(status = "pending_payment", paymentStatus = null).canStartPaymentSubmission())
        assertTrue(summary(status = "rejected", paymentStatus = null).canStartPaymentSubmission())
        assertFalse(summary(status = "payment_submitted", paymentStatus = "pending").canStartPaymentSubmission())
        assertFalse(summary(status = "payment_submitted", paymentStatus = null).canStartPaymentSubmission())
        assertFalse(summary(status = "pending_payment", paymentStatus = "pending").canStartPaymentSubmission())
        assertFalse(summary(status = "pending_payment", paymentStatus = null, canSubmit = false).canStartPaymentSubmission())
    }

    private fun summary(
        status: String,
        paymentStatus: String?,
        canSubmit: Boolean = true
    ) = AffiliationRequestSummary(
        requestCode = "SOL-1",
        status = status,
        statusLabel = status,
        statusDescription = null,
        planName = "AFILIACION INICIAL",
        amountDue = 250.0,
        currency = "BOB",
        observations = null,
        paymentStatus = paymentStatus,
        paymentStatusLabel = paymentStatus,
        canSubmitPayment = canSubmit,
        canLogin = true,
        canViewCredential = false
    )
}
