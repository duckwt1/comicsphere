package com.android.dacs3.presentations.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.dacs3.utliz.Screens
import com.android.dacs3.utliz.SessionManager
import com.example.financial_app.presentation.navigation.BottomNavigationBar

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color(0xFFE0E0E0)), // Light gray background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Avatar",
                    modifier = Modifier.size(60.dp),
                    tint = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card with profile information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileItem(label = "Full name", value = "N/A")
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                    ProfileItem(label = "Nickname", value = "N/A")
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                    ProfileItem(label = "Email", value = "N/A")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Button(
                    onClick = {
                        sessionManager.clearSession()
                        navController.navigate(Screens.LoginScreen.route) {
                            popUpTo(Screens.SplashScreen.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



@Composable
fun ProfileItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(
            value.ifBlank { "N/A" },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}
