package com.android.dacs3.data.repository

import android.content.Context
import android.net.Uri

interface CloudinaryRepository {
    fun initialize(context: Context)
    suspend fun uploadImage(imageUri: Uri): Result<String>
}