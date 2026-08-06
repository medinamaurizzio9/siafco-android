package bo.org.siafco.app.domain

data class SessionProfile(
    val name: String,
    val email: String,
    val affiliateStatus: String,
    val affiliateStatusLabel: String,
    val accessLevel: AccessLevel,
    val hasAffiliateProfile: Boolean = false,
    val allowedProfileFields: Set<String> = emptySet(),
    val requestCode: String? = null,
    val photoUrl: String? = null,
    val registrationNumber: String? = null
)

enum class AccessLevel {
    Pending,
    Active
}

data class AffiliateCapabilities(
    val canViewProfile: Boolean,
    val canEditProfile: Boolean,
    val canViewAffiliationRequest: Boolean,
    val canSubmitPayment: Boolean,
    val canViewCredential: Boolean
)
