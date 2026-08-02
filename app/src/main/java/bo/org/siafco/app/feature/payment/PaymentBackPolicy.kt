package bo.org.siafco.app.feature.payment

object PaymentBackPolicy {
    fun decide(submitting: Boolean, networkRetryAvailable: Boolean): PaymentBackDecision =
        if (submitting || networkRetryAvailable) {
            PaymentBackDecision.ConfirmAndPreserveDraft
        } else {
            PaymentBackDecision.LeaveAndClearDraft
        }
}

enum class PaymentBackDecision {
    LeaveAndClearDraft,
    ConfirmAndPreserveDraft
}
