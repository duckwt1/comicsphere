package com.android.dacs3.data.model

import com.google.firebase.Timestamp

data class ReadingProgress(
    val mangaId: String = "",
    val chapterId: String = "",
    val lastPageIndex: Int = 0,
    val timestamp: Timestamp? = null
)
