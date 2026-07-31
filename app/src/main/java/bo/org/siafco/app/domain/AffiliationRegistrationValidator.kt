package bo.org.siafco.app.domain

object AffiliationRegistrationValidator {
    fun validateStep(step: Int, form: AffiliationRegistrationForm): Map<String, String> = when (step) {
        0 -> validateIdentity(form)
        1 -> validateContact(form)
        2 -> validateInstitutional(form)
        3 -> validatePhoto(form)
        4 -> validateConfirmation(form)
        else -> emptyMap()
    }

    fun validateAll(form: AffiliationRegistrationForm): Map<String, String> {
        return (0..4).flatMap { validateStep(it, form).entries }
            .associate { it.key to it.value }
    }

    private fun validateIdentity(form: AffiliationRegistrationForm): Map<String, String> = buildMap {
        if (form.fullName.isBlank()) put("full_name", "Ingresa tu nombre completo.")
        if (form.ci.isBlank()) put("ci", "Ingresa tu CI.")
        if (form.issuedIn.isBlank()) put("issued_in", "Selecciona el expedido.")
        if (!form.birthDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            put("birth_date", "Usa el formato AAAA-MM-DD.")
        }
        if (form.maritalStatus.isBlank()) put("marital_status", "Selecciona tu estado civil.")
    }

    private fun validateContact(form: AffiliationRegistrationForm): Map<String, String> = buildMap {
        if (!form.phone.matches(Regex("""\d{8}"""))) put("phone", "Ingresa un celular de 8 digitos.")
        if (form.email.isBlank() || !form.email.contains("@") || !form.email.contains(".")) {
            put("email", "Ingresa un correo valido.")
        }
        if (form.address.isBlank()) put("address", "Ingresa tu direccion.")
        if (form.password.length < 8) put("password", "La contrasena debe tener al menos 8 caracteres.")
        if (form.passwordConfirmation != form.password) put("password_confirmation", "Las contrasenas no coinciden.")
    }

    private fun validateInstitutional(form: AffiliationRegistrationForm): Map<String, String> = buildMap {
        if (form.sectorId == null) put("sector_id", "Selecciona un sector.")
        if (form.planId == null) put("affiliation_plan_id", "Selecciona un plan.")
        if (form.regional.isBlank()) put("regional", "Selecciona una regional.")
        if (form.institution.isBlank()) put("institution", "Ingresa la institucion.")
        if (form.position.isBlank()) put("position", "Ingresa tu cargo.")
    }

    private fun validatePhoto(form: AffiliationRegistrationForm): Map<String, String> = buildMap {
        if (form.photo == null || !form.photo.file.exists()) put("photo", "Selecciona una fotografia.")
    }

    private fun validateConfirmation(form: AffiliationRegistrationForm): Map<String, String> = buildMap {
        if (!form.termsAccepted) put("terms_accepted", "Debes aceptar los terminos.")
        if (!form.privacyAccepted) put("privacy_accepted", "Debes aceptar la privacidad.")
    }
}
