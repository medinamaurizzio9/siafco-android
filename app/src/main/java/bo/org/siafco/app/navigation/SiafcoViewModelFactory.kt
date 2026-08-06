package bo.org.siafco.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import bo.org.siafco.app.data.payment.PendingPaymentStore
import bo.org.siafco.app.data.repository.AffiliationRepository
import bo.org.siafco.app.data.repository.AuthRepository
import bo.org.siafco.app.data.repository.CredentialRepository
import bo.org.siafco.app.data.repository.PaymentRepository
import bo.org.siafco.app.data.repository.ProfileRepository
import bo.org.siafco.app.data.repository.StoreRepository
import bo.org.siafco.app.data.store.StoreCartStore
import bo.org.siafco.app.data.store.StorePendingOrderStore
import bo.org.siafco.app.feature.auth.LoginViewModel
import bo.org.siafco.app.feature.credential.CredentialViewModel
import bo.org.siafco.app.feature.home.HomeViewModel
import bo.org.siafco.app.feature.payment.PaymentViewModel
import bo.org.siafco.app.feature.profile.ProfileViewModel
import bo.org.siafco.app.feature.register.RegisterAffiliationViewModel
import bo.org.siafco.app.feature.request.AffiliationRequestViewModel
import bo.org.siafco.app.feature.splash.SplashViewModel
import bo.org.siafco.app.feature.store.StoreCartViewModel
import bo.org.siafco.app.feature.store.StoreCatalogViewModel
import bo.org.siafco.app.feature.store.StoreCheckoutViewModel
import bo.org.siafco.app.feature.store.StoreOrderDetailViewModel
import bo.org.siafco.app.feature.store.StoreOrdersViewModel
import bo.org.siafco.app.feature.store.StoreProductViewModel
import bo.org.siafco.app.feature.store.StoreReceiptViewModel

class SiafcoViewModelFactory(
    private val authRepository: AuthRepository,
    private val affiliationRepository: AffiliationRepository,
    private val paymentRepository: PaymentRepository,
    private val profileRepository: ProfileRepository,
    private val credentialRepository: CredentialRepository,
    private val storeRepository: StoreRepository,
    private val storeCartStore: StoreCartStore,
    private val pendingOrderStore: StorePendingOrderStore,
    private val pendingPaymentStore: PendingPaymentStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> SplashViewModel(authRepository)
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(authRepository)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(authRepository, storeRepository)
            modelClass.isAssignableFrom(RegisterAffiliationViewModel::class.java) -> {
                RegisterAffiliationViewModel(affiliationRepository)
            }
            modelClass.isAssignableFrom(PaymentViewModel::class.java) -> {
                PaymentViewModel(paymentRepository, authRepository, pendingPaymentStore)
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(profileRepository)
            }
            modelClass.isAssignableFrom(AffiliationRequestViewModel::class.java) -> {
                AffiliationRequestViewModel(authRepository)
            }
            modelClass.isAssignableFrom(CredentialViewModel::class.java) -> {
                CredentialViewModel(credentialRepository)
            }
            modelClass.isAssignableFrom(StoreCatalogViewModel::class.java) -> {
                StoreCatalogViewModel(storeRepository)
            }
            modelClass.isAssignableFrom(StoreProductViewModel::class.java) -> {
                StoreProductViewModel(storeRepository, storeCartStore)
            }
            modelClass.isAssignableFrom(StoreCartViewModel::class.java) -> {
                StoreCartViewModel(storeRepository, storeCartStore)
            }
            modelClass.isAssignableFrom(StoreCheckoutViewModel::class.java) -> {
                StoreCheckoutViewModel(storeRepository, storeCartStore, pendingOrderStore)
            }
            modelClass.isAssignableFrom(StoreOrdersViewModel::class.java) -> {
                StoreOrdersViewModel(storeRepository)
            }
            modelClass.isAssignableFrom(StoreOrderDetailViewModel::class.java) -> {
                StoreOrderDetailViewModel(storeRepository)
            }
            modelClass.isAssignableFrom(StoreReceiptViewModel::class.java) -> {
                StoreReceiptViewModel(storeRepository)
            }
            else -> error("ViewModel no soportado: ${modelClass.name}")
        } as T
    }
}
