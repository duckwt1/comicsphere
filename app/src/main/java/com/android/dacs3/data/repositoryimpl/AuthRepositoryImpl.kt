package com.android.dacs3.data.repositoryimpl

import com.android.dacs3.data.model.User
import com.android.dacs3.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    override suspend fun login(username: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(username, password).await()
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
                val user = document.toObject(User::class.java)
                Result.success(user!!)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
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

