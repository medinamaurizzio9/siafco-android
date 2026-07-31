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
import bo.org.siafco.app.feature.splash.SplashScreen
import bo.org.siafco.app.feature.splash.SplashViewModel

@Composable
fun SiafcoAppRoot(container: AppContainer, navController: NavHostController = rememberNavController()) {
    val factory = remember(container) { SiafcoViewModelFactory(container.authRepository) }

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
                }
            )
        }
        composable<Routes.Home> {
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = viewModel,
                onLoggedOut = {
                    navController.navigate(Routes.Login) {
                        popUpTo<Routes.Home> { inclusive = true }
                    }
                }
            )
        }
    }
}
