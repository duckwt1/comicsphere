package com.android.dacs3.data.repositoryimpl

import android.content.Context
import android.net.Uri
import com.android.dacs3.BuildConfig
import com.android.dacs3.data.repository.CloudinaryRepository
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CloudinaryRepositoryImpl @Inject constructor() : CloudinaryRepository {

    private var isMediaManagerInitialized = false

    override fun initialize(context: Context) {
        if (!isMediaManagerInitialized) {
            val config = mapOf(
                "cloud_name" to BuildConfig.CloudinaryCloudName,
                "api_key" to BuildConfig.CloudinaryApiKey,
                "api_secret" to BuildConfig.CloudinaryApiSecret,
                "secure" to true
            )
            MediaManager.init(context, config)
            isMediaManagerInitialized = true
        }
    }
    
    override suspend fun uploadImage(imageUri: Uri): Result<String> = try {
        val url = uploadImageInternal(imageUri, "comicsphere_avatar", "c_fill,w_200,h_200")
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun uploadCoverImage(imageUri: Uri): Result<String> = try {
        val url = uploadImageInternal(imageUri, "comicsphere_covers", "c_fill,w_800,h_1200")
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    private suspend fun uploadImageInternal(
        imageUri: Uri, 
        folder: String, 
        transformation: String
    ): String = 
        suspendCancellableCoroutine { continuation ->
            MediaManager.get()
                .upload(imageUri)
                .option("folder", folder)
                .option("resource_type", "image")
                .option("transformation", transformation)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Upload progress
                    }

                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            continuation.resume(url)
                        } else {
                            continuation.resumeWithException(Exception("Failed to get image URL"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(Exception(error.description))
                    }
                })
                .dispatch()
        }
}
