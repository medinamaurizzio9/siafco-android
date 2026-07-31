package bo.org.siafco.app.domain

data class SessionProfile(
    val name: String,
    val email: String,
    val affiliateStatus: String,
    val affiliateStatusLabel: String,
    val accessLevel: AccessLevel
)

enum class AccessLevel {
    Pending,
    Active
}
