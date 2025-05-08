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
import com.android.dacs3.data.service.CloudinaryService
import com.android.dacs3.utliz.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val cloudinaryService: CloudinaryService
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

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            viewModelScope.launch {
                isLoading = true
                try {
                    authRepository.getUserInfo(firebaseUser.uid)
                        .onSuccess { user ->
                            currentUser = user
                            Log.d("AuthViewModel", "User info loaded: $user")
                        }
                        .onFailure { e ->
                            loginState = e.message ?: "Failed to load user info"
                        }
                } catch (e: Exception) {
                    loginState = e.message ?: "An unexpected error occurred"
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
                val imageUrl = cloudinaryService.uploadImage(imageUri)
                
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

    fun logout() {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            sessionManager.saveLoginState(false)
            loginState = "Logged out"
            isLoginSuccessful = false
            currentUser = null
        }
    }
}