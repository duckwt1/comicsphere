package com.android.dacs3.di

import android.content.Context
import com.android.dacs3.data.api.MangaDexApi
import com.android.dacs3.data.repository.AuthRepository
import com.android.dacs3.data.repository.FavouriteRepository
import com.android.dacs3.data.repository.MangaRepository
import com.android.dacs3.data.repository.ZaloPayRepository
import com.android.dacs3.data.repositoryimpl.AuthRepositoryImpl
import com.android.dacs3.data.repositoryimpl.FavouriteRepositoryImp
import com.android.dacs3.data.repositoryimpl.MangaRepositoryImpl
import com.android.dacs3.data.repositoryimpl.ZaloPayRepositoryImpl
import com.android.dacs3.utliz.NetworkConnectivityManager
import com.android.dacs3.utliz.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFavouriteRepository(
        favouriteRepositoryImpl: FavouriteRepositoryImp
    ): FavouriteRepository

    @Binds
    @Singleton
    abstract fun bindMangaRepository(
        mangaRepositoryImpl: MangaRepositoryImpl
    ): MangaRepository

    @Binds
    @Singleton
    abstract fun bindZaloPayRepository(
        zaloPayRepositoryImpl: ZaloPayRepositoryImpl
    ): ZaloPayRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(
        adminRepositoryImpl: com.android.dacs3.data.repositoryimpl.AdminRepositoryImpl
    ): com.android.dacs3.data.repository.AdminRepository
}
