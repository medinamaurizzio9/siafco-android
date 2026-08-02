package bo.org.siafco.app.domain

data class MobileProfile(
    val fullName: String,
    val ci: String?,
    val email: String,
    val phone: String?,
    val address: String?,
    val birthDate: String?,
    val maritalStatus: String?,
    val photoUrl: String?,
    val registrationNumber: String?,
    val sectorName: String?,
    val planName: String?,
    val regional: String?,
    val institution: String?,
    val position: String?,
    val status: String?,
    val statusLabel: String?,
    val allowedFields: Set<String>,
    val mustChangePassword: Boolean
)

data class ProfileUpdateForm(
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val birthDate: String = "",
    val maritalStatus: String = ""
)

data class ChangePasswordForm(
    val currentPassword: String = "",
    val password: String = "",
    val passwordConfirmation: String = ""
) {
    fun cleared(): ChangePasswordForm = ChangePasswordForm()
}
