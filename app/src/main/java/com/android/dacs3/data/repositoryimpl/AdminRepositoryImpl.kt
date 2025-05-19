package com.android.dacs3.data.repositoryimpl

import android.util.Log
import com.android.dacs3.data.model.MangaAttributes
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.Relationship
import com.android.dacs3.data.model.RelationshipAttributes
import com.android.dacs3.data.model.Tag
import com.android.dacs3.data.model.TagAttributes
import com.android.dacs3.data.model.TagWrapper
import com.android.dacs3.data.model.User
import com.android.dacs3.data.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID
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

    override suspend fun getAllMangas(): Result<List<MangaData>> {
        return try {
            // First get all tags for reference
            val tagsResult = getAllTags()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Get manga documents
            val querySnapshot = firestore.collection("manga")
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Log.d(TAG, "Fetched ${querySnapshot.documents.size} manga documents from Firestore")
            
            // Convert documents to MangaData objects
            val mangaList = querySnapshot.documents.mapNotNull { document ->
                try {
                    val id = document.id
                    val title = document.get("title") as? Map<String, String> 
                        ?: mapOf("en" to (document.getString("title") ?: "Unknown"))
                    val description = document.get("description") as? Map<String, String> 
                        ?: mapOf("en" to (document.getString("description") ?: ""))
                    val status = document.getString("status") ?: "ongoing"
                    
                    // Handle author field - could be string or list
                    val author = document.getString("author") ?: 
                                (document.get("authors") as? List<*>)?.joinToString(", ") { it.toString() } ?: 
                                "Unknown Author"
                    
                    // Get cover URL
                    val coverImageUrl = document.getString("coverImageUrl") ?: ""
                    val coverUrl = document.getString("coverUrl") ?: ""

                    // Xác định URL cuối cùng
                    val finalCoverUrl = when {
                        coverImageUrl.isNotEmpty() -> coverImageUrl
                        coverUrl.isNotEmpty() && coverUrl.startsWith("http") -> coverUrl
                        else -> "https://via.placeholder.com/512x768?text=No+Cover"
                    }

                    
                    // Get tag IDs and convert to Tag objects
                    val tagIds = document.get("tagIds") as? List<String> ?: emptyList()
                    val tags = tagIds.mapNotNull { tagId -> 
                        tagMap[tagId]?.let { tag ->
                            TagWrapper(
                                id = tag.id,
                                type = "tag",
                                attributes = TagAttributes(
                                    name = mapOf("en" to tag.name),
                                    group = tag.group,
                                    description = emptyMap()
                                )
                            )
                        }
                    }
                    
                    // Create MangaData object
                    MangaData(
                        id = id,
                        attributes = MangaAttributes(
                            title = title,
                            description = description,
                            status = status,
                            availableTranslatedLanguages = listOf("en"),
                            altTitles = emptyList(),
                            tags = tags,
                            author = author
                        ),
                        relationships = listOf(
                            Relationship(
                                id = id,
                                type = "cover_art",
                                attributes = RelationshipAttributes(
                                    fileName = finalCoverUrl
                                )
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing manga document: ${document.id}", e)
                    null
                }
            }
            
            Result.success(mangaList)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all mangas", e)
            Result.failure(e)
        }
    }

    override suspend fun getMangaById(mangaId: String): Result<MangaData?> {
        return try {
            val document = firestore.collection("manga").document(mangaId).get().await()
            
            if (!document.exists()) {
                return Result.success(null)
            }
            
            // Get tags for reference
            val tagsResult = getAllTags()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Convert document to MangaData
            val id = document.id
            val title = document.get("title") as? Map<String, String> 
                ?: mapOf("en" to (document.getString("title") ?: "Unknown"))
            val description = document.get("description") as? Map<String, String> 
                ?: mapOf("en" to (document.getString("description") ?: ""))
            val status = document.getString("status") ?: "ongoing"
            
            // Handle author field
            val author = document.getString("author") ?: 
                        (document.get("authors") as? List<*>)?.joinToString(", ") { it.toString() } ?: 
                        "Unknown Author"
            
            // Get cover URL
            val coverUrl = document.getString("coverUrl") ?: ""
            
            // Get tag IDs and convert to Tag objects
            val tagIds = document.get("tagIds") as? List<String> ?: emptyList()
            val tags = tagIds.mapNotNull { tagId -> 
                tagMap[tagId]?.let { tag ->
                    TagWrapper(
                        id = tag.id,
                        type = "tag",
                        attributes = TagAttributes(
                            name = mapOf("en" to tag.name),
                            group = tag.group,
                            description = emptyMap()
                        )
                    )
                }
            }
            
            // Create MangaData object
            val mangaData = MangaData(
                id = id,
                attributes = MangaAttributes(
                    title = title,
                    description = description,
                    status = status,
                    availableTranslatedLanguages = listOf("en"),
                    altTitles = emptyList(),
                    tags = tags,
                    author = author
                ),
                relationships = listOf(
                    Relationship(
                        id = id,
                        type = "cover_art",
                        attributes = RelationshipAttributes(
                            fileName = coverUrl
                        )
                    )
                )
            )
            
            Result.success(mangaData)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting manga by ID", e)
            Result.failure(e)
        }
    }

    override suspend fun addManga(
        title: String, 
        description: String, 
        coverUrl: String, 
        status: String, 
        author: String, 
        tagIds: List<String>
    ): Result<String> {
        return try {
            val mangaId = UUID.randomUUID().toString()
            
            // Process author string - split by commas
            val authorList = author.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            val mangaData = hashMapOf(
                "title" to title,
                "description" to title,
                "coverUrl" to coverUrl,
                "status" to status,
                "authors" to (if (authorList.isEmpty()) listOf("Unknown Author") else authorList),
                "tagIds" to tagIds,
                "availableLanguages" to listOf("en"),
                "lastUpdated" to System.currentTimeMillis(),
                "viewCount" to 0
            )
            
            firestore.collection("manga").document(mangaId)
                .set(mangaData)
                .await()
            
            Result.success(mangaId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding manga", e)
            Result.failure(e)
        }
    }

    override suspend fun updateManga(
        mangaId: String,
        title: String, 
        description: String, 
        coverUrl: String, 
        status: String, 
        author: String, 
        tagIds: List<String>
    ): Result<Boolean> {
        return try {
            // Process author string - split by commas
            val authorList = author.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            val mangaData = hashMapOf(
                "title" to title,
                "description" to description,
                "coverImageUrl" to coverUrl,
                "status" to status,
                "author" to author,
                "authors" to (if (authorList.isEmpty()) listOf("Unknown Author") else authorList),
                "tagIds" to tagIds,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection("manga").document(mangaId)
                .update(mangaData)
                .await()
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating manga", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteManga(mangaId: String): Result<Boolean> {
        return try {
            // Delete manga document
            firestore.collection("manga").document(mangaId)
                .delete()
                .await()
            
            // Also delete all chapters for this manga
            val chaptersSnapshot = firestore.collection("chapters")
                .whereEqualTo("mangaId", mangaId)
                .get()
                .await()
            
            val batch = firestore.batch()
            chaptersSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            if (chaptersSnapshot.documents.isNotEmpty()) {
                batch.commit().await()
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting manga", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllTags(): Result<List<Tag>> {
        return try {
            val querySnapshot = firestore.collection("tags")
                .get()
                .await()
            
            val tags = querySnapshot.documents.mapNotNull { document ->
                try {
                    val id = document.id
                    val name = document.getString("name") ?: return@mapNotNull null
                    val group = document.getString("group") ?: "unknown"
                    
                    Tag(id = id, name = name, group = group)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing tag document: ${document.id}", e)
                    null
                }
            }
            
            Result.success(tags)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all tags", e)
            Result.failure(e)
        }
    }
}



