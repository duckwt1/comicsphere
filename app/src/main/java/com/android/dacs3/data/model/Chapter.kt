package com.android.dacs3.data.model


data class ChapterListResponse(
    val data: List<ChapterData>
)

data class ChapterData(
    val id: String,
    val attributes: ChapterAttributes
)

data class ChapterContentResponse(
    val baseUrl: String,
    val chapter: ChapterPageData
)

data class ChapterPageData(
    val hash: String,
    val data: List<String>
)


data class ChapterAttributes(
    val chapter: String?,
    val title: String?,
    val translatedLanguage: String,
    val publishAt: String
)
