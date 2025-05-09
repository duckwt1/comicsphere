package com.android.dacs3.presentations.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.android.dacs3.R
import com.android.dacs3.utliz.Screens
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Tăng chiều cao một chút
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color.White)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                spotColor = Color(0x1A000000)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // History item (using drawable resource)
            BottomNavItemWithDrawable(
                drawableRes = R.drawable.ic_history,
                label = "History",
                isSelected = currentRoute == Screens.HistoryScreen.route
            ) {
                navController.navigate(Screens.HistoryScreen.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }

            // Favourite item
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

            // Explore item
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

            // Profile item
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
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF444444) else Color.Transparent

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF000000),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF000000)
        )
    }
}

@Composable
fun BottomNavItemWithDrawable(
    drawableRes: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = Color(0xFF626262) // Màu xám nhẹ
    val backgroundColor = if (isSelected) Color(0xFF7E7E7E) else Color.Transparent

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF000000),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF000000)
        )
    }
}