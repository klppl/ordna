package io.github.klppl.klar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.klppl.klar.ui.settings.SettingsScreen
import io.github.klppl.klar.ui.signin.SignInScreen
import io.github.klppl.klar.ui.today.TodayScreen

@Composable
fun KlarNavGraph(
    navigateTodayTrigger: Int = 0,
    authViewModel: AuthCheckViewModel = hiltViewModel(),
) {
    val isSignedIn by authViewModel.isSignedIn.collectAsState()

    // Wait for DataStore to load before showing anything — avoids sign-in screen flash
    val signedIn = isSignedIn ?: return

    val navController = rememberNavController()
    val startDestination = if (signedIn) "today" else "signin"

    // If launched/re-targeted from a notification, pop back to the today screen.
    // Keyed on a counter so repeated taps re-trigger even while the app is open.
    LaunchedEffect(navigateTodayTrigger) {
        if (navigateTodayTrigger > 0 && signedIn) {
            navController.popBackStack("today", inclusive = false)
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("signin") {
            SignInScreen(
                onSignedIn = {
                    navController.navigate("today") {
                        popUpTo("signin") { inclusive = true }
                    }
                },
            )
        }
        composable("today") {
            TodayScreen(
                onAuthExpired = {
                    navController.navigate("signin") {
                        popUpTo("today") { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate("signin") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
