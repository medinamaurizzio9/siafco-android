package bo.org.siafco.app.domain

data class AffiliationRequestSummary(
    val requestCode: String,
    val status: String,
    val statusLabel: String,
    val statusDescription: String?,
    val planName: String?,
    val amountDue: Double?,
    val currency: String?,
    val observations: String?,
    val paymentStatus: String?,
    val paymentStatusLabel: String?,
    val canSubmitPayment: Boolean,
    val canLogin: Boolean,
    val canViewCredential: Boolean
)
