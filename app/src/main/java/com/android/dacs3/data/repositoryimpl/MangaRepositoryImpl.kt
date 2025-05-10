package com.android.dacs3.data.repositoryimpl

import com.android.dacs3.data.api.MangaDexApi
import com.android.dacs3.data.model.ChapterContentResponse
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaListResponse
import com.android.dacs3.data.repository.MangaRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.android.dacs3.data.model.Comment
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.ReadingProgress
import com.android.dacs3.data.model.TagWrapper
import com.android.dacs3.data.model.User
import com.google.firebase.auth.FirebaseAuth

@Singleton
class MangaRepositoryImpl @Inject constructor(
    private val api: MangaDexApi,
    private val firestore: FirebaseFirestore
) : MangaRepository {

    override suspend fun fetchMangaList(limit: Int, offset: Int): Result<MangaListResponse> {
        return try {
            val response = api.getMangaList(limit, offset)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchManga(title: String): Result<MangaListResponse> {
        return try {
            val response = api.searchManga(title)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTags(): Result<List<TagWrapper>> {
        return try {
            val response = api.getTags()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMangaById(id: String): Result<MangaDetailResponse> {
        return try {
            val response = api.getMangaById(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMangaChapters(
        mangaId: String,
        language: String,
        limit: Int,
        offset: Int
    ): Result<List<ChapterData>> {
        return try {
            val response = api.getMangaChapters(mangaId, listOf(language), "asc", limit, offset)
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChapterContent(chapterId: String): Result<ChapterContentResponse> {
        return try {
            val response = api.getChapterContent(chapterId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveReadingProgress(
        userId: String,
        mangaId: String,
        chapterId: String,
        language: String,
        lastPageIndex: Int
    ): Result<Boolean> {
        return try {
            val data = mapOf(
                "mangaId" to mangaId,
                "chapterId" to chapterId,
                "language" to language,
                "lastPageIndex" to lastPageIndex,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .collection("readingProgress")
                .document("${mangaId}_${language}_$chapterId")
                .set(data)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastReadChapter(
        userId: String,
        mangaId: String,
        language: String
    ): Result<Pair<String, Int>> {
        return try {
            val querySnapshot = firestore.collection("users")
                .document(userId)
                .collection("readingProgress")
                .whereEqualTo("mangaId", mangaId)
                .whereEqualTo("language", language)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.e("MangaRepositoryImpl", "No reading progress found for mangaId=$mangaId, language=$language")
                return Result.failure(Exception("No reading progress found"))
            }

            val latestDoc = querySnapshot.documents.maxByOrNull {
                it.getLong("timestamp") ?: 0L
            }

            if (latestDoc != null) {
                val chapterId = latestDoc.getString("chapterId") ?: return Result.failure(Exception("No chapterId"))
                val lastPageIndex = latestDoc.getLong("lastPageIndex")?.toInt() ?: 0
                Result.success(chapterId to lastPageIndex)
            } else {
                Result.failure(Exception("No valid reading progress found"))
            }
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error getting last read chapter", e)
            Result.failure(e)
        }
    }

    override suspend fun getReadingProgress(userId: String): Result<List<ReadingProgress>> {
        return try {
            Log.d("MangaRepositoryImpl", "Getting reading progress for user: $userId")

            val querySnapshot = firestore.collection("users")
                .document(userId)
                .collection("readingProgress")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d("MangaRepositoryImpl", "Found ${querySnapshot.documents.size} reading progress documents")

            val progressList = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val mangaId = doc.getString("mangaId")
                    val chapterId = doc.getString("chapterId")
                    val language = doc.getString("language")
                    val lastPageIndex = doc.getLong("lastPageIndex")
                    val timestamp = doc.getLong("timestamp")

                    if (mangaId == null || chapterId == null || language == null || lastPageIndex == null || timestamp == null) {
                        Log.e("MangaRepositoryImpl", "Missing required fields in document: ${doc.id}")
                        return@mapNotNull null
                    }

                    ReadingProgress(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        language = language,
                        lastPageIndex = lastPageIndex.toInt(),
                        timestamp = timestamp
                    )
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing reading progress document ${doc.id}", e)
                    null
                }
            }

            Log.d("MangaRepositoryImpl", "Successfully parsed ${progressList.size} reading progress items")
            Result.success(progressList)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error getting reading progress", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteReadingProgress(
        userId: String,
        mangaId: String,
        chapterId: String,
        language: String
    ): Result<Boolean> {
        return try {
            Log.d("MangaRepositoryImpl", "Deleting reading progress for user=$userId, manga=$mangaId, chapter=$chapterId, language=$language")

            firestore.collection("users")
                .document(userId)
                .collection("readingProgress")
                .document("${mangaId}_${language}_$chapterId")
                .delete()
                .await()

            Log.d("MangaRepositoryImpl", "Successfully deleted reading progress")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error deleting reading progress", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAllMangaReadingProgress(
        userId: String,
        mangaId: String
    ): Result<Boolean> {
        return try {
            Log.d("MangaRepositoryImpl", "Deleting all reading progress for user=$userId, manga=$mangaId")

            val querySnapshot = firestore.collection("users")
                .document(userId)
                .collection("readingProgress")
                .whereEqualTo("mangaId", mangaId)
                .get()
                .await()

            val batch = firestore.batch()
            querySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d("MangaRepositoryImpl", "Successfully deleted all reading progress for manga")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error deleting all reading progress", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchTrendingManga(limit: Int, offset: Int): Result<MangaListResponse> {
        return try {
            val response = api.getTrendingManga(
                limit = limit,
                offset = offset,
                order = "desc"
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun fetchRecommendedManga(
        includedTagIds: List<String>,
        limit: Int,
        offset: Int
    ): Result<MangaListResponse> {
        return try {
            val response = api.getRecommendedManga(
                limit = limit,
                offset = offset,
                includedTags = includedTagIds,
                includedTagsMode = "OR"
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMangaByTags(
        includedTags: List<String>,
        includedTagsMode: String,
        limit: Int,
        offset: Int
    ): Result<MangaListResponse> {
        return try {
            val response = api.getMangaByTags(
                includedTags = includedTags,
                includedTagsMode = includedTagsMode,
                limit = limit,
                offset = offset
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addComment(
        mangaId: String,
        userId: String,
        nickname: String,
        content: String
    ): Result<Boolean> {
        return try {
            // Lấy thông tin người dùng để có avatar
            var userAvatar: String? = null
            try {
                val userResult = getUserInfo(userId)
                if (userResult.isSuccess) {
                    userAvatar = userResult.getOrNull()?.avatar
                }
            } catch (e: Exception) {
                Log.e("MangaRepositoryImpl", "Error getting user avatar", e)
            }

            // Tạo document reference mới
            val commentRef = firestore.collection("manga")
                .document(mangaId)
                .collection("comments")
                .document()
            
            val commentId = commentRef.id
            val timestamp = System.currentTimeMillis()

            val commentData = hashMapOf(
                "userId" to userId,
                "comment" to content,
                "timestamp" to timestamp,
                "likes" to 0,
                "isEdited" to false,
                "nickname" to nickname,
                "avatar" to userAvatar
            )

            commentRef.set(commentData).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error adding comment", e)
            Result.failure(e)
        }
    }


    override suspend fun getComments(mangaId: String): Result<List<Comment>> {
        return try {
            Log.d("MangaRepositoryImpl", "Getting comments for manga: $mangaId") // Thêm log này
            val querySnapshot = firestore.collection("manga")
                .document(mangaId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d("MangaRepositoryImpl", "Comments query returned ${querySnapshot.documents.size} documents") // Thêm log này

            val comments = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val userId = doc.getString("userId") ?: return@mapNotNull null
                    val comment = doc.getString("comment") ?: return@mapNotNull null
                    val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                    val likes = doc.getLong("likes")?.toInt() ?: 0
                    val isEdited = doc.getBoolean("isEdited") ?: false
                    val nickname = doc.getString("nickname") ?: "Anonymous"
                    val avatar = doc.getString("avatar")

                    Comment(
                        id = id,
                        userId = userId,
                        mangaId = mangaId,
                        comment = comment,
                        timestamp = timestamp,
                        likes = likes,
                        isEdited = isEdited,
                        nickname = nickname,
                        avatar = avatar
                    )
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing comment", e)
                    null
                }
            }

            Result.success(comments)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error getting comments", e)
            Result.failure(e)
        }
    }


    override suspend fun deleteComment(mangaId: String, commentId: String): Result<Boolean> {
        return try {
            Log.d("MangaRepositoryImpl", "Attempting to delete comment: mangaId=$mangaId, commentId=$commentId")
            
            // Lấy thông tin comment trước khi xóa để kiểm tra quyền
            val commentDoc = firestore.collection("manga")
                .document(mangaId)
                .collection("comments")
                .document(commentId)
                .get()
                .await()
            
            if (!commentDoc.exists()) {
                Log.e("MangaRepositoryImpl", "Comment not found: $commentId")
                return Result.failure(Exception("Comment not found"))
            }
            
            val userId = commentDoc.getString("userId")
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            
            Log.d("MangaRepositoryImpl", "Comment userId=$userId, currentUserId=$currentUserId")
            
            if (userId != currentUserId) {
                Log.e("MangaRepositoryImpl", "Permission denied: Comment belongs to $userId, current user is $currentUserId")
                return Result.failure(Exception("You can only delete your own comments"))
            }
            
            // Xóa comment
            Log.d("MangaRepositoryImpl", "Deleting comment: $commentId")
            firestore.collection("manga")
                .document(mangaId)
                .collection("comments")
                .document(commentId)
                .delete()
                .await()
            
            Log.d("MangaRepositoryImpl", "Comment deleted successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error deleting comment", e)
            Result.failure(e)
        }
    }


    override suspend fun likeComment(mangaId: String, commentId: String, userId: String): Result<Boolean> {
        return try {
            val likeDocRef = firestore.collection("commentLikes")
                .document("${userId}_$commentId")
            
            val commentRef = firestore.collection("manga")
                .document(mangaId)
                .collection("comments")
                .document(commentId)
            
            val likeDoc = likeDocRef.get().await()
            
            if (likeDoc.exists()) {
                // Unlike
                likeDocRef.delete().await()
                
                firestore.runTransaction { transaction ->
                    val comment = transaction.get(commentRef)
                    val currentLikes = comment.getLong("likes") ?: 0
                    transaction.update(commentRef, "likes", maxOf(0, currentLikes - 1))
                }.await()
            } else {
                // Like
                val likeData = hashMapOf(
                    "userId" to userId,
                    "commentId" to commentId,
                    "timestamp" to System.currentTimeMillis()
                )
                
                likeDocRef.set(likeData).await()
                
                firestore.runTransaction { transaction ->
                    val comment = transaction.get(commentRef)
                    val currentLikes = comment.getLong("likes") ?: 0
                    transaction.update(commentRef, "likes", currentLikes + 1)
                }.await()
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error liking/unliking comment", e)
            Result.failure(e)
        }
    }

    override suspend fun checkIfUserLikedComment(commentId: String, userId: String): Result<Boolean> {
        return try {
            val likeDoc = firestore.collection("commentLikes")
                .document("${userId}_$commentId")
                .get()
                .await()
            
            Result.success(likeDoc.exists())
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error checking if user liked comment", e)
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

}
