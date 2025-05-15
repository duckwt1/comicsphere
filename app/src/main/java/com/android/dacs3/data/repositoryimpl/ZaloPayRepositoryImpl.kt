package com.android.dacs3.data.repositoryimpl

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.dacs3.data.repository.ZaloPayRepository
import com.android.dacs3.utliz.AppInfo
import com.android.dacs3.utliz.CreateOrder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZaloPayRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ZaloPayRepository {

    companion object {
        private const val TAG = "ZaloPayRepository"
    }

    // Lưu thông tin số tháng VIP cho mỗi giao dịch
    private val paymentMonths = mutableMapOf<String, Int>()

    // Thêm một map mới để lưu trữ số tháng đã xử lý
    private val processedPayments = mutableMapOf<String, Int>()

    override fun initSdk(activity: Activity) {
        ZaloPaySDK.init(AppInfo.APP_ID, Environment.SANDBOX)
        Log.d(TAG, "ZaloPay SDK initialized with APP_ID: ${AppInfo.APP_ID}")
    }

    override suspend fun createOrder(amount: Long, description: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val createOrder = CreateOrder()
                Log.d(TAG, "Creating order with amount: $amount, description: $description")
                
                val jsonData = createOrder.createOrder(amount.toString())
                
                if (jsonData == null) {
                    Log.e(TAG, "Failed to create order: No response data")
                    return@withContext Result.failure(Exception("Failed to create order: No response data"))
                }
                
                Log.d(TAG, "ZaloPay API response: $jsonData")
                
                val returnCode = jsonData.getString("return_code")
                val returnMessage = jsonData.optString("return_message", "Unknown error")
                
                if (returnCode == "1") {
                    val token = jsonData.getString("zp_trans_token")
                    val appTransId = jsonData.optString("app_trans_id", "")
                    Log.d(TAG, "Order created successfully with token: $token, appTransId: $appTransId")
                    Result.success(token)
                } else {
                    Log.e(TAG, "Failed to create order: $returnMessage (code: $returnCode)")
                    Result.failure(Exception("Failed to create order: $returnMessage (code: $returnCode)"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order", e)
                Result.failure(e)
            }
        }
    }

    override fun payOrder(activity: Activity, token: String, months: Int): Result<Boolean> {
        return try {
            // Lưu số tháng VIP cho token này
            paymentMonths[token] = months
            Log.d(TAG, "Starting payment with token: $token, months: $months")
            
            ZaloPaySDK.getInstance().payOrder(
                activity, 
                token, 
                "comicsphere://zalopay.callback",
                object : PayOrderListener {
                    override fun onPaymentSucceeded(transactionId: String, transToken: String, appTransID: String) {
                        Log.d(TAG, "Payment succeeded via SDK callback: transactionId=$transactionId, transToken=$transToken")
                        // Khi thanh toán thành công, cập nhật trạng thái VIP
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null) {
                            // Lấy số tháng đã lưu hoặc mặc định là 1 tháng
                            val durationMonths = paymentMonths[transToken] ?: 1
                            Log.d(TAG, "Updating VIP status for user $userId with $durationMonths months")
                            updateVipStatusAfterPayment(userId, durationMonths)
                            // Xóa khỏi map sau khi đã xử lý
                            paymentMonths.remove(transToken)
                        } else {
                            Log.e(TAG, "User ID is null, cannot update VIP status")
                        }
                    }

                    override fun onPaymentCanceled(zpTransToken: String, appTransID: String) {
                        Log.d(TAG, "Payment canceled via SDK callback: token=$zpTransToken")
                        // Xóa khỏi map khi hủy
                        paymentMonths.remove(zpTransToken)
                    }

                    override fun onPaymentError(zaloPayError: ZaloPayError, zpTransToken: String, appTransID: String) {
                        Log.e(TAG, "Payment error via SDK callback: ${zaloPayError.toString()}, token=$zpTransToken")
                        // Xóa khỏi map khi lỗi
                        paymentMonths.remove(zpTransToken)
                    }
                }
            )
            Log.d(TAG, "Payment initiated successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing payment", e)
            Result.failure(e)
        }
    }
    
    override fun processZaloPayCallback(uri: Uri): Boolean {
        try {
            Log.d(TAG, "Processing ZaloPay callback: $uri")
            
            // Kiểm tra xem callback có phải từ ZaloPay không
            if (uri.scheme == "comicsphere" && uri.host == "zalopay.callback") {
                // Kiểm tra trạng thái thanh toán - ZaloPay sử dụng 'code' thay vì 'status'
                val code = uri.getQueryParameter("code")
                val isSuccess = code == "1" // 1 = success
                
                Log.d(TAG, "ZaloPay callback code: $code, isSuccess: $isSuccess")
                
                if (isSuccess) {
                    // Lấy thông tin từ callback URL
                    val transToken = uri.getQueryParameter("zpTransToken")
                    val appTransId = uri.getQueryParameter("appTransID")
                    val transactionId = uri.getQueryParameter("transactionId")
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    
                    Log.d(TAG, "ZaloPay callback params: transToken=$transToken, appTransId=$appTransId, transactionId=$transactionId, userId=$userId")
                    
                    if (userId == null) {
                        Log.e(TAG, "User ID is null, cannot update VIP status")
                        return false
                    }
                    
                    // Lấy số tháng đã lưu hoặc mặc định là 1 tháng
                    val durationMonths = if (transToken != null) {
                        val months = paymentMonths[transToken]
                        Log.d(TAG, "Retrieved months from map for token $transToken: $months")
                        
                        // Lưu số tháng vào processedPayments trước khi xóa khỏi paymentMonths
                        if (months != null) {
                            processedPayments[transToken] = months
                        }
                        
                        months ?: 1
                    } else {
                        Log.d(TAG, "No token found in callback, using default 1 month")
                        1 // Mặc định 1 tháng nếu không có token
                    }
                    
                    Log.d(TAG, "Updating VIP status via deep link callback for user $userId with $durationMonths months")
                    
                    // Cập nhật trạng thái VIP
                    updateVipStatusAfterPayment(userId, durationMonths)
                    
                    // Xóa khỏi map sau khi đã xử lý (nếu có token)
                    if (transToken != null) {
                        paymentMonths.remove(transToken)
                    }
                    
                    return true
                }
                return false
            }
            Log.d(TAG, "Not a ZaloPay callback URI")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error processing ZaloPay callback", e)
            return false
        }
    }
    
    private fun updateVipStatusAfterPayment(userId: String, durationMonths: Int) {
        try {
            Log.d(TAG, "Starting updateVipStatusAfterPayment for user $userId with $durationMonths months")
            
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { userDoc ->
                    if (!userDoc.exists()) {
                        Log.e(TAG, "User document does not exist for $userId")
                        return@addOnSuccessListener
                    }
                    
                    Log.d(TAG, "Retrieved user document: ${userDoc.data}")
                    
                    // Lấy ngày hết hạn hiện tại hoặc thời gian hiện tại nếu chưa có
                    val currentExpireDate = when (val expireDate = userDoc.get("vipExpireDate")) {
                        is com.google.firebase.Timestamp -> {
                            val time = expireDate.toDate().time
                            Log.d(TAG, "Current vipExpireDate is Timestamp: $time")
                            time
                        }
                        is Long -> {
                            Log.d(TAG, "Current vipExpireDate is Long: $expireDate")
                            expireDate
                        }
                        else -> {
                            Log.d(TAG, "No vipExpireDate found, using current time")
                            System.currentTimeMillis()
                        }
                    }
                    
                    val calendar = Calendar.getInstance()
                    val currentTime = System.currentTimeMillis()
                    
                    // Nếu VIP đã hết hạn, tính từ hiện tại, nếu chưa hết hạn, cộng thêm vào thời gian hết hạn
                    calendar.timeInMillis = if (currentExpireDate > currentTime) {
                        Log.d(TAG, "VIP not expired, adding to current expiry date")
                        currentExpireDate
                    } else {
                        Log.d(TAG, "VIP expired or not set, starting from current time")
                        currentTime
                    }
                    
                    calendar.add(Calendar.MONTH, durationMonths)
                    val newExpireDate = calendar.timeInMillis
                    
                    Log.d(TAG, "New VIP expire date: $newExpireDate (${java.util.Date(newExpireDate)})")
                    
                    // Cập nhật trạng thái VIP
                    val updates = mapOf(
                        "isVip" to true,
                        "vipExpireDate" to newExpireDate
                    )
                    
                    Log.d(TAG, "Updating Firestore with: $updates")
                    
                    firestore.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "VIP status updated successfully for user $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating VIP status after payment", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting user data for VIP update", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateVipStatusAfterPayment", e)
        }
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return try {
            ZaloPaySDK.getInstance().onResult(data)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleActivityResult", e)
            false
        }
    }


    override suspend fun updateVipStatus(userId: String, durationMonths: Int): Result<Boolean> {
        return try {
            Log.d(TAG, "updateVipStatus called for user $userId with $durationMonths months")
            
            val userRef = firestore.collection("users").document(userId)
            val userDoc = userRef.get().await()
            
            if (!userDoc.exists()) {
                Log.e(TAG, "User document does not exist for $userId")
                return Result.failure(Exception("User document not found"))
            }
            
            // Lấy ngày hết hạn hiện tại hoặc thời gian hiện tại nếu chưa có
            val currentExpireDate = when (val expireDate = userDoc.get("vipExpireDate")) {
                is com.google.firebase.Timestamp -> {
                    val time = expireDate.toDate().time
                    Log.d(TAG, "Current vipExpireDate is Timestamp: $time")
                    time
                }
                is Long -> {
                    Log.d(TAG, "Current vipExpireDate is Long: $expireDate")
                    expireDate
                }
                else -> {
                    Log.d(TAG, "No vipExpireDate found, using current time")
                    System.currentTimeMillis()
                }
            }
            
            val calendar = Calendar.getInstance()
            val currentTime = System.currentTimeMillis()
            
            // Nếu VIP đã hết hạn, tính từ hiện tại, nếu chưa hết hạn, cộng thêm vào thời gian hết hạn
            calendar.timeInMillis = if (currentExpireDate > currentTime) {
                Log.d(TAG, "VIP not expired, adding to current expiry date")
                currentExpireDate
            } else {
                Log.d(TAG, "VIP expired or not set, starting from current time")
                currentTime
            }
            
            calendar.add(Calendar.MONTH, durationMonths)
            val newExpireDate = calendar.timeInMillis
            
            Log.d(TAG, "New VIP expire date: $newExpireDate (${java.util.Date(newExpireDate)})")
            
            // Cập nhật trạng thái VIP
            userRef.update(
                mapOf(
                    "isVip" to true,
                    "vipExpireDate" to newExpireDate
                )
            ).await()
            
            Log.d(TAG, "VIP status updated for user $userId, new expire date: $newExpireDate")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating VIP status", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getVipStatus(userId: String): Result<Pair<Boolean, Long>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            val isVip = userDoc.getBoolean("isVip") ?: false
            val vipExpireDate = when (val expireDate = userDoc.get("vipExpireDate")) {
                is com.google.firebase.Timestamp -> expireDate.toDate().time
                is Long -> expireDate
                else -> 0L
            }
            
            Result.success(Pair(isVip, vipExpireDate))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting VIP status", e)
            Result.failure(e)
        }
    }
    
    override suspend fun checkVipExpiration(userId: String): Result<Boolean> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            val isVip = userDoc.getBoolean("isVip") ?: false
            if (!isVip) return Result.success(false)
            
            val vipExpireDate = when (val expireDate = userDoc.get("vipExpireDate")) {
                is com.google.firebase.Timestamp -> expireDate.toDate().time
                is Long -> expireDate
                else -> 0L
            }
            
            val currentTime = System.currentTimeMillis()
            val isExpired = vipExpireDate < currentTime
            
            if (isExpired) {
                // Cập nhật trạng thái VIP thành false nếu đã hết hạn
                firestore.collection("users").document(userId)
                    .update("isVip", false)
                    .await()
                
                Log.d(TAG, "VIP status expired for user $userId")
                Result.success(false)
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VIP expiration", e)
            Result.failure(e)
        }
    }

    override fun getMonthsForToken(token: String): Int? {
        // Kiểm tra trong paymentMonths trước, nếu không có thì kiểm tra trong processedPayments
        return paymentMonths[token] ?: processedPayments[token]
    }

    override fun saveMonthsForToken(token: String, months: Int) {
        paymentMonths[token] = months
        Log.d(TAG, "Saved $months months for token $token")
    }
}



