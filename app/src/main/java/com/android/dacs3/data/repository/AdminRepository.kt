package com.android.dacs3.data.repository

import com.android.dacs3.data.model.User

interface AdminRepository {
    suspend fun getAllUsers(): Result<List<User>>
    suspend fun updateUserVipStatus(userId: String, isVip: Boolean, vipExpireDate: Long): Result<Boolean>
    suspend fun updateUserAdminStatus(userId: String, isAdmin: Boolean): Result<Boolean>
    suspend fun deleteUser(userId: String): Result<Boolean>
    suspend fun updateUserInfo(userId: String, fullname: String, nickname: String): Result<Boolean>
}
