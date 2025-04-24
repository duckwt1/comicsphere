package com.android.dacs3.presentations.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFE0E0FF))
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("Sign in to start", color = Color.Gray, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(24.dp))
            GoogleSignInButton()

            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text("Haven't account?", color = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Sign up!",
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate(Screens.SignUpScreen.route)
                    }
                )
            }
        }

        LoginForm(navController = navController)
    }
}

@Composable
fun LoginForm(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val viewModel: AuthViewModel = hiltViewModel()

    val context = LocalContext.current

    LaunchedEffect(viewModel.loginState) {
        if (viewModel.loginState.isNotEmpty()) {
            Toast.makeText(context, viewModel.loginState, Toast.LENGTH_SHORT).show()
        }

        if (viewModel.isLoginSuccessful) {
            navController.navigate(Screens.HistoryScreen.route) { // Ensure this matches the navigation graph
                popUpTo(Screens.LoginScreen.route) { inclusive = true }
            }
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, shape = RoundedCornerShape(20.dp))
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

            Text(
                "Forgot password?",
                color = Color.DarkGray,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* TODO: Forgot password */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    showError = email.isBlank() || password.isBlank()
                    if (!showError) {
                        Log.d("Login", "Email: $email, Password: $password")
                        viewModel.login(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Continue", color = Color.White)
            }
        }
    }
}


@Composable
fun GoogleSignInButton() {
    OutlinedButton(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_google_logo),
            contentDescription = "Google",
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Continue with Google", color = Color.Black)
    }
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(navController = rememberNavController())
}
