package bo.org.siafco.app.data.repository

import bo.org.siafco.app.core.debug.MobileDiagnostics
import bo.org.siafco.app.data.remote.MobileProfileDto
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.SessionProfile

fun MobileProfileDto.toSessionProfile(source: String): SessionProfile {
    val status = affiliate?.status.orEmpty()
    val accessLevelRaw = affiliate?.accessLevel
    val allowedFields = allowedProfileFields.toSet()

    MobileDiagnostics.home(
        "$source.dto",
        "allowed_profile_fields=$allowedProfileFields status=$status access_level=$accessLevelRaw hasAffiliate=${affiliate != null}"
    )

    val profile = SessionProfile(
        name = affiliate?.fullName ?: user.name,
        email = user.email,
        affiliateStatus = status,
        affiliateStatusLabel = affiliate?.statusLabel ?: status.ifBlank { "Sin estado" },
        accessLevel = if (status == "activo" || accessLevelRaw == "full") {
            AccessLevel.Active
        } else {
            AccessLevel.Pending
        },
        hasAffiliateProfile = affiliate != null,
        allowedProfileFields = allowedFields
    )

    MobileDiagnostics.home(
        "$source.mapper",
        "allowed_profile_fields=${profile.allowedProfileFields} status=${profile.affiliateStatus} access_level=${profile.accessLevel} hasAffiliate=${profile.hasAffiliateProfile}"
    )

    return profile
}
