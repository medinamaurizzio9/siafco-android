package bo.org.siafco.app.domain

data class AffiliationRequestSummary(
    val requestCode: String,
    val status: String,
    val statusLabel: String,
    val statusDescription: String?,
    val planName: String?,
    val planType: String? = null,
    val planPaymentInstructions: String? = null,
    val amountDue: Double?,
    val currency: String?,
    val observations: String?,
    val paymentStatus: String?,
    val paymentStatusLabel: String?,
    val transactionNumber: String? = null,
    val paymentDate: String? = null,
    val paidAmount: Double? = null,
    val submittedAt: String? = null,
    val rejectionReason: String? = null,
    val hasReceipt: Boolean = false,
    val paymentBank: String? = null,
    val paymentHolder: String? = null,
    val paymentAccount: String? = null,
    val paymentInstructions: String? = null,
    val canSubmitPayment: Boolean,
    val canLogin: Boolean,
    val canViewCredential: Boolean
)

fun AffiliationRequestSummary.canStartPaymentSubmission(): Boolean {
    if (!canSubmitPayment) return false
    if (status == "payment_submitted" || paymentStatus == "pending") return false
    return status == "pending_payment" || status == "rejected"
}

fun AffiliationRequestSummary.paymentDisabledReason(): String? {
    if (canStartPaymentSubmission()) return null
    return when (status) {
        "payment_approved", "approved", "active" -> "El pago ya fue aprobado."
        "payment_rejected", "rejected" -> "La solicitud no permite registrar pagos en este momento."
        "payment_submitted" -> "El pago ya fue enviado y esta en revision."
        else -> "La solicitud no permite enviar comprobantes en su estado actual."
    }
}
