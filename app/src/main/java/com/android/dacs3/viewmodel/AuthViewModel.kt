package com.android.dacs3.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.model.User
import com.android.dacs3.data.repository.AuthRepository
import com.android.dacs3.data.repository.CloudinaryRepository
import com.android.dacs3.utliz.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context
import com.android.dacs3.R
import com.android.dacs3.utliz.AdminConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val cloudinaryRepository: CloudinaryRepository
) : ViewModel() {

    var loginState by mutableStateOf("")
        private set

    var isLoginSuccessful by mutableStateOf(false)
        private set

    var currentUser by mutableStateOf<User?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isUpdatingAvatar by mutableStateOf(false)
        private set

    var resetPasswordState by mutableStateOf("")
        private set

    var isResetEmailSent by mutableStateOf(false)
        private set

    var updateProfileState by mutableStateOf("")
        private set

    var isProfileUpdateSuccessful by mutableStateOf(false)
        private set

    init {
        loadCurrentUser()
    }

    // Thêm kiểm tra admin khi load user
    fun checkAdminStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email
            if (email == AdminConfig.ADMIN_EMAIL) {
                // Cập nhật trạng thái admin trong Firestore nếu cần
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .update("isAdmin", true)
            }
        }
    }

    fun loadCurrentUser() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            isLoading = true
            viewModelScope.launch {
                try {
                    authRepository.getUserInfo(firebaseUser.uid)
                        .onSuccess { user ->
                            currentUser = user
                            // Kiểm tra nếu email trùng với admin email
                            if (user.email == AdminConfig.ADMIN_EMAIL) {
                                // Cập nhật trạng thái admin
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(firebaseUser.uid)
                                    .update("isAdmin", true)
                                    .addOnSuccessListener {
                                        currentUser = currentUser?.copy(isAdmin = true)
                                    }
                            }
                        }
                        .onFailure { e ->
                            loginState = e.message ?: "Failed to load user data"
                        }
                } catch (e: Exception) {
                    loginState = e.message ?: "An error occurred"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun updateAvatar(imageUri: Uri) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            loginState = "User not logged in"
            return
        }

        viewModelScope.launch {
            isUpdatingAvatar = true
            try {
                // Upload image to Cloudinary
                cloudinaryRepository.uploadImage(imageUri)
                    .onSuccess { imageUrl ->
                        // Update user document in Firestore
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(firebaseUser.uid)
                            .update("avatar", imageUrl)
                            .await()

                        // Reload user data to get the latest information
                        authRepository.getUserInfo(firebaseUser.uid)
                            .onSuccess { user ->
                                currentUser = user
                                loginState = ""
                            }
                            .onFailure { e ->
                                loginState = e.message ?: "Failed to reload user data"
                            }
                    }
                    .onFailure { e ->
                        loginState = e.message ?: "Failed to upload image"
                    }
            } catch (e: Exception) {
                loginState = e.message ?: "Failed to update avatar"
            } finally {
                isUpdatingAvatar = false
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            if (username.isBlank() || password.isBlank()) {
                loginState = "Please enter all fields"
                return@launch
            }
            
            // Kiểm tra định dạng email
            if (!isValidEmail(username)) {
                loginState = "Invalid email format"
                return@launch
            }

            // Kiểm tra xem có phải admin không
            if (username == AdminConfig.ADMIN_EMAIL && password == AdminConfig.ADMIN_PASSWORD) {
                // Đăng nhập với tài khoản admin
                val result = authRepository.login(username, password)
                if (result.isSuccess) {
                    loginState = "Admin login successful"
                    isLoginSuccessful = true
                    sessionManager.saveLoginState(true)
                    
                    // Cập nhật trạng thái admin trong Firestore
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(firebaseUser.uid)
                            .update("isAdmin", true)
                            .addOnSuccessListener {
                                // Tải lại thông tin người dùng để cập nhật isAdmin
                                loadCurrentUser()
                            }
                    }
                } else {
                    loginState = result.exceptionOrNull()?.message ?: "Admin login failed"
                    isLoginSuccessful = false
                }
                return@launch
            }

            // Đăng nhập thông thường
            val result = authRepository.login(username, password)
            if (result.isSuccess) {
                loginState = "Login successful"
                isLoginSuccessful = true
                sessionManager.saveLoginState(true)
                loadCurrentUser()
            } else {
                loginState = result.exceptionOrNull()?.message ?: "Login failed"
                isLoginSuccessful = false
            }
        }
    }

    fun signup(
        email: String,
        password: String,
        fullname: String = "",
        nickname: String = "",
        avatarUrl: String = ""
    ) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank() || fullname.isBlank() || nickname.isBlank()) {
                loginState = "Please enter all fields"
                return@launch
            }

            val result = authRepository.signup(email, password, fullname, nickname, avatarUrl)
            if (result.isSuccess) {
                loginState = "Signup successful"
                isLoginSuccessful = true
                loadCurrentUser()
            } else {
                loginState = result.exceptionOrNull()?.message ?: "Signup failed"
                isLoginSuccessful = false
            }
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            try {
                // Đăng xuất khỏi Firebase
                FirebaseAuth.getInstance().signOut()
                
                // Đăng xuất khỏi Google Sign-In
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                
                // Đăng xuất khỏi Google
                googleSignInClient.signOut().await()
                
                // Cập nhật trạng thái
                sessionManager.saveLoginState(false)
                loginState = "Logged out"
                isLoginSuccessful = false
                currentUser = null
                
                Log.d("AuthViewModel", "Logged out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during logout", e)
                loginState = "Error during logout: ${e.message}"
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            if (email.isBlank()) {
                resetPasswordState = "Please enter your email"
                return@launch
            }
            
            isLoading = true
            try {
                authRepository.sendPasswordResetEmail(email)
                    .onSuccess {
                        resetPasswordState = "Password reset email sent"
                        isResetEmailSent = true
                    }
                    .onFailure { e ->
                        resetPasswordState = e.message ?: "Failed to send reset email"
                        isResetEmailSent = false
                    }
            } catch (e: Exception) {
                resetPasswordState = e.message ?: "An unexpected error occurred"
                isResetEmailSent = false
            } finally {
                isLoading = false
            }
        }
    }

    fun clearResetPasswordState() {
        resetPasswordState = ""
        isResetEmailSent = false
    }

    fun updateUserProfile(fullname: String, nickname: String) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            updateProfileState = "User not logged in"
            isProfileUpdateSuccessful = false
            return
        }

        viewModelScope.launch {
            isLoading = true
            try {
                // Update user document in Firestore
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(firebaseUser.uid)
                    .update(
                        mapOf(
                            "fullname" to fullname,
                            "nickname" to nickname
                        )
                    )
                    .await()

                // Reload user data to get the latest information
                authRepository.getUserInfo(firebaseUser.uid)
                    .onSuccess { user ->
                        currentUser = user
                        updateProfileState = "Profile updated successfully"
                        isProfileUpdateSuccessful = true
                    }
                    .onFailure { e ->
                        updateProfileState = e.message ?: "Failed to reload user data"
                        isProfileUpdateSuccessful = false
                    }
            } catch (e: Exception) {
                updateProfileState = e.message ?: "Failed to update profile"
                isProfileUpdateSuccessful = false
            } finally {
                isLoading = false
            }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            isLoading = true
            try {
                Log.d("AuthViewModel", "Starting Google sign-in with token: ${account.idToken?.take(10)}...")
                authRepository.signInWithGoogle(account.idToken ?: "")
                    .onSuccess { user ->
                        currentUser = user
                        loginState = "Login successful"
                        isLoginSuccessful = true
                        sessionManager.saveLoginState(true)
                        Log.d("AuthViewModel", "Google sign-in successful, user: ${user.email}")
                    }
                    .onFailure { e ->
                        loginState = e.message ?: "Google sign-in failed"
                        isLoginSuccessful = false
                        Log.e("AuthViewModel", "Google sign-in failed", e)
                    }
            } catch (e: Exception) {
                loginState = e.message ?: "Google sign-in failed"
                isLoginSuccessful = false
                Log.e("AuthViewModel", "Exception during Google sign-in", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Hàm kiểm tra email hợp lệ
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        return emailRegex.matches(email)
    }
}
