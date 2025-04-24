package com.android.dacs3.presentations.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.dacs3.presentations.screens.*
import com.android.dacs3.utliz.Screens
import com.android.dacs3.presentations.screens.ProfileScreen
import com.android.dacs3.presentations.screens.ExploreScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screens.SplashScreen.route
    ) {
        composable(Screens.SplashScreen.route) { SplashScreen(navController) }
        composable(Screens.LoginScreen.route) { LoginScreen(navController) }
        composable(Screens.HistoryScreen.route) { HistoryScreen(navController) }
        composable(Screens.FavouriteScreen.route) { FavouriteScreen(navController) }
        composable(Screens.ExploreScreen.route) { ExploreScreen(navController) }
        composable(Screens.ProfileScreen.route) { ProfileScreen(navController) }
        composable(Screens.SignUpScreen.route) { SignUpScreen(navController) }

    }
}