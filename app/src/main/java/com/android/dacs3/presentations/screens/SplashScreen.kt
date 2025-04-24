package com.android.dacs3.presentations.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.android.dacs3.utliz.Screens
import com.google.android.gms.cast.framework.SessionManager

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { com.android.dacs3.utliz.SessionManager(context) }

    LaunchedEffect(Unit) {
        if (sessionManager.isLoggedIn()) {
            navController.navigate(Screens.HistoryScreen.route) {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...")

    }
}