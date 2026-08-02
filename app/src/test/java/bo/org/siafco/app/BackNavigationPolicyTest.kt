package bo.org.siafco.app

import bo.org.siafco.app.feature.payment.PaymentBackDecision
import bo.org.siafco.app.feature.payment.PaymentBackPolicy
import bo.org.siafco.app.feature.photo.PhotoInputPhase
import bo.org.siafco.app.feature.photo.PhotoInputPolicy
import bo.org.siafco.app.feature.register.RegisterBackDecision
import bo.org.siafco.app.feature.register.RegisterBackPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class BackNavigationPolicyTest {
    @Test
    fun registerBackMovesBetweenStepsAndConfirmsAtFirstStep() {
        assertEquals(RegisterBackDecision.ConfirmExit, RegisterBackPolicy.decide(0))
        assertEquals(RegisterBackDecision.PreviousStep, RegisterBackPolicy.decide(1))
        assertEquals(RegisterBackDecision.PreviousStep, RegisterBackPolicy.decide(4))
    }

    @Test
    fun paymentBackPreservesDraftWhenSubmissionIsUncertain() {
        assertEquals(PaymentBackDecision.LeaveAndClearDraft, PaymentBackPolicy.decide(submitting = false, networkRetryAvailable = false))
        assertEquals(PaymentBackDecision.ConfirmAndPreserveDraft, PaymentBackPolicy.decide(submitting = true, networkRetryAvailable = false))
        assertEquals(PaymentBackDecision.ConfirmAndPreserveDraft, PaymentBackPolicy.decide(submitting = false, networkRetryAvailable = true))
    }

    @Test
    fun selectedCameraOrGalleryUriOpensCropInsteadOfProcessingImmediately() {
        assertEquals(PhotoInputPhase.Cropping, PhotoInputPolicy.nextPhaseAfterExternalResult("content://photo/1"))
        assertEquals(PhotoInputPhase.Cancelled, PhotoInputPolicy.nextPhaseAfterExternalResult(null))
        assertEquals(PhotoInputPhase.Cancelled, PhotoInputPolicy.nextPhaseAfterExternalResult(""))
    }
}
