package com.android.dacs3.data.repositoryimpl

import android.util.Log
import com.android.dacs3.data.model.User
import com.android.dacs3.data.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : AdminRepository {

    private val TAG = "AdminRepositoryImpl"

    override suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                user?.copy(uid = doc.id) // Đảm bảo ID được thiết lập
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all users", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserVipStatus(
        userId: String,
        isVip: Boolean,
        vipExpireDate: Long
    ): Result<Boolean> {
        return try {
            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "isVip" to isVip,
                        "vipExpireDate" to vipExpireDate
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user VIP status", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserAdminStatus(userId: String, isAdmin: Boolean): Result<Boolean> {
        return try {
            firestore.collection("users").document(userId)
                .update("isAdmin", isAdmin)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user admin status", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Boolean> {
        return try {
            // Xóa dữ liệu người dùng từ Firestore
            firestore.collection("users").document(userId).delete().await()
            
            // Lưu ý: Để xóa tài khoản từ Firebase Authentication, bạn cần sử dụng Firebase Admin SDK
            // hoặc Cloud Functions. Ở đây chúng ta chỉ xóa dữ liệu từ Firestore
            // Bạn có thể thêm một Cloud Function để xử lý việc xóa tài khoản Authentication
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserInfo(userId: String, fullname: String, nickname: String): Result<Boolean> {
        return try {
            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "fullname" to fullname,
                        "nickname" to nickname
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user info", e)
            Result.failure(e)
        }
    }
}

