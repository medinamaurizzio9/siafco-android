package bo.org.siafco.app.navigation

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object Splash : Routes

    @Serializable
    data object Login : Routes

    @Serializable
    data object RegisterAffiliation : Routes

    @Serializable
    data object Home : Routes

    @Serializable
    data object Payment : Routes

    @Serializable
    data object Profile : Routes

    @Serializable
    data object AffiliationRequest : Routes

    @Serializable
    data object Credential : Routes
}
