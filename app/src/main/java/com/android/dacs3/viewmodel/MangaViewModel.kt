package com.android.dacs3.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.api.MangaDexApi
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.MangaDetailResponse
import com.android.dacs3.data.model.MangaDetailUiState
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

    private val _chapters = MutableStateFlow<List<ChapterData>>(emptyList())
    val chapters: StateFlow<List<ChapterData>> = _chapters

    private var currentPage = 0
    private val pageSize = 15
    private var isLoading = false

    // --- For ChapterScreen ---
    private val _chapterImageUrls = MutableStateFlow<List<String>>(emptyList())
    val chapterImageUrls: StateFlow<List<String>> = _chapterImageUrls

    private val _currentPageReading = MutableStateFlow(1)
    val currentPageReading: StateFlow<Int> = _currentPageReading

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages


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

    fun searchManga(title: String) {
        viewModelScope.launch {
            try {
                val result = api.searchManga(title)
                _mangas.value = result.data
            } catch (e: Exception) {
                e.printStackTrace()
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

                if (response.data == null) {
                    Log.w("MangaViewModel", "No manga data found for ID: $mangaId")
                    _mangaDetail.update { MangaDetailUiState(title = "Manga not found") }
                    return@launch
                }

                val attributes = response.data.attributes

                val availableLanguages = attributes.availableTranslatedLanguages.distinct()
                _availableLanguages.value = availableLanguages

                val selectedTitle = attributes.title[_selectedLanguage.value]
                    ?: attributes.altTitles.firstOrNull { it[_selectedLanguage.value] != null }
                        ?.get(_selectedLanguage.value)
                    ?: attributes.title["en"]
                    ?: attributes.altTitles.firstOrNull { it["en"] != null }?.get("en")
                    ?: "No title available"

                val rawDescription = attributes.description[_selectedLanguage.value]
                    ?: attributes.description["en"]
                    ?: "No description available"

                val selectedDescription = rawDescription.split("---")[0].trim()


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

    fun loadChapters(mangaId: String, language: String) {
        viewModelScope.launch {
            val allChapters = mutableListOf<ChapterData>()
            var offset = 0
            val limit = 100
            var hasMore = true

            while (hasMore) {
                try {
                    val response = api.getMangaChapters(
                        mangaId = mangaId,
                        translatedLanguage = listOf(language),
                        order = "asc",
                        limit = limit,
                        offset = offset
                    )
                    val newChapters = response.data
                    allChapters.addAll(newChapters)

                    hasMore = newChapters.isNotEmpty()
                    offset += limit
                } catch (e: Exception) {
                    Log.e("MangaViewModel", "Error loading paged chapters", e)
                    hasMore = false
                }
            }

            _chapters.value = allChapters
            Log.d("MangaViewModel", "Loaded ${allChapters.size} chapters")
        }
    }


    fun loadChapterContent(chapterId: String) {
        Log.d("DEBUG", "Loading content for chapterId=$chapterId")

        viewModelScope.launch {
            try {
                val response = api.getChapterContent(chapterId)
                val baseUrl = response.baseUrl
                val hash = response.chapter.hash
                val imageUrls = response.chapter.data.map { filename ->
                    "$baseUrl/data/$hash/$filename"
                }
                _chapterImageUrls.value = imageUrls
                _totalPages.value = imageUrls.size
                _currentPageReading.value = 1
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error loading chapter content", e)
            }
        }
    }

    fun updateCurrentPage(page: Int) {
        _currentPageReading.value = page.coerceIn(1, _totalPages.value)
    }

    fun loadNextChapter(
        currentChapterId: String,
        mangaId: String,
        language: String,
        onChapterLoaded: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Reload chapters only if `_chapters` is empty
                if (_chapters.value.isEmpty()) {
                    loadChapters(mangaId, language)
                }

                // Find the index of the current chapter
                val currentIndex = _chapters.value.indexOfFirst { it.id == currentChapterId }

                // Check if the next chapter exists
                if (currentIndex != -1 && currentIndex < _chapters.value.size - 1) {
                    val nextChapter = _chapters.value[currentIndex + 1]
                    loadChapterContent(nextChapter.id)
                    onChapterLoaded(nextChapter.id)
                } else {
                    Log.d("MangaViewModel", "No more chapters available")
                    onChapterLoaded(null)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error loading next chapter", e)
                onChapterLoaded(null)
            }
        }
    }


}


