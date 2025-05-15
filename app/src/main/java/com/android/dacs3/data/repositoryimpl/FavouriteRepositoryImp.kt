package com.android.dacs3.data.repositoryimpl

import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.repository.FavouriteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavouriteRepositoryImp @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val api: com.android.dacs3.data.api.MangaDexApi
) : FavouriteRepository {
    
    // Số lượng tối đa truyện yêu thích cho người dùng không phải VIP
    private val MAX_FREE_FAVOURITES = 3
    
    override suspend fun getFavourites(userId: String): Result<List<String>> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val favRef = db.collection("users")
                .document(userId)
                .collection("favourite")

            val snapshot = favRef.get().await()
            val favourites = snapshot.documents.mapNotNull { it.getString("mangaId") }

            Result.success(favourites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMangaById(id: String): Result<MangaData> {
        return try {
            val response = api.getMangaById(id)
            Result.success(response.data) // Lấy MangaData từ response
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addFavourite(userId: String, mangaId: String): Result<Boolean> {
        return try {
            val db = FirebaseFirestore.getInstance()
            
            // Kiểm tra xem người dùng có phải VIP không
            val userDoc = db.collection("users").document(userId).get().await()
            val isVip = userDoc.getBoolean("isVip") ?: false
            val vipExpireDate = when (val expireDate = userDoc.get("vipExpireDate")) {
                is com.google.firebase.Timestamp -> expireDate.toDate().time
                is Long -> expireDate
                else -> 0L
            }
            
            // Kiểm tra xem VIP có còn hiệu lực không
            val isVipValid = isVip && vipExpireDate > System.currentTimeMillis()
            
            // Nếu không phải VIP, kiểm tra số lượng truyện yêu thích
            if (!isVipValid) {
                val favSnapshot = db.collection("users")
                    .document(userId)
                    .collection("favourite")
                    .get()
                    .await()
                
                // Nếu đã đạt giới hạn, trả về lỗi
                if (favSnapshot.size() >= MAX_FREE_FAVOURITES) {
                    return Result.failure(Exception("Đã đạt giới hạn số lượng truyện yêu thích. Nâng cấp lên VIP để thêm không giới hạn!"))
                }
            }
            
            // Tiếp tục thêm vào danh sách yêu thích
            val favRef = db.collection("users")
                .document(userId)
                .collection("favourite")
                .document(mangaId)

            val data = mapOf("mangaId" to mangaId)
            favRef.set(data).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun removeFavourite(userId: String, mangaId: String): Result<Boolean> {
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .document(userId)
                .collection("favourite")
                .document(mangaId)
                .delete()
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllFavourites(userId: String): Result<Boolean> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val favRef = db.collection("users")
                .document(userId)
                .collection("favourite")

            val snapshot = favRef.get().await()
            val batch = db.batch()
            
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
