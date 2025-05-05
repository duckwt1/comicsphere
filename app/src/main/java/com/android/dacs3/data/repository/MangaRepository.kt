package com.android.dacs3.data.repository

import com.android.dacs3.data.model.ChapterContentResponse
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaListResponse
import com.android.dacs3.data.model.ReadingProgress

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

    suspend fun getLastReadChapter(
        userId: String,
        mangaId: String,
        language: String
    ): Result<Pair<String, Int>>

    suspend fun getReadingProgress(userId: String): Result<List<ReadingProgress>>

    suspend fun deleteReadingProgress(
        userId: String,
        mangaId: String,
        chapterId: String,
        language: String
    ): Result<Boolean>

    suspend fun fetchTrendingManga(limit: Int, offset: Int): Result<MangaListResponse>

    suspend fun fetchRecommendedManga(
        includedTagIds: List<String>,
        limit: Int,
        offset: Int
    ): Result<MangaListResponse>
}
