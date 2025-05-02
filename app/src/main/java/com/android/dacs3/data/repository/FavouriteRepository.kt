package com.android.dacs3.data.repository

interface FavouriteRepository {
    suspend fun getFavourites(userId: String): Result<List<String>>
    suspend fun addFavourite(userId: String, mangaId: String): Result<Boolean>
    suspend fun removeFavourite(userId: String, mangaId: String): Result<Boolean>
}