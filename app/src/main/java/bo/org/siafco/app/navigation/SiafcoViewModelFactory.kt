package bo.org.siafco.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import bo.org.siafco.app.data.repository.AuthRepository
import bo.org.siafco.app.feature.auth.LoginViewModel
import bo.org.siafco.app.feature.home.HomeViewModel
import bo.org.siafco.app.feature.splash.SplashViewModel

class SiafcoViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> SplashViewModel(authRepository)
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(authRepository)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(authRepository)
            else -> error("ViewModel no soportado: ${modelClass.name}")
        } as T
    }
}
