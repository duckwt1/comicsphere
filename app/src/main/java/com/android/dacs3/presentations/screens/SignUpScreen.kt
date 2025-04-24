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
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.AuthViewModel

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

    LaunchedEffect(viewModel.loginState) {
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
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Welcome", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("Sign up to start", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        googleSignInButton()

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))
                    ),
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
                    Text("Passwords do not match", color = Color.Red, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Continue", color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))
                // Have account? Login
                TextButton(
                    onClick = { navController.navigate(Screens.LoginScreen.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Have an account? Login", color = Color.Gray)
                }
            }
        }
    }
}


@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    hasError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                fontWeight = FontWeight.Normal,
                color = if (hasError) Color.Red else Color.DarkGray
            )
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(
            fontWeight = FontWeight.Normal,
            color = Color.Black
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        isError = hasError,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0x40FFFFFF),
            focusedContainerColor = Color(0x55FFFFFF),
            unfocusedTextColor = Color.Black,
            focusedTextColor = Color.Black,
            errorTextColor = Color.Red,
            focusedBorderColor = if (hasError) Color.Red else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (hasError) Color.Red else Color.Gray,
            errorBorderColor = Color.Red,
        )
    )
}



@Composable
fun googleSignInButton() {
    OutlinedButton(
        onClick = { /* Google login */ },
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
fun PreviewSignUpScreen() {
    // Dùng Navigation giả để preview
    SignUpScreen(navController = rememberNavController())
}
