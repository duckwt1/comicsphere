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
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.ReadingProgress
import com.android.dacs3.data.model.TagWrapper

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

}