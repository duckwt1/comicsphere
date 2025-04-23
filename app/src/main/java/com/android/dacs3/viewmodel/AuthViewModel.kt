package com.android.dacs3.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var loginState by mutableStateOf("")
        private set

    var isLoginSuccessful by mutableStateOf(false)
        private set

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
            } else {
                loginState = result.exceptionOrNull()?.message ?: "Signup failed"
                isLoginSuccessful = false
            }
        }
    }

}