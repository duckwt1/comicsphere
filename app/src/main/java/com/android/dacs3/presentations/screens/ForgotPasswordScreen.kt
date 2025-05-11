package com.android.dacs3.presentations.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.dacs3.R
import com.android.dacs3.presentations.components.StyledTextField
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.AuthViewModel

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
    val CardBackground = Color(0xFFF5F5F5) // Màu nền cho card
    val ErrorColor = Color.Red
}

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val viewModel: AuthViewModel = hiltViewModel()
    val context = LocalContext.current

    LaunchedEffect(viewModel.resetPasswordState) {
        if (viewModel.resetPasswordState.isNotEmpty()) {
            Toast.makeText(context, viewModel.resetPasswordState, Toast.LENGTH_SHORT).show()
        }

        if (viewModel.isResetEmailSent) {
            // Navigate back to login after successful email send
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(Screens.ForgotPasswordScreen.route) { inclusive = true }
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

        // Header với icon (tùy chọn)
        if (true) { // Đặt thành true nếu bạn muốn hiển thị icon
            Icon(
                painter = painterResource(id = R.drawable.ic_lock_reset),
                contentDescription = "Reset Password Icon",
                modifier = Modifier.size(60.dp),
                tint = BlackWhiteThemeColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            "Reset Password",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = BlackWhiteThemeColors.TextPrimary
        )

        Text(
            "Enter your email to receive a password reset link",
            fontSize = 16.sp,
            color = BlackWhiteThemeColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = BlackWhiteThemeColors.CardBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                StyledTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        showError = false
                    },
                    label = "Email",
                    hasError = showError && (email.isBlank() || !isValidEmail(email))
                )

                if (showError && email.isBlank()) {
                    Text(
                        "Email is required",
                        color = BlackWhiteThemeColors.ErrorColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                } else if (showError && !isValidEmail(email)) {
                    Text(
                        "Please enter a valid email address",
                        color = BlackWhiteThemeColors.ErrorColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showError = email.isBlank() || !isValidEmail(email)
                        if (!showError) {
                            viewModel.resetPassword(email)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteThemeColors.ButtonBackground
                    )
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BlackWhiteThemeColors.ButtonText
                        )
                    } else {
                        Text("Send Reset Link", color = BlackWhiteThemeColors.ButtonText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BlackWhiteThemeColors.TextSecondary
                    )
                ) {
                    Text("Back to Login")
                }
            }
        }
    }
}

// Hàm kiểm tra email hợp lệ
fun isValidEmail(email: String): Boolean {
    val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    return emailRegex.matches(email)
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen(navController = rememberNavController())
}