package com.android.dacs3.di

import com.android.dacs3.data.repository.CloudinaryRepository
import com.android.dacs3.data.repositoryimpl.CloudinaryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudinaryModule {
    
    @Provides
    @Singleton
    fun provideCloudinaryRepository(): CloudinaryRepository {
        return CloudinaryRepositoryImpl()
    }
}