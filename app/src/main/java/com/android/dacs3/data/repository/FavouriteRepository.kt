package com.android.dacs3.data.repository

import com.android.dacs3.data.model.MangaData

interface FavouriteRepository {
    suspend fun getFavourites(userId: String): Result<List<String>>
    suspend fun addFavourite(userId: String, mangaId: String): Result<Boolean>
    suspend fun removeFavourite(userId: String, mangaId: String): Result<Boolean>
    suspend fun getMangaById(id: String): Result<MangaData>
}