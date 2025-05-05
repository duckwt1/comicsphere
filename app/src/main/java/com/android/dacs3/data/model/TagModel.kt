package com.android.dacs3.data.model

data class TagWrapper(
    val id: String,
    val type: String,
    val attributes: TagAttributes
)

data class TagAttributes(
    val name: Map<String, String>,
    val description: Map<String, String>,
    val group: String
)

data class TagListResponse(
    val result: String,
    val data: List<TagWrapper>
)
