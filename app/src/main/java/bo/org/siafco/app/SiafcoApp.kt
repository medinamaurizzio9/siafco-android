package bo.org.siafco.app

import android.app.Application
import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.core.data.SecureTokenStore
import bo.org.siafco.app.core.crypto.AndroidKeyStoreTokenCipher
import bo.org.siafco.app.core.network.NetworkModule
import bo.org.siafco.app.data.repository.AuthRepository

class SiafcoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val tokenStore = SecureTokenStore(
            context = this,
            cipher = AndroidKeyStoreTokenCipher()
        )
        container = AppContainer(
            tokenStore = tokenStore,
            authRepository = AuthRepository(
                api = NetworkModule.createApi(tokenStore),
                tokenStore = tokenStore
            )
        )
    }
}

data class AppContainer(
    val tokenStore: TokenStore,
    val authRepository: AuthRepository
)
