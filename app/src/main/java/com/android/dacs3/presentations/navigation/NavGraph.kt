package com.android.dacs3.presentations.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.dacs3.presentations.screens.*
import com.android.dacs3.utliz.Screens
import com.android.dacs3.presentations.screens.ProfileScreen
import com.android.dacs3.viewmodel.AuthViewModel
import com.android.dacs3.viewmodel.FavouriteViewModel
import com.android.dacs3.viewmodel.MangaViewModel

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screens.SplashScreen.route
    ) {
        composable(Screens.SplashScreen.route) { SplashScreen(navController) }
        composable(Screens.LoginScreen.route) { LoginScreen(navController) }

        composable(route = Screens.ForgotPasswordScreen.route) {
            ForgotPasswordScreen(navController = navController)
        }

        composable(Screens.HistoryScreen.route) {
            val viewModel: MangaViewModel = hiltViewModel()
            HistoryScreen(navController = navController, viewModel = viewModel)
        }

        composable(Screens.FavouriteScreen.route) { FavouriteScreen(navController) }
        composable(Screens.ExploreScreen.route) { ExploreScreen(navController) }
        composable(Screens.ProfileScreen.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(navController = navController,  viewModel)
        }
        composable(Screens.SignUpScreen.route) { SignUpScreen(navController) }

        composable(
            route = Screens.DetailsScreen.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("id") ?: ""
            val viewModel: MangaViewModel = hiltViewModel()
            val favViewModel: FavouriteViewModel = hiltViewModel()

            MangaDetailScreen(
                mangaId = mangaId,
                navController = navController,
                viewModel = viewModel,
                favViewModel = favViewModel,
                onBackClick = { navController.popBackStack() } // Handle back navigation
            )
        }

        composable(
            route = Screens.ChapterScreen.route,
            arguments = listOf(
                navArgument("mangaId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("language") { type = NavType.StringType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val language = backStackEntry.arguments?.getString("language") ?: ""
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0

            val viewModel = hiltViewModel<MangaViewModel>()

            ChapterScreen(
                mangaId = mangaId,
                chapterId = chapterId,
                language = language,
                pageIndex = pageIndex,
                navController = navController,
                viewModel = viewModel
            )
        }



    }
}