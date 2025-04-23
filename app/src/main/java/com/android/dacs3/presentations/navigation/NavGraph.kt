package com.android.dacs3.presentations.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.dacs3.presentations.screens.LoginScreen
import com.android.dacs3.presentations.screens.SignUpScreen
import com.android.dacs3.presentations.screens.historyScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController)
        }

        composable("signup") {
            SignUpScreen(navController)
        }

        composable("history") {
             historyScreen(navController)
        }


    }
}