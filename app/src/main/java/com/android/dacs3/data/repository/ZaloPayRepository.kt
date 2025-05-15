package com.android.dacs3.data.repository

import android.app.Activity
import android.content.Intent

interface ZaloPayRepository {
    fun initSdk(activity: Activity)
    
    suspend fun createOrder(amount: Long, description: String): Result<String>
    
    fun payOrder(activity: Activity, token: String, months: Int): Result<Boolean>
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean

    suspend fun updateVipStatus(userId: String, durationMonths: Int): Result<Boolean>
    
    suspend fun getVipStatus(userId: String): Result<Pair<Boolean, Long>>
    
    suspend fun checkVipExpiration(userId: String): Result<Boolean>
    
    // Thêm phương thức để kiểm tra kết quả thanh toán từ callback URL
    fun processZaloPayCallback(uri: android.net.Uri): Boolean
}
