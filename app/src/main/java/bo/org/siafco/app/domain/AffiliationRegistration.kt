package bo.org.siafco.app.domain

import java.io.File

data class AffiliationCatalogs(
    val sectors: List<CatalogSector>,
    val plans: List<CatalogPlan>,
    val regionals: List<String>,
    val issuedIn: List<CatalogOption>,
    val maritalStatuses: List<String>,
    val institution: CatalogInstitution
)

data class CatalogSector(
    val id: Long,
    val name: String,
    val code: String?,
    val regional: String?,
    val institution: String?
)

data class CatalogPlan(
    val id: Long,
    val sectorId: Long?,
    val name: String,
    val type: String?,
    val currency: String,
    val affiliationFee: Double,
    val credentialFee: Double,
    val totalAmount: Double,
    val description: String?,
    val paymentInstructions: String?
)

data class CatalogOption(val value: String, val label: String)

data class CatalogInstitution(
    val name: String,
    val termsVersion: String,
    val privacyVersion: String
)

data class AffiliationRegistrationForm(
    val fullName: String = "",
    val ci: String = "",
    val ciComplement: String = "",
    val issuedIn: String = "",
    val birthDate: String = "",
    val maritalStatus: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val sectorId: Long? = null,
    val planId: Long? = null,
    val regional: String = "",
    val institution: String = "",
    val position: String = "",
    val photo: PreparedPhoto? = null,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false
) {
    fun withoutSensitiveData(): AffiliationRegistrationForm = copy(
        password = "",
        passwordConfirmation = "",
        photo = null
    )
}

data class PreparedPhoto(
    val file: File,
    val displayName: String
)

data class AffiliationRegistrationSuccess(
    val profile: SessionProfile,
    val requestCode: String?
)
