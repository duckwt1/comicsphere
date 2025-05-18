package com.android.dacs3.data.repository

import com.android.dacs3.data.model.ChapterContentResponse
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.Comment
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaListResponse
import com.android.dacs3.data.model.ReadingProgress
import com.android.dacs3.data.model.Tag
import com.android.dacs3.data.model.TagWrapper
import com.android.dacs3.data.model.User

interface MangaRepository {
    suspend fun fetchMangaList(limit: Int, offset: Int): Result<MangaListResponse>
    suspend fun searchManga(title: String): Result<MangaListResponse>
    suspend fun getMangaById(id: String): Result<MangaDetailResponse>
    suspend fun getMangaChapters(
        mangaId: String,
        language: String,
        limit: Int,
        offset: Int
    ): Result<List<ChapterData>>
    suspend fun getChapterContent(chapterId: String): Result<ChapterContentResponse>

    suspend fun saveReadingProgress(
        userId: String,
        mangaId: String,
        chapterId: String,
        language: String,
        lastPageIndex: Int
    ): Result<Boolean>


    suspend fun getReadingProgress(userId: String): Result<List<ReadingProgress>>

    suspend fun deleteReadingProgress(
        userId: String,
        mangaId: String,
        chapterId: String,
        language: String
    ): Result<Boolean>

    suspend fun deleteAllMangaReadingProgress(
        userId: String,
        mangaId: String
    ): Result<Boolean>

    suspend fun fetchTrendingManga(limit: Int, offset: Int): Result<MangaListResponse>

    suspend fun fetchRecommendedManga(
        includedTagIds: List<String>,
        limit: Int,
        offset: Int
    ): Result<MangaListResponse>

    suspend fun getTags(): Result<List<TagWrapper>>

    suspend fun getMangaByTags(
        includedTags: List<String>,
        includedTagsMode: String = "AND",
        limit: Int = 100,
        offset: Int = 0
    ): Result<MangaListResponse>

    suspend fun addComment(
        mangaId: String,
        userId: String,
        nickname: String,
        content: String
    ): Result<Boolean>

    suspend fun getComments(mangaId: String): Result<List<Comment>>

    suspend fun deleteComment(mangaId: String, commentId: String): Result<Boolean>

    suspend fun likeComment(mangaId: String, commentId: String, userId: String): Result<Boolean>

    suspend fun checkIfUserLikedComment(commentId: String, userId: String): Result<Boolean>

    suspend fun getUserInfo(userId: String): Result<User>

    suspend fun getReadChapters(
        userId: String,
        mangaId: String,
        language: String
    ): Result<List<String>>

    suspend fun getTagsFromFirestore(): Result<List<Tag>>

    suspend fun fetchTrendingMangaFromFirestore(limit: Int = 20): Result<List<MangaData>>

    suspend fun fetchRecommendedMangaFromFirestore(
        userId: String,
        includedTagIds: List<String>? = null,
        limit: Int = 20
    ): Result<List<MangaData>>

    suspend fun getChaptersFromFirestore(mangaId: String, language: String): Result<List<ChapterData>>

    suspend fun getChapterContentFromFirestore(chapterId: String): Result<Pair<String, List<String>>>

    suspend fun getNextChapterFromFirestore(mangaId: String, currentChapterId: String, language: String): Result<ChapterData?>

    suspend fun getLastReadChapter(userId: String, mangaId: String, language: String): Result<Pair<String, Int>?>
}
