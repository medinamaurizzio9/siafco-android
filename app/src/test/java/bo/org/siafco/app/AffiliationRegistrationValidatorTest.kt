package bo.org.siafco.app

import bo.org.siafco.app.domain.AffiliationRegistrationForm
import bo.org.siafco.app.domain.AffiliationRegistrationValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AffiliationRegistrationValidatorTest {
    @Test
    fun identityRequiresRequiredFields() {
        val errors = AffiliationRegistrationValidator.validateStep(0, AffiliationRegistrationForm())

        assertTrue(errors.containsKey("full_name"))
        assertTrue(errors.containsKey("ci"))
        assertTrue(errors.containsKey("issued_in"))
        assertTrue(errors.containsKey("birth_date"))
        assertTrue(errors.containsKey("marital_status"))
    }

    @Test
    fun contactValidatesPhoneEmailAndPasswordConfirmation() {
        val errors = AffiliationRegistrationValidator.validateStep(
            1,
            AffiliationRegistrationForm(
                phone = "123",
                email = "bad",
                address = "Calle",
                password = "12345678",
                passwordConfirmation = "87654321"
            )
        )

        assertTrue(errors.containsKey("phone"))
        assertTrue(errors.containsKey("email"))
        assertTrue(errors.containsKey("password_confirmation"))
    }

    @Test
    fun institutionalRequiresPlanAndSector() {
        val errors = AffiliationRegistrationValidator.validateStep(
            2,
            AffiliationRegistrationForm(regional = "LA PAZ", institution = "Demo", position = "Cargo")
        )

        assertTrue(errors.containsKey("sector_id"))
        assertTrue(errors.containsKey("affiliation_plan_id"))
    }

    @Test
    fun confirmationRequiresSeparateAcceptances() {
        val errors = AffiliationRegistrationValidator.validateStep(4, AffiliationRegistrationForm())

        assertTrue(errors.containsKey("terms_accepted"))
        assertTrue(errors.containsKey("privacy_accepted"))
    }

    @Test
    fun validFormHasNoValidationErrors() {
        val form = validForm()

        val errors = AffiliationRegistrationValidator.validateAll(form)

        assertFalse(errors.keys.any { it != "photo" })
    }

    private fun validForm() = AffiliationRegistrationForm(
        fullName = "Ana Perez",
        ci = "123",
        issuedIn = "LP",
        birthDate = "1990-01-01",
        maritalStatus = "SOLTERO",
        phone = "70000001",
        email = "ana@siafco.test",
        address = "Calle",
        password = "Secret123",
        passwordConfirmation = "Secret123",
        sectorId = 1,
        planId = 1,
        regional = "LA PAZ",
        institution = "Institucion",
        position = "Cargo",
        termsAccepted = true,
        privacyAccepted = true
    )
}
