package com.android.dacs3.di

import android.content.Context
import com.android.dacs3.data.api.MangaDexApi
import com.android.dacs3.data.repository.AuthRepository
import com.android.dacs3.data.repository.FavouriteRepository
import com.android.dacs3.data.repository.MangaRepository
import com.android.dacs3.data.repositoryimpl.AuthRepositoryImpl
import com.android.dacs3.data.repositoryimpl.FavouriteRepositoryImp
import com.android.dacs3.data.repositoryimpl.MangaRepositoryImpl
import com.android.dacs3.utliz.SessionManager
import com.google.firebase.auth.FirebaseAuth
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

}



@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMangaDexApi(retrofit: Retrofit): MangaDexApi {
        return retrofit.create(MangaDexApi::class.java)
    }
}
