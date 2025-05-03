package com.android.dacs3.data.repositoryimpl

import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.repository.FavouriteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavouriteRepositoryImp @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val api: com.android.dacs3.data.api.MangaDexApi
) : FavouriteRepository {
    override suspend fun getFavourites(userId: String): Result<List<String>> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val favRef = db.collection("users")
                .document(userId)
                .collection("favourite")

            val snapshot = favRef.get().await()
            val favourites = snapshot.documents.mapNotNull { it.getString("mangaId") }

            Result.success(favourites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMangaById(id: String): Result<MangaData> {
        return try {
            val response = api.getMangaById(id)
            Result.success(response.data) // Lấy MangaData từ response
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addFavourite(userId: String, mangaId: String): Result<Boolean> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val favRef = db.collection("users")
                .document(userId)
                .collection("favourite")
                .document(mangaId) // dùng mangaId làm documentId

            val data = mapOf("mangaId" to mangaId)
            favRef.set(data).await() // dùng await từ kotlinx-coroutines-play-services

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun removeFavourite(userId: String, mangaId: String): Result<Boolean> {
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .document(userId)
                .collection("favourite")
                .document(mangaId)
                .delete()
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}