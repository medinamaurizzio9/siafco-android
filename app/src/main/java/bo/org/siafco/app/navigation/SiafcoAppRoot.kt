package bo.org.siafco.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import bo.org.siafco.app.AppContainer
import bo.org.siafco.app.feature.auth.LoginScreen
import bo.org.siafco.app.feature.auth.LoginViewModel
import bo.org.siafco.app.feature.home.HomeScreen
import bo.org.siafco.app.feature.home.HomeViewModel
import bo.org.siafco.app.feature.payment.PaymentScreen
import bo.org.siafco.app.feature.payment.PaymentViewModel
import bo.org.siafco.app.feature.register.RegisterAffiliationScreen
import bo.org.siafco.app.feature.register.RegisterAffiliationViewModel
import bo.org.siafco.app.feature.splash.SplashScreen
import bo.org.siafco.app.feature.splash.SplashViewModel

@Composable
fun SiafcoAppRoot(container: AppContainer, navController: NavHostController = rememberNavController()) {
    val factory = remember(container) {
        SiafcoViewModelFactory(
            container.authRepository,
            container.affiliationRepository,
            container.paymentRepository,
            container.pendingPaymentStore
        )
    }

    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable<Routes.Splash> {
            val viewModel: SplashViewModel = viewModel(factory = factory)
            SplashScreen(
                viewModel = viewModel,
                onAuthenticated = {
                    navController.navigate(Routes.Home) {
                        popUpTo<Routes.Splash> { inclusive = true }
                    }
                },
                onLoginRequired = {
                    navController.navigate(Routes.Login) {
                        popUpTo<Routes.Splash> { inclusive = true }
                    }
                }
            )
        }
        composable<Routes.Login> {
            val viewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.Home) {
                        popUpTo<Routes.Login> { inclusive = true }
                    }
                },
                onRegister = {
                    navController.navigate(Routes.RegisterAffiliation)
                }
            )
        }
        composable<Routes.RegisterAffiliation> {
            val viewModel: RegisterAffiliationViewModel = viewModel(factory = factory)
            RegisterAffiliationScreen(
                viewModel = viewModel,
                onCompleted = {
                    navController.navigate(Routes.Home) {
                        popUpTo<Routes.RegisterAffiliation> { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.navigate(Routes.Login) {
                        popUpTo<Routes.RegisterAffiliation> { inclusive = true }
                    }
                }
            )
        }
        composable<Routes.Home> {
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = viewModel,
                onSubmitPayment = {
                    navController.navigate(Routes.Payment)
                },
                onLoggedOut = {
                    navController.navigate(Routes.Login) {
                        popUpTo<Routes.Home> { inclusive = true }
                    }
                }
            )
        }
        composable<Routes.Payment> {
            val viewModel: PaymentViewModel = viewModel(factory = factory)
            PaymentScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.navigate(Routes.Home) {
                        popUpTo<Routes.Home> { inclusive = true }
                    }
                },
                onLoggedOut = {
                    navController.navigate(Routes.Login) {
                        popUpTo<Routes.Home> { inclusive = true }
                    }
                }
            )
        }
    }
}
