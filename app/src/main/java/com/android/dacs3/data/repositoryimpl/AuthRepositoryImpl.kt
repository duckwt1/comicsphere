package com.android.dacs3.data.repositoryimpl

import com.android.dacs3.data.model.User
import com.android.dacs3.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    override suspend fun login(username: String, password: String): Result<Boolean> {
        return try {
            // Trim email để loại bỏ khoảng trắng thừa
            val trimmedEmail = username.trim()
            firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun signup(
        email: String,
        password: String,
        fullname: String,
        nickname: String,
        avatarUrl: String
    ): Result<Boolean> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID is null")

            val userInfo = mapOf(
                "email" to email,
                "fullname" to fullname,
                "nickname" to nickname,
                "avatar" to avatarUrl,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(userInfo)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserInfo(userId: String): Result<User> {
        return try {
            val document = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                // Lấy dữ liệu từ document
                val uid = document.id
                val email = document.getString("email") ?: ""
                val fullname = document.getString("fullname") ?: ""
                val nickname = document.getString("nickname") ?: ""
                val avatar = document.getString("avatar") ?: ""
                val createdAt = document.getTimestamp("createdAt")
                val isVip = document.getBoolean("isVip") ?: false
                val isAdmin = document.getBoolean("isAdmin") ?: false
                
                // Xử lý vipExpireDate có thể là Timestamp hoặc Long
                val vipExpireDate = when (val expireDate = document.get("vipExpireDate")) {
                    is com.google.firebase.Timestamp -> expireDate.toDate().time
                    is Long -> expireDate
                    else -> 0L
                }
                
                val user = User(
                    email = email,
                    fullname = fullname,
                    nickname = nickname,
                    avatar = avatar,
                    createdAt = createdAt,
                    isVip = isVip,
                    vipExpireDate = vipExpireDate,
                    isAdmin = isAdmin,
                )
                
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Error getting user info", e)
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Authentication failed")
            
            // Kiểm tra xem user đã tồn tại trong Firestore chưa
            try {
                val userResult = getUserInfo(firebaseUser.uid)
                if (userResult.isSuccess) {
                    // User đã tồn tại, trả về thông tin
                    return userResult
                }
            } catch (e: Exception) {
                // User chưa tồn tại, tiếp tục tạo mới
            }
            
            // Tạo user mới từ thông tin Google
            val newUser = User(
                email = firebaseUser.email ?: "",
                fullname = firebaseUser.displayName ?: "",
                nickname = firebaseUser.displayName?.split(" ")?.lastOrNull() ?: "",
                avatar = firebaseUser.photoUrl?.toString() ?: ""
            )
            
            // Lưu thông tin user vào Firestore
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(newUser)
                .await()
            
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


