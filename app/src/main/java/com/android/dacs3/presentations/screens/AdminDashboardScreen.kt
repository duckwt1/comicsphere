package com.android.dacs3.presentations.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.AuthViewModel
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import com.android.dacs3.utliz.AdminConfig
import com.google.firebase.auth.FirebaseAuth

// Define black and white theme colors - same as in AdminUserManagementScreen
private val BlackWhiteTheme = object {
    val primary = Color.Black
    val onPrimary = Color.White
    val background = Color.White
    val surface = Color.White
    val onSurface = Color.Black
    val border = Color.Black
    val divider = Color.LightGray
    val iconTint = Color.Black
    val vipIndicator = Color.Black
    val adminIndicator = Color.Black
    val error = Color.Black
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlackWhiteTheme.primary,
                    titleContentColor = BlackWhiteTheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout(context)
                        navController.navigate(Screens.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = BlackWhiteTheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackWhiteTheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome, Admin",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BlackWhiteTheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            val adminFeatures = listOf(
                AdminFeature(
                    title = "User Management",
                    icon = Icons.Default.Person,
                    route = Screens.AdminUserManagementScreen.route
                ),
                AdminFeature(
                    title = "Manga Management",
                    icon = Icons.Default.Info,
                    route = Screens.AdminMangaManagementScreen.route
                )
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(adminFeatures) { feature ->
                    AdminFeatureCard(
                        feature = feature,
                        onClick = { navController.navigate(feature.route) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeatureCard(
    feature: AdminFeature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = BlackWhiteTheme.surface
        ),
        border = BorderStroke(1.dp, BlackWhiteTheme.border),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = BlackWhiteTheme.iconTint,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = feature.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = BlackWhiteTheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = BlackWhiteTheme.iconTint
            )
        }
    }
}

data class AdminFeature(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)