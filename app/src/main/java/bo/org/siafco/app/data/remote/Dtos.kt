package bo.org.siafco.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: Map<String, List<String>> = emptyMap()
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String = "Android"
)

@Serializable
data class LoginPayload(
    @SerialName("token_type") val tokenType: String,
    @SerialName("access_token") val accessToken: String,
    val profile: MobileProfileDto
)

@Serializable
data class ProfilePayload(
    val profile: MobileProfileDto
)

@Serializable
data class CatalogsPayload(
    val sectors: List<SectorDto>,
    val plans: List<AffiliationPlanDto>,
    val regionals: List<String>,
    @SerialName("issued_in") val issuedIn: List<IssuedInDto>,
    @SerialName("marital_statuses") val maritalStatuses: List<String>,
    val institution: InstitutionDto
)

@Serializable
data class SectorDto(
    val id: Long,
    val name: String,
    val code: String? = null,
    val regional: String? = null,
    val institution: String? = null
)

@Serializable
data class AffiliationPlanDto(
    val id: Long,
    @SerialName("sector_id") val sectorId: Long? = null,
    val name: String,
    val type: String? = null,
    val currency: String,
    @SerialName("affiliation_fee") val affiliationFee: Double,
    @SerialName("credential_fee") val credentialFee: Double,
    @SerialName("total_amount") val totalAmount: Double,
    val description: String? = null,
    @SerialName("payment_instructions") val paymentInstructions: String? = null
)

@Serializable
data class IssuedInDto(
    val value: String,
    val label: String
)

@Serializable
data class InstitutionDto(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("payment_bank") val paymentBank: String? = null,
    @SerialName("payment_holder") val paymentHolder: String? = null,
    @SerialName("payment_account") val paymentAccount: String? = null,
    @SerialName("payment_instructions") val paymentInstructions: String? = null,
    @SerialName("terms_version") val termsVersion: String,
    @SerialName("privacy_version") val privacyVersion: String
)

@Serializable
class EmptyPayload

@Serializable
data class MobileProfileDto(
    val user: UserDto,
    val affiliate: AffiliateDto? = null
)

@Serializable
data class UserDto(
    val name: String,
    val email: String
)

@Serializable
data class AffiliateDto(
    @SerialName("full_name") val fullName: String? = null,
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("access_level") val accessLevel: String? = null
)
