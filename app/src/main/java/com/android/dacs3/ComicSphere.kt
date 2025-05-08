package com.android.dacs3

import android.app.Application
import com.android.dacs3.data.service.CloudinaryService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ComicSphere : Application(){
    @Inject
    lateinit var cloudinaryService: CloudinaryService

    override fun onCreate() {
        super.onCreate()
        cloudinaryService.init(this)
    }
}
