package com.example.financial_app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.android.dacs3.utliz.Screens
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xFFF5F5F5))
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.padding(start = 15.dp))

        BottomNavItem(
            icon = Icons.Filled.Home,
            label = "History",
            isSelected = currentRoute == Screens.HistoryScreen.route
        ) {
            navController.navigate(Screens.HistoryScreen.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }

        BottomNavItem(
            icon = Icons.Filled.Favorite,
            label = "Favourite",
            isSelected = currentRoute == Screens.FavouriteScreen.route
        ) {
            navController.navigate(Screens.FavouriteScreen.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }

        BottomNavItem(
            icon = Icons.Filled.Search,
            label = "Explore",
            isSelected = currentRoute == Screens.ExploreScreen.route
        ) {
            navController.navigate(Screens.ExploreScreen.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }

        BottomNavItem(
            icon = Icons.Filled.Person,
            label = "Profile",
            isSelected = currentRoute == Screens.ProfileScreen.route
        ) {
            navController.navigate(Screens.ProfileScreen.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }

        Spacer(modifier = Modifier.padding(end = 15.dp))
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))
    )
    val transparentBrush = SolidColor(Color.Transparent) // Use SolidColor for transparent background

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(brush = if (isSelected) gradient else transparentBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF052224)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF00D09E) else Color(0xFF052224)
        )
    }
}