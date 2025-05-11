package com.android.dacs3.presentations.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.android.dacs3.R

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
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    hasError: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = BlackWhiteThemeColors.TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BlackWhiteThemeColors.Accent,
            unfocusedBorderColor = BlackWhiteThemeColors.BorderColor,
            errorBorderColor = Color.Red,
            focusedTextColor = BlackWhiteThemeColors.TextPrimary,
            unfocusedTextColor = BlackWhiteThemeColors.TextPrimary,
            cursorColor = BlackWhiteThemeColors.Accent
        ),
        isError = hasError,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (passwordVisible)
                                R.drawable.ic_visibility_off
                            else
                                R.drawable.ic_visibility
                        ),
                        modifier = Modifier.size(20.dp),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = BlackWhiteThemeColors.TextSecondary
                    )
                }
            }
        }
    )
}
