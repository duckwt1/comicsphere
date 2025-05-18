package com.android.dacs3.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.model.User
import com.android.dacs3.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    init {
        loadAllUsers()
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.getAllUsers()
                    .onSuccess { userList ->
                        _users.value = userList
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to load users"
                        Log.e("AdminViewModel", "Error loading users", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception loading users", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun getFilteredUsers(): List<User> {
        val query = searchQuery.trim().lowercase()
        return if (query.isEmpty()) {
            _users.value
        } else {
            _users.value.filter {
                it.email.lowercase().contains(query) ||
                it.fullname.lowercase().contains(query) ||
                it.nickname.lowercase().contains(query)
            }
        }
    }

    fun toggleVipStatus(user: User, months: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val calendar = Calendar.getInstance()
                
                // Nếu đang là VIP, hủy VIP
                if (user.isVip) {
                    adminRepository.updateUserVipStatus(user.uid, false, 0)
                        .onSuccess {
                            loadAllUsers() // Tải lại danh sách người dùng
                        }
                        .onFailure { e ->
                            _errorMessage.value = e.message ?: "Failed to update VIP status"
                        }
                } else {
                    // Nếu chưa là VIP, thêm thời hạn VIP
                    calendar.add(Calendar.MONTH, months)
                    val expireDate = calendar.timeInMillis
                    
                    adminRepository.updateUserVipStatus(user.uid, true, expireDate)
                        .onSuccess {
                            loadAllUsers() // Tải lại danh sách người dùng
                        }
                        .onFailure { e ->
                            _errorMessage.value = e.message ?: "Failed to update VIP status"
                        }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleAdminStatus(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.updateUserAdminStatus(user.uid, !user.isAdmin)
                    .onSuccess {
                        loadAllUsers() // Tải lại danh sách người dùng
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to update admin status"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.deleteUser(user.uid)
                    .onSuccess {
                        loadAllUsers() // Tải lại danh sách người dùng
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to delete user"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserInfo(user: User, fullname: String, nickname: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.updateUserInfo(user.uid, fullname, nickname)
                    .onSuccess {
                        loadAllUsers() // Tải lại danh sách người dùng
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to update user info"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
