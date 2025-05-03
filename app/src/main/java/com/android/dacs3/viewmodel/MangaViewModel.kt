package com.android.dacs3.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.model.*
import com.android.dacs3.data.repository.MangaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaViewModel @Inject constructor(
    private val repository: MangaRepository
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

    // Chapter page navigation
    private val _chapterImageUrls = MutableStateFlow<List<String>>(emptyList())
    val chapterImageUrls: StateFlow<List<String>> = _chapterImageUrls

    private val _currentPageReading = MutableStateFlow(1)
    val currentPageReading: StateFlow<Int> = _currentPageReading

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages

    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages: StateFlow<List<String>> = _availableLanguages

    var currentChapterId by mutableStateOf<String?>(null)
        private set

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

                val result = repository.fetchMangaList(pageSize, currentPage * pageSize)
                result.onSuccess {
                    _mangas.value = _mangas.value + it.data
                    currentPage++
                }.onFailure {
                    Log.e("MangaViewModel", "Error fetching manga list", it)
                }

            } finally {
                isLoading = false
                if (reset) _isRefreshing.value = false
            }
        }
    }

    fun searchManga(title: String) {
        viewModelScope.launch {
            val result = repository.searchManga(title)
            result.onSuccess {
                _mangas.value = it.data
            }.onFailure {
                Log.e("MangaViewModel", "Error searching manga", it)
            }
        }
    }

    fun refreshMangaList() {
        _isRefreshing.value = true
        fetchMangaList(reset = true)
    }

    fun loadMangaDetails(mangaId: String) {
        viewModelScope.launch {
            val result = repository.getMangaById(mangaId)
            result.onSuccess { response ->
                if (response.data == null) {
                    _mangaDetail.update { MangaDetailUiState(title = "Manga not found") }
                    return@onSuccess
                }

                val attributes = response.data.attributes
                val availableLanguages = attributes.availableTranslatedLanguages.distinct()
                _availableLanguages.value = availableLanguages

                val selectedTitle = attributes.title[_selectedLanguage.value]
                    ?: attributes.altTitles.firstOrNull { it[_selectedLanguage.value] != null }?.get(_selectedLanguage.value)
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
            }.onFailure {
                Log.e("MangaViewModel", "Error loading manga details", it)
                _mangaDetail.update { MangaDetailUiState(title = "Error loading manga details") }
            }
        }
    }

    fun changeLanguage(language: String) {
        _selectedLanguage.value = language
    }

    private fun buildCoverUrl(response: MangaDetailResponse): String? {
        val coverFileName = response.data.relationships.firstOrNull { it.type == "cover_art" }?.attributes?.fileName
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
                val result = repository.getMangaChapters(mangaId, language, limit, offset)
                result.onSuccess { newChapters ->
                    allChapters.addAll(newChapters)
                    hasMore = newChapters.isNotEmpty()
                    offset += limit
                }.onFailure {
                    Log.e("MangaViewModel", "Error loading chapters", it)
                    hasMore = false
                }
            }

            _chapters.value = allChapters
        }
    }

    fun loadChapterContent(chapterId: String) {
        viewModelScope.launch {
            val result = repository.getChapterContent(chapterId)
            result.onSuccess { response ->
                val baseUrl = response.baseUrl
                val hash = response.chapter.hash
                val imageUrls = response.chapter.data.map { "$baseUrl/data/$hash/$it" }

                _chapterImageUrls.value = imageUrls
                _totalPages.value = imageUrls.size
                _currentPageReading.value = 1
            }.onFailure {
                Log.e("MangaViewModel", "Error loading chapter content", it)
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
                if (_chapters.value.isEmpty()) {
                    loadChapters(mangaId, language)
                    while (_chapters.value.isEmpty()) {
                        delay(100)
                    }
                }

                val currentIndex = _chapters.value.indexOfFirst { it.id == currentChapterId }
                if (currentIndex != -1 && currentIndex < _chapters.value.size - 1) {
                    val nextChapter = _chapters.value[currentIndex + 1]
                    onChapterLoaded(nextChapter.id)
                } else {
                    onChapterLoaded(null)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error loading next chapter", e)
                onChapterLoaded(null)
            }
        }
    }

    fun setChapter(chapterId: String) {
        currentChapterId = chapterId
        loadChapterContent(chapterId)
    }
}
