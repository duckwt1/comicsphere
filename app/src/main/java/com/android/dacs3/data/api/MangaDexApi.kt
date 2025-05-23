package com.android.dacs3.data.api

import com.android.dacs3.data.model.ChapterContentResponse
import com.android.dacs3.data.model.ChapterListResponse
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaListResponse
import com.android.dacs3.data.model.TagListResponse
import com.android.dacs3.data.model.TagWrapper
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
        @Query("limit") limit: Int = 100,
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

    @GET("manga")
    suspend fun getTrendingManga(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art"),
        @Query("order[followedCount]") order: String = "desc"
    ): MangaListResponse

    @GET("manga")
    suspend fun getRecommendedManga(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art"),
        @Query("includedTags[]") includedTags: List<String> = listOf(),
        @Query("includedTagsMode") includedTagsMode: String = "AND"
    ): MangaListResponse

    @GET("manga/tag")
    suspend fun getTags(): TagListResponse

    @GET("manga")
    suspend fun getMangaByTags(
        @Query("includedTags[]") includedTags: List<String>,
        @Query("includedTagsMode") includedTagsMode: String = "AND",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaListResponse
}



