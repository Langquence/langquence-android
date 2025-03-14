package kr.co.langquence.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kr.co.langquence.presentation.ui.home.CorrectScreen
import kr.co.langquence.presentation.ui.home.HomeScreen
import kr.co.langquence.presentation.ui.profile.ProfileScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME_SCREEN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.HOME_SCREEN) {
            HomeScreen(
                onNavigateToProfile = { navController.navigate(Routes.PROFILE_SCREEN) },
                onNavigateToResult = { navController.navigate(Routes.VOICE_RESULT_SCREEN) }
            )
        }

        composable(Routes.PROFILE_SCREEN) {
            ProfileScreen (
                onBackClick = { navController.popBackStack() }
            )
        }

        // 음성 인식 결과 화면
        composable(Routes.VOICE_RESULT_SCREEN) { 
            CorrectScreen(
                text = "음성 인식 결과가 표시되는 화면",
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}