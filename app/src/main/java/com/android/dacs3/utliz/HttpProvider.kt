package com.android.dacs3.utliz

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpProvider{
    companion object {
        fun sendPost(URL: String, formBody: RequestBody): JSONObject? {
            val client = OkHttpClient.Builder()
                .callTimeout(5000, TimeUnit.MILLISECONDS) // Timeout 5s
                .build()

            val request = Request.Builder()
                .url(URL)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            return try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    JSONObject(response.body?.string())
                } else {
                    null
                }
            } catch (e: IOException) {
                null
            }
        }
    }
}