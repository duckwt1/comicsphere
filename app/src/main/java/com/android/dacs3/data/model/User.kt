package com.android.dacs3.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val fullname: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val createdAt: Timestamp? = null,
    val vipExpireDate: Long = 0, // Thay đổi từ Timestamp sang Long
    val isVip: Boolean = false,
)

// Extension function để chuyển đổi từ Timestamp sang Long nếu cần
fun Timestamp?.toLongOrDefault(default: Long = 0): Long {
    return this?.toDate()?.time ?: default
}
