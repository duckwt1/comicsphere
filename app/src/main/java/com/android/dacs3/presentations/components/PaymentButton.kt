package com.android.dacs3.presentations.components

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.dacs3.utliz.CreateOrder
import kotlinx.coroutines.launch
import org.json.JSONObject
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener

@Composable
fun PaymentButton(
    amount: String,
    description: String = "Thanh toán VIP ComicSphere",
    onPaymentInitiated: () -> Unit = {},
    onPaymentSuccess: (String) -> Unit = {},
    onPaymentError: (String) -> Unit = {},
    onPaymentCanceled: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Button(
        onClick = {
            coroutineScope.launch {
                try {
                    // Tạo đơn hàng
                    val amountLong = (amount.toDoubleOrNull() ?: 0.0).toLong()
                    val orderData = CreateOrder().createOrder(amountLong.toString())
                    val token = orderData?.getString("zp_trans_token")
                    if (token == null) {
                        onPaymentError("Không lấy được token giao dịch từ ZaloPay")
                        return@launch
                    }
                    
                    // Thông báo đã bắt đầu thanh toán
                    onPaymentInitiated()
                    
                    // Gọi ZaloPay để thanh toán
                    ZaloPaySDK.getInstance().payOrder(
                        context as Activity,
                        token,
                        "comicsphere://zalopay.callback",
                        object : PayOrderListener {
                            override fun onPaymentSucceeded(
                                transactionId: String,
                                transToken: String,
                                appTransID: String
                            ) {
                                onPaymentSuccess(transactionId)
                            }
                            
                            override fun onPaymentCanceled(
                                zpTransToken: String,
                                appTransID: String
                            ) {
                                onPaymentCanceled()
                            }
                            
                            override fun onPaymentError(
                                zaloPayError: vn.zalopay.sdk.ZaloPayError,
                                zpTransToken: String,
                                appTransID: String
                            ) {
                                onPaymentError(zaloPayError.toString())
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("PaymentButton", "Error processing payment", e)
                    onPaymentError(e.message ?: "Lỗi không xác định")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00D09E),
            contentColor = Color.White
        )
    ) {
        Text(
            text = "Thanh toán với ZaloPay",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}