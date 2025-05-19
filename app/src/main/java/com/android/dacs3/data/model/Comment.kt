package com.android.dacs3.data.model

data class Comment(
    val id: String = "",
    val userId: String = "",
    val mangaId: String = "",
    val comment: String = "",
    val timestamp: Long = 0,
    val likes: Int = 0,
    val isEdited: Boolean = false,
    val nickname: String = "Anonymous",
    val avatar: String? = null
)
