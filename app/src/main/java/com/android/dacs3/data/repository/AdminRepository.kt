package com.android.dacs3.data.repository

import com.android.dacs3.data.model.User
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.Tag

interface AdminRepository {
    // Existing user management methods
    suspend fun getAllUsers(): Result<List<User>>
    suspend fun updateUserVipStatus(userId: String, isVip: Boolean, vipExpireDate: Long): Result<Boolean>
    suspend fun updateUserAdminStatus(userId: String, isAdmin: Boolean): Result<Boolean>
    suspend fun deleteUser(userId: String): Result<Boolean>
    suspend fun updateUserInfo(userId: String, fullname: String, nickname: String): Result<Boolean>
    
    // New manga management methods
    suspend fun getAllMangas(): Result<List<MangaData>>
    suspend fun getMangaById(mangaId: String): Result<MangaData?>
    suspend fun addManga(
        title: String, 
        description: String, 
        coverUrl: String, 
        status: String, 
        author: String, 
        tagIds: List<String>
    ): Result<String> // Returns the new manga ID
    
    suspend fun updateManga(
        mangaId: String,
        title: String, 
        description: String, 
        coverUrl: String, 
        status: String, 
        author: String, 
        tagIds: List<String>
    ): Result<Boolean>
    
    suspend fun deleteManga(mangaId: String): Result<Boolean>
    
    // Tag management methods
    suspend fun getAllTags(): Result<List<Tag>>
    
    // Add new method for creating users
    suspend fun createUser(
        email: String,
        password: String,
        fullname: String,
        nickname: String,
        isVip: Boolean = false,
        isAdmin: Boolean = false
    ): Result<String> // Returns the new user ID
}


