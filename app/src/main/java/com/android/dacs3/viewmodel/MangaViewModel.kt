package com.android.dacs3.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.api.MangaDexApi
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaDetailUiState
import com.android.dacs3.data.model.TagWrapper
import com.android.dacs3.data.model.coverImageUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaViewModel @Inject constructor(
    private val api: MangaDexApi
) : ViewModel() {

    private val _mangas = MutableStateFlow<List<MangaData>>(emptyList())
    val mangas: StateFlow<List<MangaData>> = _mangas

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _mangaDetail = MutableStateFlow(MangaDetailUiState())
    val mangaDetail: StateFlow<MangaDetailUiState> = _mangaDetail

    private var currentPage = 0
    private val pageSize = 15
    private var isLoading = false

    init {
        fetchMangaList()
    }

    fun fetchMangaList(reset: Boolean = false) {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                if (reset) {
                    currentPage = 0
                    _mangas.value = emptyList()
                }

                val result = api.getMangaList(
                    limit = pageSize, offset = currentPage * pageSize
                )

                _mangas.value = _mangas.value + result.data
                currentPage++

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
                if (reset) _isRefreshing.value = false
            }
        }
    }

    fun refreshMangaList() {
        _isRefreshing.value = true
        fetchMangaList(reset = true)
    }

    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()
    private val _availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages: StateFlow<List<String>> = _availableLanguages

    fun loadMangaDetails(mangaId: String) {
        Log.d("MangaViewModel", "Loading manga details for ID: $mangaId")
        viewModelScope.launch {
            try {
                val response = api.getMangaById(mangaId)
                Log.d("MangaViewModel", "API Response: $response")

                if (response.data == null) {
                    Log.w("MangaViewModel", "No manga data found for ID: $mangaId")
                    _mangaDetail.update { MangaDetailUiState(title = "Manga not found") }
                    return@launch
                }

                val attributes = response.data.attributes

                // Merge languages from title and altTitles
                val mergedLanguages = (attributes.title.keys + attributes.altTitles.flatMap { it.keys }).distinct()
                _availableLanguages.value = mergedLanguages

                // Select title based on the selected language
                val selectedTitle = attributes.title[_selectedLanguage.value]
                    ?: attributes.altTitles.firstOrNull { it[_selectedLanguage.value] != null }?.get(_selectedLanguage.value)
                    ?: attributes.title["en"]
                    ?: attributes.altTitles.firstOrNull { it["en"] != null }?.get("en")
                    ?: "No title available"

                // Select description based on the selected language
                val selectedDescription = attributes.description[_selectedLanguage.value]
                    ?: attributes.description["en"]
                    ?: "No description available"

                _mangaDetail.update {
                    MangaDetailUiState(
                        title = selectedTitle,
                        description = selectedDescription,
                        status = attributes.status ?: "Unknown",
                        author = response.data.relationships.firstOrNull { it.type == "author" }?.attributes?.name.orEmpty(),
                        coverImageUrl = buildCoverUrl(response),
                        genres = extractGenres(response),
                        titles = attributes.title,
                        descriptions = attributes.description
                    )
                }

            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error loading manga details", e)
                _mangaDetail.update { MangaDetailUiState(title = "Error loading manga details") }
            }
        }
    }



    fun changeLanguage(language: String) {
        _selectedLanguage.value = language
    }

    private fun buildCoverUrl(response: MangaDetailResponse): String? {
        val coverFileName =
            response.data.relationships.firstOrNull { it.type == "cover_art" }?.attributes?.fileName
        return coverFileName?.let {
            "https://uploads.mangadex.org/covers/${response.data.id}/$it.512.jpg"
        }
    }

    private fun extractGenres(response: MangaDetailResponse): List<String> {
        return response.data.attributes.tags.mapNotNull { it.attributes.name["en"] }
    }


}


