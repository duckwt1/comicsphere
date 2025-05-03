package com.android.dacs3.data.repository

import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaListResponse

interface MangaRepository {
    suspend fun fetchMangaList(limit: Int, offset: Int): Result<MangaListResponse>
    suspend fun searchManga(title: String): Result<MangaListResponse>
    suspend fun getMangaById(id: String): Result<MangaDetailResponse>
    suspend fun getMangaChapters(mangaId: String, language: String, limit: Int, offset: Int): Result<List<ChapterData>>
}