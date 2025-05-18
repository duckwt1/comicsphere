package com.android.dacs3.utliz

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.util.Date
import java.util.LinkedList
import java.util.Locale

object EnvConfig {
    const val SANDBOX = "sandbox"
    const val PRODUCTION = "production"
}

object AppInfo {
    const val APP_ID = 2553
    const val MAC_KEY = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL"
    const val URL_CREATE_ORDER = "https://sb-openapi.zalopay.vn/v2/create"
}

object HexStringUtil {
    private val HEX_CHAR_TABLE = byteArrayOf(
        '0'.code.toByte(), '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(),
        '4'.code.toByte(), '5'.code.toByte(), '6'.code.toByte(), '7'.code.toByte(),
        '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte(), 'b'.code.toByte(),
        'c'.code.toByte(), 'd'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()
    )

    // Chuyển đổi mảng byte thành chuỗi hex
    fun byteArrayToHexString(raw: ByteArray): String {
        val hex = ByteArray(2 * raw.size)
        var index = 0

        for (b in raw) {
            val v = b.toInt() and 0xFF
            hex[index++] = HEX_CHAR_TABLE[v ushr 4]
            hex[index++] = HEX_CHAR_TABLE[v and 0xF]
        }
        return String(hex)
    }

    // Chuyển đổi chuỗi hex thành mảng byte
    fun hexStringToByteArray(hex: String): ByteArray {
        val hexStandard = hex.lowercase(Locale.ENGLISH)
        val sz = hexStandard.length / 2
        val bytesResult = ByteArray(sz)

        var idx = 0
        for (i in 0 until sz) {
            // Convert char to byte and process
            bytesResult[i] = (hexStandard[idx]).toByte()
            idx++
            var tmp = (hexStandard[idx]).toByte()
            idx++

            // So sánh với HEX_CHAR_TABLE[9] và xử lý ký tự 'a'-'f' và '0'-'9'
            bytesResult[i] = if (bytesResult[i].toInt() > HEX_CHAR_TABLE[9].toInt()) {
                (bytesResult[i].toInt() - ('a' - 10).toInt()).toByte()
            } else {
                (bytesResult[i].toInt() - '0'.code).toByte()
            }

            tmp = if (tmp.toInt() > HEX_CHAR_TABLE[9].toInt()) {
                (tmp.toInt() - ('a' - 10).toInt()).toByte()
            } else {
                (tmp.toInt() - '0'.code).toByte()
            }

            // Ghép 2 byte thành 1 byte
            bytesResult[i] = ((bytesResult[i].toInt() * 16 + tmp.toInt()) % 256).toByte()
        }
        return bytesResult
    }
}



object HMacUtil {
    const val HMACMD5 = "HmacMD5"
    const val HMACSHA1 = "HmacSHA1"
    const val HMACSHA256 = "HmacSHA256"
    const val HMACSHA512 = "HmacSHA512"
    val UTF8CHARSET = StandardCharsets.UTF_8

    val HMACS = LinkedList(
        listOf("UnSupport", "HmacSHA256", "HmacMD5", "HmacSHA384", "HMacSHA1", "HmacSHA512")
    )

    // Hàm HMacEncode
    private fun HMacEncode(algorithm: String, key: String, data: String): ByteArray? {
        var macGenerator: Mac? = null
        try {
            macGenerator = Mac.getInstance(algorithm)
            val signingKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), algorithm)
            macGenerator.init(signingKey)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        macGenerator?.let {
            return it.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun HMacBase64Encode(algorithm: String, key: String, data: String): String? {
        val hmacEncodeBytes = HMacEncode(algorithm, key, data)
        return hmacEncodeBytes?.let {
            Base64.getEncoder().encodeToString(it)
        }
    }

    fun HMacHexStringEncode(algorithm: String, key: String, data: String): String? {
        val hmacEncodeBytes = HMacEncode(algorithm, key, data)
        return hmacEncodeBytes?.let {
            HexStringUtil.byteArrayToHexString(it)
        }
    }
}

object Helpers {
    private var transIdDefault = 1

    @SuppressLint("DefaultLocale")
    fun getAppTransId(): String {
        if (transIdDefault >= 100000) {
            transIdDefault = 1
        }

        transIdDefault += 1
        val formatDateTime = SimpleDateFormat("yyMMdd_hhmmss", Locale.getDefault())
        val timeString = formatDateTime.format(Date())
        return String.format("%s%06d", timeString, transIdDefault)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun getMac(key: String, data: String): String {
        return HMacUtil.HMacHexStringEncode(HMacUtil.HMACSHA256, key, data)
            ?: throw IllegalArgumentException("Unable to generate MAC")
    }
}

object AdminConfig {
    const val ADMIN_EMAIL = "admin@comicsphere.com"
    const val ADMIN_PASSWORD = "admin123456" // Nên thay đổi thành mật khẩu mạnh hơn
}
