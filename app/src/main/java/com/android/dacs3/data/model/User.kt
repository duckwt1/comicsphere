package com.android.dacs3.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val fullname: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
)
