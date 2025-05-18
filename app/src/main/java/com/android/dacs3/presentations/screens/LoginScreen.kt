package com.android.dacs3.presentations.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.dacs3.R
import com.android.dacs3.presentations.components.GoogleSignInButton
import com.android.dacs3.presentations.components.StyledTextField
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.AuthViewModel

// Định nghĩa màu sắc cho chủ đề trắng đen
private val BlackWhiteThemeColors = object {
    val Background = Color.White
    val BackgroundGradientEnd = Color(0xFFE0E0E0) // Xám nhạt thay vì xanh nhạt
    val TextPrimary = Color.Black
    val TextSecondary = Color(0xFF505050) // Xám đậm
    val Accent = Color.Black
    val FormBackground = Color(0xFFF0F0F0) // Xám rất nhạt
    val ButtonBackground = Color.Black
    val ButtonText = Color.White
    val BorderColor = Color(0xFFCCCCCC) // Xám nhạt cho viền
    val LinkColor = Color.Black
}


@Composable
fun LoginScreen(navController: NavController) {

    val viewModel: AuthViewModel = hiltViewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BlackWhiteThemeColors.Background,
                        BlackWhiteThemeColors.BackgroundGradientEnd
                    )
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Thêm Logo ứng dụng
            Box(
                modifier = Modifier
                    .padding(top = 40.dp, bottom = 20.dp)
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.logo),
//                    contentDescription = "App Logo",
//                    modifier = Modifier.size(100.dp),
//                    tint = Color.Unspecified
//                )

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(100.dp)
                )
            }

            Text(
                "Welcome",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = BlackWhiteThemeColors.TextPrimary
            )
            Text(
                "Sign in to start",
                color = BlackWhiteThemeColors.TextSecondary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
            GoogleSignInButton(viewModel)

            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text(
                    "Haven't account?",
                    color = BlackWhiteThemeColors.TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Sign up!",
                    color = BlackWhiteThemeColors.LinkColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate(Screens.SignUpScreen.route)
                    }
                )
            }
        }

        LoginForm(navController = navController, viewModel = viewModel)
    }
}

@Composable
fun LoginForm(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(viewModel.loginState, viewModel.isLoginSuccessful) {
        if (viewModel.loginState.isNotEmpty()) {
            Toast.makeText(context, viewModel.loginState, Toast.LENGTH_SHORT).show()
        }

        if (viewModel.isLoginSuccessful) {
            navController.navigate(Screens.HistoryScreen.route) {
                popUpTo(Screens.LoginScreen.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = BlackWhiteThemeColors.FormBackground,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            StyledTextField(
                value = email,
                onValueChange = {
                    email = it
                    showError = false
                },
                label = "Email",
                hasError = showError && email.isBlank()
            )

            Spacer(modifier = Modifier.height(10.dp))

            StyledTextField(
                value = password,
                onValueChange = {
                    password = it
                    showError = false
                },
                label = "Password",
                isPassword = true,
                hasError = showError && password.isBlank()
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = { navController.navigate(Screens.ForgotPasswordScreen.route) },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BlackWhiteThemeColors.LinkColor
                )
            ) {
                Text("Forgot Password?")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val isEmailValid = isValidEmail(email)
                    showError = email.isBlank() || password.isBlank() || !isEmailValid
                    
                    if (email.isBlank() || password.isBlank()) {
                        // Hiển thị thông báo lỗi chung
                    } else if (!isEmailValid) {
                        // Hiển thị thông báo lỗi định dạng email
                        Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("Login", "Email: $email, Password: $password")
                        viewModel.login(email.trim(), password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlackWhiteThemeColors.ButtonBackground
                )
            ) {
                Text("Continue", color = BlackWhiteThemeColors.ButtonText)
            }
        }
    }
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(navController = rememberNavController())
}
