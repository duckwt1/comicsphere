package com.android.dacs3.presentations.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.android.dacs3.R
import com.android.dacs3.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.util.Log
import android.widget.Toast

@Composable
fun GoogleSignInButton(viewModel: AuthViewModel) {
    val context = LocalContext.current
    
    // Cấu hình Google Sign In
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    googleSignInClient.signOut() // Đăng xuất nếu đã đăng nhập trước đó

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GoogleSignIn", "Result received: resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("GoogleSignIn", "Result OK, getting account info")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleSignIn", "Account retrieved: ${account?.email}")
                account?.let {
                    Log.d("GoogleSignIn", "Calling viewModel.signInWithGoogle")
                    viewModel.signInWithGoogle(it)
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Failed to get account", e)
                // Hiển thị thông báo lỗi cho người dùng
                Toast.makeText(
                    context,
                    "Google Sign-In failed: ${e.statusCode} - ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Log.d("GoogleSignIn", "Sign-in canceled by user")
            Toast.makeText(context, "Sign-in canceled", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("GoogleSignIn", "Sign-in failed with resultCode: ${result.resultCode}")
            Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    OutlinedButton(
        onClick = { 
            launcher.launch(googleSignInClient.signInIntent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.Gray),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.Black
        )
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


