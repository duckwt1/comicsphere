package com.android.dacs3.data.repositoryimpl

import android.util.Log
import com.android.dacs3.data.model.MangaAttributes
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.Relationship
import com.android.dacs3.data.model.RelationshipAttributes
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
            
            // Nếu API không thành công, thử lấy từ Firestore
            val mangaDoc = FirebaseFirestore.getInstance()
                .collection("manga")
                .document(id)
                .get()
                .await()
            
            if (!mangaDoc.exists()) {
                return Result.failure(Exception("Manga not found"))
            }
            
            // Xử lý ảnh bìa - ưu tiên coverImageUrl
            val coverImageUrl = mangaDoc.getString("coverImageUrl") ?: ""
            val coverUrl = mangaDoc.getString("coverUrl") ?: ""
            val coverFileName = mangaDoc.getString("coverFileName") ?: ""
            
            // Xác định URL cuối cùng theo thứ tự ưu tiên
            val finalCoverUrl = when {
                // 1. Ưu tiên coverImageUrl nếu có
                coverImageUrl.isNotEmpty() -> coverImageUrl
                // 2. Thử coverUrl nếu có
                coverUrl.isNotEmpty() && coverUrl.startsWith("http") -> coverUrl
                // 3. Nếu có coverFileName, tạo URL từ ID và fileName
                coverFileName.isNotEmpty() -> "https://uploads.mangadex.org/covers/$id/$coverFileName.512.jpg"
                // 4. Nếu không có gì, sử dụng placeholder
                else -> "https://via.placeholder.com/512x768?text=No+Cover"
            }
            
            // Tạo đối tượng MangaData với finalCoverUrl trong relationships
            val mangaData = MangaData(
                id = id,
                attributes = MangaAttributes(
                    title = (mangaDoc.get("title") as? Map<String, String>) 
                        ?: mapOf("en" to (mangaDoc.getString("title") ?: "Unknown")),
                    description = mapOf("en" to (mangaDoc.getString("description") ?: "")),
                    status = mangaDoc.getString("status") ?: "",
                    availableTranslatedLanguages = listOf("en"),
                    altTitles = emptyList(),
                    tags = emptyList()
                ),
                relationships = listOf(
                    Relationship(
                        id = id,
                        type = "cover_art",
                        attributes = RelationshipAttributes(
                            fileName = finalCoverUrl  // Sử dụng finalCoverUrl đã xử lý
                        )
                    )
                )
            )
            
            Result.success(mangaData)
        } catch (e: Exception) {
            Log.e("FavouriteRepositoryImp", "Error getting manga by ID", e)
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
