package bo.org.siafco.app.feature.register

object RegisterBackPolicy {
    fun decide(step: Int): RegisterBackDecision =
        if (step <= 0) RegisterBackDecision.ConfirmExit else RegisterBackDecision.PreviousStep
}

enum class RegisterBackDecision {
    PreviousStep,
    ConfirmExit
}
