package com.android.dacs3.data.repository

import com.android.dacs3.data.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Boolean>
    suspend fun signup(
        email: String,
        password: String,
        fullname: String,
        nickname: String,
        avatarUrl: String
    ): Result<Boolean>
    suspend fun getUserInfo(userId: String): Result<User>
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean>
}
