package com.android.dacs3.presentations.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.dacs3.presentations.screens.*
import com.android.dacs3.utliz.Screens
import com.android.dacs3.presentations.screens.ProfileScreen
import com.android.dacs3.viewmodel.MangaViewModel

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

        composable(
            route = Screens.DetailsScreen.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("id") ?: ""
            val viewModel: MangaViewModel = hiltViewModel()

            MangaDetailScreen(
                mangaId = mangaId,
                navController = navController,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() } // Handle back navigation
            )
        }


    }
}