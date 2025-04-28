package com.android.dacs3.data.model

data class MangaListResponse(
    val result: String,
    val data: List<MangaData>
)

data class MangaDetailResponse(
    val result: String,
    val data: MangaData
)

data class MangaData(
    val id: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList()
)

data class MangaAttributes(
    val title: Map<String, String>,
    val altTitles: List<Map<String, String>> = emptyList(),
    val availableTranslatedLanguages: List<String> = emptyList(),
    val description: Map<String, String>,
    val status: String?,
    val tags: List<TagWrapper> = emptyList()
)

data class MangaDetailUiState(
    val title: String = "",
    val titles: Map<String, String> = emptyMap(),
    val description: String = "",
    val descriptions: Map<String, String> = emptyMap(),
    val author: String = "",
    val status: String = "",
    val genres: List<String> = emptyList(),
    val coverImageUrl: String? = null
)


data class Relationship(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributes? = null
)

data class RelationshipAttributes(
    val fileName: String? = null,
    val name: String? = null
)

val MangaData.coverImageUrl: String?
    get() {
        val coverArt = relationships.find { it.type == "cover_art" }
        val fileName = coverArt?.attributes?.fileName
        return fileName?.let { "https://uploads.mangadex.org/covers/$id/$it.512.jpg" }
    }
