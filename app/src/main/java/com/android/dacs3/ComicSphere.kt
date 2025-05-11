package com.android.dacs3

import android.app.Application
import com.android.dacs3.data.repository.CloudinaryRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ComicSphere : Application(){
    @Inject
    lateinit var cloudinaryRepository: CloudinaryRepository

    override fun onCreate() {
        super.onCreate()
        cloudinaryRepository.initialize(this)
    }
}
