package com.android.dacs3.data.model

data class ReadingProgress(
    val mangaId: String,
    val chapterId: String,
    val language: String,
    val lastPageIndex: Int,
    val timestamp: Long
) 