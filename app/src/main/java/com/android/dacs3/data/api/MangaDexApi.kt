package com.android.dacs3.data.api

import com.android.dacs3.data.model.ChapterContentResponse
import com.android.dacs3.data.model.ChapterListResponse
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexApi {
    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaListResponse

    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaListResponse

    @GET("manga/{id}")
    suspend fun getMangaById(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("author", "artist", "cover_art", "tag")
    ): MangaDetailResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaChapters(
        @Path("id") mangaId: String,
        @Query("translatedLanguage[]") translatedLanguage: List<String> = listOf("en"),
        @Query("order[chapter]") order: String = "asc",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): ChapterListResponse

    @GET("at-home/server/{id}")
    suspend fun getChapterContent(@Path("id") chapterId: String): ChapterContentResponse

}
