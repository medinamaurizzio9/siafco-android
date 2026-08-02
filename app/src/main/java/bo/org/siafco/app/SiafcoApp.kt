package bo.org.siafco.app

import android.app.Application
import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.core.data.SecureTokenStore
import bo.org.siafco.app.core.crypto.AndroidKeyStoreTokenCipher
import bo.org.siafco.app.core.network.NetworkModule
import bo.org.siafco.app.data.payment.EncryptedPendingPaymentStore
import bo.org.siafco.app.data.store.PreferencesStoreCartStore
import bo.org.siafco.app.data.store.PreferencesStorePendingOrderStore
import bo.org.siafco.app.data.store.StoreCartStore
import bo.org.siafco.app.data.store.StorePendingOrderStore
import bo.org.siafco.app.data.repository.AffiliationRepository
import bo.org.siafco.app.data.repository.AuthRepository
import bo.org.siafco.app.data.repository.CredentialRepository
import bo.org.siafco.app.data.repository.PaymentRepository
import bo.org.siafco.app.data.repository.ProfileRepository
import bo.org.siafco.app.data.repository.StoreRepository

class SiafcoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val tokenStore = SecureTokenStore(
            context = this,
            cipher = AndroidKeyStoreTokenCipher()
        )
        val api = NetworkModule.createApi(tokenStore)
        val paymentRepository = PaymentRepository(api)
        val profileRepository = ProfileRepository(api, tokenStore)
        val credentialRepository = CredentialRepository(api, tokenStore)
        val storeCartStore = PreferencesStoreCartStore(this)
        val pendingOrderStore = PreferencesStorePendingOrderStore(this)
        container = AppContainer(
            tokenStore = tokenStore,
            authRepository = AuthRepository(
                api = api,
                tokenStore = tokenStore
            ),
            affiliationRepository = AffiliationRepository(
                api = api,
                tokenStore = tokenStore
            ),
            paymentRepository = paymentRepository,
            profileRepository = profileRepository,
            credentialRepository = credentialRepository,
            storeRepository = StoreRepository(api),
            storeCartStore = storeCartStore,
            pendingOrderStore = pendingOrderStore,
            pendingPaymentStore = EncryptedPendingPaymentStore(this)
        )
    }
}

data class AppContainer(
    val tokenStore: TokenStore,
    val authRepository: AuthRepository,
    val affiliationRepository: AffiliationRepository,
    val paymentRepository: PaymentRepository,
    val profileRepository: ProfileRepository,
    val credentialRepository: CredentialRepository,
    val storeRepository: StoreRepository,
    val storeCartStore: StoreCartStore,
    val pendingOrderStore: StorePendingOrderStore,
    val pendingPaymentStore: bo.org.siafco.app.data.payment.PendingPaymentStore
)
