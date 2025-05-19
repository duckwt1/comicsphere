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
import com.android.dacs3.data.model.ChapterAttributes
import com.android.dacs3.data.model.Comment
import com.android.dacs3.data.model.MangaAttributes
import com.android.dacs3.data.model.ReadingProgress
import com.android.dacs3.data.model.Relationship
import com.android.dacs3.data.model.RelationshipAttributes
import com.android.dacs3.data.model.Tag
import com.android.dacs3.data.model.TagAttributes
import com.android.dacs3.data.model.TagWrapper
import com.android.dacs3.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import android.content.Context
import com.android.dacs3.data.model.MangaData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.firebase.firestore.FieldPath
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class MangaRepositoryImpl @Inject constructor(
    private val api: MangaDexApi,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : MangaRepository {

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

    override suspend fun getLastReadChapter(userId: String, mangaId: String, language: String): Result<Pair<String, Int>?> {
        return try {
            // Sử dụng document ID pattern để truy vấn trực tiếp
            val progressCollection = firestore
                .collection("users")
                .document(userId)
                .collection("readingProgress")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), "${mangaId}_${language}_")
                .whereLessThanOrEqualTo(FieldPath.documentId(), "${mangaId}_${language}_\uf8ff")
                .get()
                .await()
            
            if (progressCollection.documents.isEmpty()) {
                return Result.success(null)
            }
            
            // Tìm document có timestamp mới nhất
            val progressDoc = progressCollection.documents.maxByOrNull { 
                it.getLong("timestamp") ?: 0L 
            }
            
            if (progressDoc == null) {
                return Result.success(null)
            }
            
            val chapterId = progressDoc.getString("chapterId")
            val lastPageIndex = progressDoc.getLong("lastPageIndex")?.toInt() ?: 1
            
            if (chapterId == null) {
                return Result.success(null)
            }
            
            Result.success(Pair(chapterId, lastPageIndex))
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

//    override suspend fun getReadChapters(
//        userId: String,
//        mangaId: String,
//        language: String
//    ): Result<List<String>> {
//        return try {
//            val querySnapshot = firestore.collection("users")
//                .document(userId)
//                .collection("readingProgress")
//                .whereEqualTo("mangaId", mangaId)
//                .whereEqualTo("language", language)
//                .get()
//                .await()
//
//            val readChapterIds = querySnapshot.documents.mapNotNull { doc ->
//                doc.getString("chapterId")
//            }
//
//            Result.success(readChapterIds)
//        } catch (e: Exception) {
//            Log.e("MangaRepositoryImpl", "Error getting read chapters", e)
//            Result.failure(e)
//        }
//    }

    override suspend fun getTagsFromFirestore(): Result<List<Tag>> {
        return try {
            Log.d("MangaRepositoryImpl", "Fetching tags from Firestore")
            
            val querySnapshot = firestore.collection("tags")
                .get()
                .await()
            
            Log.d("MangaRepositoryImpl", "Fetched ${querySnapshot.documents.size} tag documents")
            
            val tags = querySnapshot.documents.mapNotNull { document ->
                try {
                    val id = document.id
                    val name = document.getString("name") ?: return@mapNotNull null
                    val group = document.getString("group") ?: "unknown"
                    
                    Tag(id = id, name = name, group = group)
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing tag document: ${document.id}", e)
                    null
                }
            }
            
            Log.d("MangaRepositoryImpl", "Successfully parsed ${tags.size} tags")
            Result.success(tags)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error fetching tags from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchTrendingMangaFromFirestore(limit: Int): Result<List<MangaData>> {
        return try {
            Log.d("MangaRepositoryImpl", "Fetching trending manga from Firestore with limit: $limit")
            
            // Lấy danh sách tags trước
            val tagsResult = getTagsFromFirestore()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Lấy dữ liệu từ Firestore
            val mangaCollection = FirebaseFirestore.getInstance().collection("manga")
            
            // Kiểm tra xem có manga nào có trường viewCount không
            val testQuery = mangaCollection.whereGreaterThanOrEqualTo("viewCount", 0).limit(1)
            val testResult = testQuery.get().await()
            
            // Nếu không có manga nào có trường viewCount, lấy tất cả manga và sắp xếp theo lastUpdated
            val query = if (testResult.isEmpty) {
                Log.d("MangaRepositoryImpl", "No manga with viewCount field found, using lastUpdated instead")
                mangaCollection.orderBy("lastUpdated", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
            } else {
                mangaCollection.orderBy("viewCount", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
            }
            
            val querySnapshot = query.get().await()
            
            Log.d("MangaRepositoryImpl", "Fetched ${querySnapshot.documents.size} trending manga documents")
            
            // Chuyển đổi dữ liệu Firestore thành MangaData
            val mangaList = querySnapshot.documents.mapNotNull { document ->
                try {
                    // Log thông tin viewCount để debug
                    val viewCount = document.getLong("viewCount") ?: 0
                    Log.d("MangaRepositoryImpl", "Manga ${document.id} has viewCount: $viewCount")
                    
                    convertFirestoreDocumentToMangaData(document, tagMap)
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing manga document: ${document.id}", e)
                    null
                }
            }
            
            // Nếu không có kết quả, thử lấy tất cả manga
            if (mangaList.isEmpty()) {
                Log.d("MangaRepositoryImpl", "No trending manga found, fetching all manga instead")
                return fetchMangaListFromFirestore(true, limit)
            }
            
            Log.d("MangaRepositoryImpl", "Loaded ${mangaList.size} trending manga from Firestore")
            Result.success(mangaList)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading trending manga", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchRecommendedMangaFromFirestore(
        includedTagIds: List<String>?,
        limit: Int
    ): Result<List<MangaData>> {
        return try {
            Log.d("MangaRepositoryImpl", "Fetching recommended manga from Firestore with limit: $limit")
            Log.d("MangaRepositoryImpl", "Included tag IDs: ${includedTagIds?.joinToString() ?: "none"}")
            
            // Lấy danh sách tags trước
            val tagsResult = getTagsFromFirestore()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Lấy dữ liệu từ Firestore
            val mangaCollection = FirebaseFirestore.getInstance().collection("manga")
            
            // Nếu có tagIds, lọc theo tags
            val query = if (!includedTagIds.isNullOrEmpty()) {
                // Kiểm tra xem có manga nào có trường tagIds không
                val testQuery = mangaCollection.whereArrayContainsAny("tagIds", includedTagIds).limit(1)
                val testResult = testQuery.get().await()
                
                if (testResult.isEmpty) {
                    Log.d("MangaRepositoryImpl", "No manga with matching tagIds found, using lastUpdated instead")
                    mangaCollection.orderBy("lastUpdated", Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                } else {
                    mangaCollection.whereArrayContainsAny("tagIds", includedTagIds)
                        .limit(limit.toLong())
                }
            } else {
                // Nếu không có tags, lấy manga theo viewCount (trending)
                Log.d("MangaRepositoryImpl", "No tags provided, using viewCount instead")
                mangaCollection.orderBy("viewCount", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
            }
            
            val querySnapshot = query.get().await()
            
            Log.d("MangaRepositoryImpl", "Fetched ${querySnapshot.documents.size} recommended manga documents")
            
            // Chuyển đổi dữ liệu Firestore thành MangaData
            val mangaList = querySnapshot.documents.mapNotNull { document ->
                try {
                    convertFirestoreDocumentToMangaData(document, tagMap)
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing manga document: ${document.id}", e)
                    null
                }
            }
            
            // Nếu không có kết quả, thử lấy tất cả manga
            if (mangaList.isEmpty()) {
                Log.d("MangaRepositoryImpl", "No recommended manga found, fetching all manga instead")
                return fetchMangaListFromFirestore(true, limit)
            }
            
            Log.d("MangaRepositoryImpl", "Loaded ${mangaList.size} recommended manga from Firestore")
            Result.success(mangaList)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading recommended manga", e)
            Result.failure(e)
        }
    }

    // Hàm tiện ích để chuyển đổi document Firestore thành MangaData
    private fun convertFirestoreDocumentToMangaData(
        document: DocumentSnapshot,
        tagMap: Map<String, Tag>
    ): MangaData {
        val id = document.id
        
        // Log toàn bộ dữ liệu của document để debug
        Log.d("MangaRepositoryImpl", "Converting document ${document.id}")
        
        // Ưu tiên coverImageUrl
        val coverImageUrl = document.getString("coverImageUrl") ?: ""
        val coverUrl = document.getString("coverUrl") ?: ""
        
        // Xác định URL cuối cùng
        val finalCoverUrl = when {
            coverImageUrl.isNotEmpty() -> coverImageUrl
            coverUrl.isNotEmpty() && coverUrl.startsWith("http") -> coverUrl
            else -> "https://via.placeholder.com/512x768?text=No+Cover"
        }
        
        // Xử lý các thông tin khác
        val title: Map<String, String> = (document.get("title") as? Map<String, String>) 
            ?: mapOf("en" to (document.getString("title") ?: "Unknown"))
        val description = document.getString("description") ?: ""
        val status = document.getString("status") ?: ""
        
        // Xử lý author - có thể là String hoặc List<String>
        val authorRaw = document.get("authors")
        Log.d("MangaRepositoryImpl", "Raw author data for ${document.id}: $authorRaw (type: ${authorRaw?.javaClass?.simpleName})")
        
        val author = when (authorRaw) {
            is String -> authorRaw.takeIf { it.isNotEmpty() } ?: "Unknown Author"
            is List<*> -> {
                val authorList = authorRaw.filterIsInstance<String>()
                if (authorList.isEmpty()) "Unknown Author" else authorList.joinToString(", ")
            }
            else -> {
                Log.e("MangaRepositoryImpl", "Author field is missing or has unexpected type for manga ${document.id}")
                "Unknown Author"
            }
        }
        
        Log.d("MangaRepositoryImpl", "Processed author for manga ${document.id}: $author")
        
        // Xử lý availableTranslatedLanguages
        val availableLanguages = (document.get("availableLanguages") as? List<String>) ?: listOf("en")
        
        // Xử lý tags
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
                    ))
            }
        }
        
        // Tạo đối tượng MangaData với coverImageUrl
        return MangaData(
            id = id,
            attributes = MangaAttributes(
                title = title,
                description = mapOf("en" to description),
                status = status,
                availableTranslatedLanguages = availableLanguages,
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
    }

    // Thêm phương thức để lấy danh sách manga từ Firestore
    override suspend fun fetchMangaListFromFirestore(reset: Boolean, limit: Int): Result<List<MangaData>> {
        return try {
            Log.d("MangaRepositoryImpl", "Fetching manga list from Firestore with limit: $limit")
            
            // Lấy danh sách tags trước
            val tagsResult = getTagsFromFirestore()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Lấy tất cả dữ liệu từ Firestore
            val mangaCollection = FirebaseFirestore.getInstance().collection("manga")
            val query = mangaCollection.orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            val querySnapshot = query.get().await()
            
            Log.d("MangaRepositoryImpl", "Fetched ${querySnapshot.documents.size} manga documents from Firestore")
            
            // Chuyển đổi dữ liệu Firestore thành MangaData
            val mangaList = querySnapshot.documents.mapNotNull { document ->
                try {
                    convertFirestoreDocumentToMangaData(document, tagMap)
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing manga document: ${document.id}", e)
                    null
                }
            }
            
            Log.d("MangaRepositoryImpl", "Loaded ${mangaList.size} manga from Firestore")
            Result.success(mangaList)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading manga list", e)
            Result.failure(e)
        }
    }

    override suspend fun getChaptersFromFirestore(mangaId: String, language: String): Result<List<ChapterData>> {
        return try {
            Log.d("MangaRepositoryImpl", "Loading chapters for manga $mangaId with language $language")
            
            // Thay đổi cách truy vấn để tránh lỗi index
            // Lấy tất cả chapters của manga trước, sau đó lọc theo language trong code
            val chaptersCollection = firestore
                .collection("manga")
                .document(mangaId)
                .collection("chapters")
                .get()
                .await()
            
            Log.d("MangaRepositoryImpl", "Found ${chaptersCollection.documents.size} total chapters in Firestore")
            
            // Lọc chapters theo language và sắp xếp theo chapter number
            val chaptersList = chaptersCollection.documents
                .filter { doc -> doc.getString("language") == language }
                .sortedByDescending { doc -> 
                    doc.getString("chapter")?.toFloatOrNull() ?: 0f 
                }
                .mapNotNull { doc ->
                    try {
                        val chapterId = doc.id
                        val chapterNumber = doc.getString("chapter") ?: ""
                        val title = doc.getString("title") ?: ""
                        val translatedLanguage = doc.getString("language") ?: language
                        val pages = doc.getLong("pages")?.toInt() ?: 0
                        val publishAt = doc.getString("publishAt") ?: ""
                        
                        Log.d("MangaRepositoryImpl", "Parsed chapter: $chapterId, number: $chapterNumber, title: $title, pages: $pages")
                        
                        ChapterData(
                            id = chapterId,
                            attributes = ChapterAttributes(
                                chapter = chapterNumber,
                                title = title,
                                translatedLanguage = translatedLanguage,
                                externalUrl = null,
                                publishAt = publishAt
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("MangaRepositoryImpl", "Error parsing chapter document: ${doc.id}", e)
                        null
                    }
                }
            
            Log.d("MangaRepositoryImpl", "Successfully loaded ${chaptersList.size} chapters for language $language")
            Result.success(chaptersList)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading chapters from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun getChapterContentFromFirestore(chapterId: String): Result<Pair<String, List<String>>> {
        return try {
            Log.d("MangaRepositoryImpl", "Loading content for chapter $chapterId")
            
            // Tìm chapter trong collection chapters của tất cả manga
            val mangaId = findMangaIdForChapter(chapterId)
            
            if (mangaId == null) {
                Log.e("MangaRepositoryImpl", "Could not find manga containing chapter $chapterId")
                return Result.failure(Exception("Chapter not found in any manga"))
            }
            
            Log.d("MangaRepositoryImpl", "Found mangaId: $mangaId for chapter: $chapterId")
            
            // Lấy thông tin chapter
            val chapterDoc = firestore
                .collection("manga")
                .document(mangaId)
                .collection("chapters")
                .document(chapterId)
                .get()
                .await()
            
            if (!chapterDoc.exists()) {
                Log.e("MangaRepositoryImpl", "Chapter $chapterId not found in manga $mangaId")
                return Result.failure(Exception("Chapter not found"))
            }
            
            Log.d("MangaRepositoryImpl", "Chapter document exists. Fields: ${chapterDoc.data?.keys}")
            
            // Kiểm tra xem chapter có externalUrl không
            val externalUrl = chapterDoc.getString("externalUrl")
            if (!externalUrl.isNullOrEmpty()) {
                Log.d("MangaRepositoryImpl", "Chapter has externalUrl: $externalUrl")
                // Trả về externalUrl dưới dạng một danh sách đặc biệt với phần tử đầu tiên là "EXTERNAL_URL"
                val title = chapterDoc.getString("title") ?: "Chapter ${chapterDoc.getString("chapter") ?: ""}"
                return Result.success(Pair(title, listOf("EXTERNAL_URL", externalUrl)))
            }
            
            // Kiểm tra xem subcollection images có tồn tại không
            val imagesCollection = firestore
                .collection("manga")
                .document(mangaId)
                .collection("chapters")
                .document(chapterId)
                .collection("images")
            
            val imagesQuery = imagesCollection.limit(1).get().await()
            if (imagesQuery.isEmpty) {
                Log.e("MangaRepositoryImpl", "No images subcollection found for chapter $chapterId")
                return Result.failure(Exception("No images found for this chapter"))
            }
            
            Log.d("MangaRepositoryImpl", "Images subcollection exists. Documents: ${imagesQuery.documents.map { it.id }}")
            
            // Lấy thông tin từ document imageList trong subcollection images
            Log.d("MangaRepositoryImpl", "Loading imageList for chapter $chapterId")
            
            val imageListDoc = imagesCollection.document("imageList").get().await()
            
            if (!imageListDoc.exists()) {
                Log.e("MangaRepositoryImpl", "imageList document not found for chapter $chapterId")
                
                // Thử tìm document khác trong subcollection images
                val allImagesQuery = imagesCollection.get().await()
                Log.d("MangaRepositoryImpl", "Found ${allImagesQuery.size()} documents in images subcollection: ${allImagesQuery.documents.map { it.id }}")
                
                if (allImagesQuery.isEmpty) {
                    return Result.failure(Exception("No images found for this chapter"))
                }
                
                // Thử lấy document đầu tiên
                val firstImageDoc = allImagesQuery.documents.first()
                Log.d("MangaRepositoryImpl", "Trying first document: ${firstImageDoc.id}, fields: ${firstImageDoc.data?.keys}")
                
                // Kiểm tra xem document có field urls không
                val urlsFromFirst = firstImageDoc.get("urls")
                if (urlsFromFirst is List<*>) {
                    val imageUrls = urlsFromFirst.filterIsInstance<String>()
                    if (imageUrls.isNotEmpty()) {
                        Log.d("MangaRepositoryImpl", "Found ${imageUrls.size} images in ${firstImageDoc.id}.urls")
                        val title = chapterDoc.getString("title") ?: "Chapter ${chapterDoc.getString("chapter") ?: ""}"
                        return Result.success(Pair(title, imageUrls))
                    }
                }
                
                return Result.failure(Exception("Image list not found for this chapter"))
            }
            
            Log.d("MangaRepositoryImpl", "imageList document exists. Fields: ${imageListDoc.data?.keys}")
            
            // Lấy mảng URLs từ field urls
            val urlsField = imageListDoc.get("urls")
            Log.d("MangaRepositoryImpl", "urls field type: ${urlsField?.javaClass?.name}, value: $urlsField")
            
            if (urlsField !is List<*>) {
                Log.e("MangaRepositoryImpl", "urls field is not a List: ${urlsField?.javaClass?.name}")
                
                // Kiểm tra các field khác
                imageListDoc.data?.forEach { (key, value) ->
                    Log.d("MangaRepositoryImpl", "Field $key: type=${value?.javaClass?.name}, value=$value")
                    
                    // Nếu có field nào đó là List<String>, thử dùng nó
                    if (value is List<*>) {
                        val stringList = value.filterIsInstance<String>()
                        if (stringList.isNotEmpty()) {
                            Log.d("MangaRepositoryImpl", "Found alternative list in field $key with ${stringList.size} items")
                            val title = chapterDoc.getString("title") ?: "Chapter ${chapterDoc.getString("chapter") ?: ""}"
                            return Result.success(Pair(title, stringList))
                        }
                    }
                }
                
                return Result.failure(Exception("Invalid image list format"))
            }
            
            val imageUrls = urlsField.filterIsInstance<String>()
            if (imageUrls.isEmpty()) {
                Log.e("MangaRepositoryImpl", "No images found in urls field")
                return Result.failure(Exception("No images found for this chapter"))
            }
            
            Log.d("MangaRepositoryImpl", "Found ${imageUrls.size} images in imageList.urls")
            
            // Lấy tiêu đề chapter
            val title = chapterDoc.getString("title") ?: "Chapter ${chapterDoc.getString("chapter") ?: ""}"
            Log.d("MangaRepositoryImpl", "Chapter title: $title, total images: ${imageUrls.size}")
            
            // Log một số URL hình ảnh đầu tiên để debug
            imageUrls.take(2).forEachIndexed { index, url ->
                Log.d("MangaRepositoryImpl", "Image ${index + 1} URL: $url")
            }
            
            Result.success(Pair(title, imageUrls))
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading chapter content from Firestore", e)
            return Result.failure(e)
        }
    }

    // Cache để lưu mapping từ chapterId -> mangaId
    private val chapterToMangaCache = mutableMapOf<String, String>()

    // Phương thức cải tiến để tìm mangaId từ chapterId - chỉ giữ một phiên bản
    private suspend fun findMangaIdForChapter(chapterId: String): String? {
        try {
            // Check cache first
            chapterToMangaCache[chapterId]?.let { cachedMangaId ->
                Log.d("MangaRepositoryImpl", "Found mangaId $cachedMangaId for chapter $chapterId in cache")
                return cachedMangaId
            }
            
            Log.d("MangaRepositoryImpl", "Searching for chapter $chapterId")
            
            // Use a different approach instead of collection group query
            // Get all manga documents first
            val mangaCollection = firestore.collection("manga").get().await()
            
            for (mangaDoc in mangaCollection.documents) {
                val mangaId = mangaDoc.id
                Log.d("MangaRepositoryImpl", "Checking manga $mangaId for chapter $chapterId")
                
                // Check if this manga contains the chapter
                val chapterDoc = firestore
                    .collection("manga")
                    .document(mangaId)
                    .collection("chapters")
                    .document(chapterId)
                    .get()
                    .await()
                
                if (chapterDoc.exists()) {
                    Log.d("MangaRepositoryImpl", "Found chapter $chapterId in manga $mangaId")
                    chapterToMangaCache[chapterId] = mangaId
                    return mangaId
                }
            }
            
            Log.e("MangaRepositoryImpl", "Chapter $chapterId not found in any manga")
            return null
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error finding mangaId for chapter $chapterId", e)
            return null
        }
    }

    // Thêm phương thức để lưu cache vào SharedPreferences
    private fun saveChapterCache() {
        try {
            val sharedPrefs = context.getSharedPreferences("manga_cache", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            
            // Chuyển đổi cache thành JSON
            val gson = Gson()
            val json = gson.toJson(chapterToMangaCache)
            
            // Lưu vào SharedPreferences
            editor.putString("chapter_to_manga_cache", json)
            editor.apply()
            
            Log.d("MangaRepositoryImpl", "Saved ${chapterToMangaCache.size} chapter-manga mappings to cache")
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error saving chapter cache", e)
        }
    }

    // Thêm phương thức để load cache từ SharedPreferences
    private fun loadChapterCache() {
        try {
            val sharedPrefs = context.getSharedPreferences("manga_cache", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("chapter_to_manga_cache", null)
            
            if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<Map<String, String>>() {}.type
                val loadedCache: Map<String, String> = gson.fromJson(json, type)
                
                // Cập nhật cache
                chapterToMangaCache.putAll(loadedCache)
                
                Log.d("MangaRepositoryImpl", "Loaded ${chapterToMangaCache.size} chapter-manga mappings from cache")
            }
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading chapter cache", e)
        }
    }

    override suspend fun getNextChapterFromFirestore(
        mangaId: String, 
        currentChapterId: String, 
        language: String
    ): Result<ChapterData?> {
        return try {
            Log.d("MangaRepositoryImpl", "Finding next chapter after $currentChapterId in manga $mangaId")
            
            // Lấy chapter hiện tại để biết số chapter
            val currentChapterDoc = firestore
                .collection("manga")
                .document(mangaId)
                .collection("chapters")
                .document(currentChapterId)
                .get()
                .await()
            
            if (!currentChapterDoc.exists()) {
                Log.e("MangaRepositoryImpl", "Current chapter $currentChapterId not found")
                return Result.failure(Exception("Current chapter not found"))
            }
            
            val currentChapterNumber = currentChapterDoc.getString("chapter")?.toFloatOrNull() ?: 0f
            Log.d("MangaRepositoryImpl", "Current chapter number: $currentChapterNumber")
            
            // Thay đổi cách truy vấn để tránh lỗi index
            // Lấy tất cả chapters của manga, sau đó lọc và sắp xếp trong code
            val chaptersCollection = firestore
                .collection("manga")
                .document(mangaId)
                .collection("chapters")
                .get()
                .await()
            
            // Lọc chapters theo language và chapter number > current chapter number
            // Sau đó sắp xếp tăng dần theo chapter number và lấy chapter đầu tiên
            val nextChapterDoc = chaptersCollection.documents
                .filter { doc -> 
                    doc.getString("language") == language &&
                    (doc.getString("chapter")?.toFloatOrNull() ?: 0f) > currentChapterNumber
                }
                .sortedBy { doc -> 
                    doc.getString("chapter")?.toFloatOrNull() ?: Float.MAX_VALUE 
                }
                .firstOrNull()
            
            if (nextChapterDoc == null) {
                Log.d("MangaRepositoryImpl", "No next chapter found after chapter $currentChapterNumber")
                return Result.success(null) // Không có chapter tiếp theo
            }
            
            val nextChapterId = nextChapterDoc.id
            val nextChapterNumber = nextChapterDoc.getString("chapter") ?: ""
            val title = nextChapterDoc.getString("title") ?: ""
            
            Log.d("MangaRepositoryImpl", "Found next chapter: $nextChapterId, number: $nextChapterNumber, title: $title")
            
            val translatedLanguage = nextChapterDoc.getString("language") ?: language
            val publishAt = nextChapterDoc.getString("publishAt") ?: ""
            
            val nextChapter = ChapterData(
                id = nextChapterId,
                attributes = ChapterAttributes(
                    chapter = nextChapterNumber,
                    title = title,
                    translatedLanguage = translatedLanguage,
                    externalUrl = null,
                    publishAt = publishAt
                )
            )
            
            Result.success(nextChapter)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error finding next chapter", e)
            Result.failure(e)
        }
    }

    override suspend fun getReadChapters(userId: String, mangaId: String, language: String): Result<List<String>> {
        return try {
            // Lấy danh sách chapter đã đọc từ Firestore
            val progressCollection = firestore
                .collection("users")
                .document(userId)
                .collection("readingProgress")
                .whereEqualTo("mangaId", mangaId)
                .whereEqualTo("language", language)
                .get()
                .await()
            
            val readChapterIds = progressCollection.documents.mapNotNull { doc ->
                doc.getString("chapterId")
            }
            
            Result.success(readChapterIds)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading read chapters", e)
            Result.failure(e)
        }
    }

    override suspend fun searchMangaFromFirestore(title: String): Result<List<MangaData>> {
        return try {
            Log.d("MangaRepositoryImpl", "Searching manga with title: $title")
            
            // Lấy danh sách tags trước
            val tagsResult = getTagsFromFirestore()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Lấy dữ liệu từ Firestore
            val querySnapshot = firestore.collection("manga").get().await()
            
            // Lọc thủ công vì Firestore không hỗ trợ tìm kiếm text đầy đủ
            val searchTermLower = title.lowercase()
            
            // Chuyển đổi dữ liệu Firestore thành MangaData và lọc theo title
            val mangaList = querySnapshot.documents.mapNotNull { document ->
                try {
                    val mangaTitle: Map<String, String> = (document.get("title") as? Map<String, String>) 
                        ?: mapOf("en" to (document.getString("title") ?: "Unknown"))
                    
                    // Kiểm tra xem title có chứa search term không
                    val titleMatches = mangaTitle.values.any { 
                        it.lowercase().contains(searchTermLower) 
                    }
                    
                    if (titleMatches) {
                        convertFirestoreDocumentToMangaData(document, tagMap)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing manga document: ${document.id}", e)
                    null
                }
            }
            
            Log.d("MangaRepositoryImpl", "Found ${mangaList.size} manga matching search term: $title")
            Result.success(mangaList)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error searching manga", e)
            Result.failure(e)
        }
    }

    override suspend fun getMangaDetailsFromFirestore(mangaId: String): Result<MangaData> {
        return try {
            Log.d("MangaRepositoryImpl", "Loading manga details for $mangaId")
            
            // Lấy danh sách tags trước
            val tagsResult = getTagsFromFirestore()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Lấy dữ liệu manga từ Firestore
            val mangaDoc = firestore.collection("manga").document(mangaId).get().await()
            
            if (!mangaDoc.exists()) {
                return Result.failure(Exception("Manga not found"))
            }
            
            // Log toàn bộ dữ liệu của document để debug
            Log.d("MangaRepositoryImpl", "Manga document data: ${mangaDoc.data}")
            
            // Kiểm tra và cập nhật availableLanguages nếu cần
            if (mangaDoc.get("availableLanguages") == null) {
                // Nếu không có trường availableLanguages, thêm vào
                firestore.collection("manga").document(mangaId)
                    .update("availableLanguages", listOf("en"))
                    .await()
                
                Log.d("MangaRepositoryImpl", "Added default availableLanguages field to manga $mangaId")
            }
            
            // Kiểm tra và cập nhật author nếu cần
            if (mangaDoc.get("authors") == null) {
                // Nếu không có trường author, thêm vào
                firestore.collection("manga").document(mangaId)
                    .update("authors", listOf("Unknown Author"))
                    .await()
                
                Log.d("MangaRepositoryImpl", "Added default author field to manga $mangaId")
                
                // Lấy lại document sau khi cập nhật
                val updatedMangaDoc = firestore.collection("manga").document(mangaId).get().await()
                val mangaData = convertFirestoreDocumentToMangaData(updatedMangaDoc, tagMap)
                return Result.success(mangaData)
            }
            
            val mangaData = convertFirestoreDocumentToMangaData(mangaDoc, tagMap)
            
            // Log thông tin về ngôn ngữ và author
            Log.d("MangaRepositoryImpl", "Manga ${mangaData.id} has available languages: ${mangaData.attributes.availableTranslatedLanguages}")
            Log.d("MangaRepositoryImpl", "Manga ${mangaData.id} has author: ${mangaData.attributes.author}")
            
            Result.success(mangaData)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error loading manga details", e)
            Result.failure(e)
        }
    }

    override suspend fun filterMangaByTags(tagIds: List<String>, filterMode: String): Result<List<MangaData>> {
        return try {
            Log.d("MangaRepositoryImpl", "Filtering manga by tags: $tagIds with mode: $filterMode")
            
            if (tagIds.isEmpty()) {
                return fetchMangaListFromFirestore(true, 100)
            }
            
            // Lấy danh sách tags trước
            val tagsResult = getTagsFromFirestore()
            val tagMap = tagsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
            
            // Lấy dữ liệu từ Firestore với filter
            val mangaCollection = firestore.collection("manga")
            
            // Sử dụng whereArrayContainsAny để lấy manga có ít nhất một trong các tag đã chọn
            val query = mangaCollection.whereArrayContainsAny("tagIds", tagIds)
            
            val querySnapshot = query.get().await()
            
            Log.d("MangaRepositoryImpl", "Fetched ${querySnapshot.documents.size} manga documents for tag filtering")
            
            // Chuyển đổi dữ liệu Firestore thành MangaData
            val mangaList = querySnapshot.documents.mapNotNull { document ->
                try {
                    convertFirestoreDocumentToMangaData(document, tagMap)
                } catch (e: Exception) {
                    Log.e("MangaRepositoryImpl", "Error parsing manga document: ${document.id}", e)
                    null
                }
            }
            
            // Nếu mode là AND, lọc thêm để đảm bảo manga chứa TẤT CẢ các tag đã chọn
            val filteredList = if (filterMode == "AND" && tagIds.size > 1) {
                mangaList.filter { manga ->
                    val mangaTags = manga.attributes.tags.map { it.id }
                    tagIds.all { tagId -> mangaTags.contains(tagId) }
                }
            } else {
                mangaList
            }
            
            Log.d("MangaRepositoryImpl", "Filtered to ${filteredList.size} manga based on tags")
            Result.success(filteredList)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error filtering manga by tags", e)
            Result.failure(e)
        }
    }

    override suspend fun incrementMangaViewCount(mangaId: String): Result<Boolean> {
        return try {
            Log.d("MangaRepositoryImpl", "Incrementing view count for manga: $mangaId")
            
            // Lấy tham chiếu đến document manga
            val mangaRef = firestore.collection("manga").document(mangaId)
            
            // Sử dụng transaction để tăng viewCount một cách an toàn
            firestore.runTransaction { transaction ->
                val mangaDoc = transaction.get(mangaRef)
                
                // Lấy giá trị viewCount hiện tại hoặc 0 nếu không tồn tại
                val currentViewCount = mangaDoc.getLong("viewCount") ?: 0
                
                // Tăng viewCount lên 1
                transaction.update(mangaRef, "viewCount", currentViewCount + 1)
                
                // Transaction thành công
                null
            }.await()
            
            Log.d("MangaRepositoryImpl", "Successfully incremented view count for manga: $mangaId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("MangaRepositoryImpl", "Error incrementing view count", e)
            Result.failure(e)
        }
    }
}
