package com.android.dacs3.data.repository

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Boolean>
    suspend fun signup(
        email: String,
        password: String,
        fullname: String,
        nickname: String,
        avatarUrl: String
    ): Result<Boolean>
}