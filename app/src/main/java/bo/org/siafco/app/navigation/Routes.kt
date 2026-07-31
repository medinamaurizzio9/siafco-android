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
}
