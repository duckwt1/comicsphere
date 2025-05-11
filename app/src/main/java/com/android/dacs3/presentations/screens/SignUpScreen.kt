package com.android.dacs3.presentations.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val BackgroundGradientEnd = Color(0xFFE0E0E0) // Xám nhạt
    val TextPrimary = Color.Black
    val TextSecondary = Color(0xFF505050) // Xám đậm
    val Accent = Color.Black
    val FormBackground = Color(0xFFF0F0F0) // Xám rất nhạt
    val ButtonBackground = Color.Black
    val ButtonText = Color.White
    val BorderColor = Color(0xFFCCCCCC) // Xám nhạt cho viền
    val LinkColor = Color.Black
    val ErrorColor = Color(0xFF990000) // Đỏ đậm cho thông báo lỗi
}

@Composable
fun SignUpScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullname by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }
    var passwordMismatch by remember { mutableStateOf(false) }

    val viewModel: AuthViewModel = hiltViewModel()
    val context = LocalContext.current

    LaunchedEffect(viewModel.loginState, viewModel.isLoginSuccessful) {
        if (viewModel.loginState.isNotEmpty()) {
            Toast.makeText(context, viewModel.loginState, Toast.LENGTH_SHORT).show()
        }

        if (viewModel.isLoginSuccessful) {
            navController.navigate(Screens.HistoryScreen.route) {
                popUpTo(Screens.SignUpScreen.route) { inclusive = true }
            }
        }
    }

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
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "Welcome",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = BlackWhiteThemeColors.TextPrimary
        )
        Text(
            "Sign up to start",
            fontSize = 16.sp,
            color = BlackWhiteThemeColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        GoogleSignInButton(viewModel = viewModel)

        Spacer(modifier = Modifier.height(20.dp))

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
                // Sử dụng CustomStyledTextField nếu không có StyledTextField từ component
                // hoặc thay CustomStyledTextField bằng StyledTextField nếu component này đã được định nghĩa
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
                    value = fullname,
                    onValueChange = {
                        fullname = it
                        showError = false
                    },
                    label = "Full Name",
                    hasError = showError && fullname.isBlank()
                )
                Spacer(modifier = Modifier.height(10.dp))

                StyledTextField(
                    value = nickname,
                    onValueChange = {
                        nickname = it
                        showError = false
                    },
                    label = "Nickname",
                    hasError = showError && nickname.isBlank()
                )
                Spacer(modifier = Modifier.height(10.dp))

                StyledTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        showError = false
                        passwordMismatch = false
                    },
                    label = "Password",
                    isPassword = true,
                    hasError = showError && password.isBlank()
                )
                Spacer(modifier = Modifier.height(10.dp))

                StyledTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        showError = false
                        passwordMismatch = false
                    },
                    label = "Confirm Password",
                    isPassword = true,
                    hasError = showError && (confirmPassword.isBlank() || password != confirmPassword)
                )

                if (passwordMismatch) {
                    Text(
                        "Passwords do not match",
                        color = BlackWhiteThemeColors.ErrorColor,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showError = email.isBlank() || password.isBlank() || fullname.isBlank() || nickname.isBlank()
                        passwordMismatch = password != confirmPassword

                        if (!showError && !passwordMismatch) {
                            viewModel.signup(
                                email = email,
                                password = password,
                                fullname = fullname,
                                nickname = nickname,
                                avatarUrl = ""
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(30),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteThemeColors.ButtonBackground
                    )
                ) {
                    Text("Continue", color = BlackWhiteThemeColors.ButtonText)
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { navController.navigate(Screens.LoginScreen.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BlackWhiteThemeColors.TextSecondary
                    )
                ) {
                    Text("Have an account? Login")
                }
            }
        }
    }
}

//@Composable
//fun GoogleSignInButton() {
//    OutlinedButton(
//        onClick = { /* Google login */ },
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(48.dp),
//        shape = RoundedCornerShape(24.dp),
//        border = BorderStroke(1.dp, BlackWhiteThemeColors.BorderColor),
//        colors = ButtonDefaults.outlinedButtonColors(
//            contentColor = BlackWhiteThemeColors.TextPrimary
//        )
//    ) {
//        Icon(
//            painter = painterResource(id = R.drawable.ic_google_logo),
//            contentDescription = "Google",
//            modifier = Modifier.size(20.dp),
//            tint = Color.Unspecified // Giữ logo Google với màu gốc
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        Text("Continue with Google", color = BlackWhiteThemeColors.TextPrimary)
//    }
//}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(navController = rememberNavController())
}
