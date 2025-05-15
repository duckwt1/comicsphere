package com.android.dacs3.utliz

import android.util.Log
import com.android.dacs3.utliz.AppInfo.URL_CREATE_ORDER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Date


class CreateOrder {
    suspend fun createOrder(amount: String): JSONObject? {
        val input = CreateOrderData.create(
            amount.toDouble().toInt().toString()
        )  // Tạo đối tượng CreateOrderData từ amount

        // Tạo RequestBody từ FormBody
        val formBody: RequestBody = FormBody.Builder()
            .add("app_id", input.AppId)
            .add("app_user", input.AppUser)
            .add("app_time", input.AppTime)
            .add("amount", input.Amount)
            .add("app_trans_id", input.AppTransId)
            .add("embed_data", input.EmbedData)
            .add("item", input.Items)
            .add("bank_code", input.BankCode)
            .add("description", input.Description)
            .add("mac", input.Mac)
            .build()

        return try {
            withContext(Dispatchers.IO) {
                Log.d("url", URL_CREATE_ORDER)
                HttpProvider.sendPost(AppInfo.URL_CREATE_ORDER, formBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Ghi đè phương thức toString để hiển thị thông tin của CreateOrder
    override fun toString(): String {
        // Lấy dữ liệu mặc định từ CreateOrderData (dùng một amount cụ thể)
        val createOrderData = CreateOrderData.create("1000") // Thay đổi amount nếu cần thiết
        return "CreateOrder(CreateOrderData=${createOrderData.toString()})"
    }
}


data class CreateOrderData(
    val AppId: String,
    val AppUser: String,
    val AppTime: String,
    val Amount: String,
    val AppTransId: String,
    val EmbedData: String,
    val Items: String,
    val BankCode: String,
    val Description: String,
    val Mac: String
) {
    companion object {
        // Method to create CreateOrderData object from amount
        fun create(amount: String): CreateOrderData {
            val appTime = Date().time
            val appId = AppInfo.APP_ID.toString()
            val appUser = "Android_Demo"
            val appTransId = Helpers.getAppTransId()
            val embedData = "{}"
            val items = "[]"
            val bankCode = "zalopayapp"
            val description = "Merchant pay for order #${Helpers.getAppTransId()}"

            val inputHMac =
                "${appId}|${appTransId}|${appUser}|${amount}|${appTime}|${embedData}|${items}"
            val mac = Helpers.getMac(AppInfo.MAC_KEY, inputHMac)
            Log.d(
                "ZaloPayPayload", """
                        app_id=$appId
                        app_user=$appUser
                        app_time=$appTime
                        amount=$amount
                        app_trans_id=$appTransId
                        embed_data=$embedData
                        items=$items
                        description=$description
                        mac=$mac
                        inputHMac=$inputHMac
                    """.trimIndent()
            )


            return CreateOrderData(
                AppId = appId,
                AppUser = appUser,
                AppTime = appTime.toString(),
                Amount = amount,
                AppTransId = appTransId,
                EmbedData = embedData,
                Items = items,
                BankCode = bankCode,
                Description = description,
                Mac = mac
            )
        }
    }

    override fun toString(): String {
        return "CreateOrderData(AppId='$AppId', AppUser='$AppUser', AppTime='$AppTime', Amount='$Amount', AppTransId='$AppTransId', EmbedData='$EmbedData', Items='$Items', BankCode='$BankCode', Description='$Description', Mac='$Mac')"
    }
}