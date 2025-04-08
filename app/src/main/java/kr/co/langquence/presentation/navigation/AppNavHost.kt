package kr.co.langquence.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kr.co.langquence.presentation.ui.home.CorrectScreen
import kr.co.langquence.presentation.ui.home.HomeScreen
import kr.co.langquence.presentation.ui.profile.ProfileScreen
import kr.co.langquence.presentation.viewmodel.home.VoiceRecordViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = Routes.Home
) {
    val correctionViewModel: VoiceRecordViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Routes.Home> { backStackEntity ->
            val voiceRecordViewModel: VoiceRecordViewModel = hiltViewModel(backStackEntity)

            HomeScreen(
                voiceRecordViewModel = voiceRecordViewModel,
                onNavigateToProfile = { navController.navigate(Routes.Profile) },
                onNavigateToResult = { navController.navigate(Routes.Correction) }
            )
        }

        composable<Routes.Profile> {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // 음성 인식 결과 화면
        composable<Routes.Correction> {
            val voiceRecordViewModel: VoiceRecordViewModel = if (navController.previousBackStackEntry != null) {
                hiltViewModel(navController.previousBackStackEntry!!)
            } else hiltViewModel()

            CorrectScreen(
                voiceRecordViewModel = voiceRecordViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}