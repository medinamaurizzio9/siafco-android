package bo.org.siafco.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import bo.org.siafco.app.data.payment.PendingPaymentStore
import bo.org.siafco.app.data.repository.AffiliationRepository
import bo.org.siafco.app.data.repository.AuthRepository
import bo.org.siafco.app.data.repository.PaymentRepository
import bo.org.siafco.app.data.repository.ProfileRepository
import bo.org.siafco.app.feature.auth.LoginViewModel
import bo.org.siafco.app.feature.home.HomeViewModel
import bo.org.siafco.app.feature.payment.PaymentViewModel
import bo.org.siafco.app.feature.profile.ProfileViewModel
import bo.org.siafco.app.feature.register.RegisterAffiliationViewModel
import bo.org.siafco.app.feature.request.AffiliationRequestViewModel
import bo.org.siafco.app.feature.splash.SplashViewModel

class SiafcoViewModelFactory(
    private val authRepository: AuthRepository,
    private val affiliationRepository: AffiliationRepository,
    private val paymentRepository: PaymentRepository,
    private val profileRepository: ProfileRepository,
    private val pendingPaymentStore: PendingPaymentStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> SplashViewModel(authRepository)
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(authRepository)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(authRepository)
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
            else -> error("ViewModel no soportado: ${modelClass.name}")
        } as T
    }
}
