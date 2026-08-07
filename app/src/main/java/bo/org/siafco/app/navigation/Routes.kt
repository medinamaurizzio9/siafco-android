package bo.org.siafco.app.navigation

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object Splash : Routes

    @Serializable
    data object Login : Routes

    @Serializable
    data object Welcome : Routes

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

    @Serializable
    data object Store : Routes

    @Serializable
    data class StoreProduct(val publicCode: String) : Routes

    @Serializable
    data object StoreCart : Routes

    @Serializable
    data object StoreCheckout : Routes

    @Serializable
    data object StoreOrders : Routes

    @Serializable
    data class StoreOrderDetail(val orderCode: String) : Routes

    @Serializable
    data class StoreReceipt(val orderCode: String) : Routes
}
