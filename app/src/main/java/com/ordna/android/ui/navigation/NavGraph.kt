package com.ordna.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ordna.android.ui.settings.SettingsScreen
import com.ordna.android.ui.signin.SignInScreen
import com.ordna.android.ui.today.TodayScreen

@Composable
fun OrdnaNavGraph(
    navigateToToday: Boolean = false,
    authViewModel: AuthCheckViewModel = hiltViewModel(),
) {
    val isSignedIn by authViewModel.isSignedIn.collectAsState()

    // Wait for DataStore to load before showing anything — avoids sign-in screen flash
    val signedIn = isSignedIn ?: return

    val navController = rememberNavController()
    val startDestination = if (signedIn) "today" else "signin"

    // If launched from notification, pop back to today screen
    LaunchedEffect(navigateToToday) {
        if (navigateToToday && signedIn) {
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
