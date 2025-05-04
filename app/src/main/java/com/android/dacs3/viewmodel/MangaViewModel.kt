package com.android.dacs3.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.model.*
import com.android.dacs3.data.repository.MangaRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MangaViewModel @Inject constructor(
    private val repository: MangaRepository,
    private val firebaseAuth: FirebaseAuth
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

    private val _lastReadChapter = MutableStateFlow<Pair<String, Int>?>(null)
    val lastReadChapter: StateFlow<Pair<String, Int>?> = _lastReadChapter.asStateFlow()

    private val _mangaDetails = MutableStateFlow<Map<String, MangaData>>(emptyMap())
    val mangaDetails: StateFlow<Map<String, MangaData>> = _mangaDetails


    // Load chapter details
    private val _chapterDetails = MutableStateFlow<Map<String, ChapterData>>(emptyMap())
    val chapterDetails: StateFlow<Map<String, ChapterData>> = _chapterDetails

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

    fun saveReadingProgress(
        mangaId: String,
        chapterId: String,
        language: String,
        lastPageIndex: Int
    ) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                val result = repository.saveReadingProgress(userId, mangaId, chapterId, language, lastPageIndex)
                result.onSuccess {
                    Log.d("MangaViewModel", "Reading progress saved successfully")
                }.onFailure {
                    Log.e("MangaViewModel", "Error saving reading progress", it)
                }
            }
        } else {
            Log.e("MangaViewModel", "User not authenticated")
        }
    }

    fun getLastReadChapter(mangaId: String, language: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = repository.getLastReadChapter(userId, mangaId, language)
            result.onSuccess { (chapterId, lastPageIndex) ->
                _lastReadChapter.value = Pair(chapterId, lastPageIndex)
                Log.d("MangaViewModel", "Last read chapter: $chapterId, page: $lastPageIndex")
            }
            result.onFailure {
                Log.e("MangaViewModel", "Failed to get last read chapter", it)
            }
        }
    }

    private val _readingProgress = MutableStateFlow<List<ReadingProgress>>(emptyList())
    val readingProgress: StateFlow<List<ReadingProgress>> = _readingProgress

    fun loadReadingProgress(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                // Nếu không cần refresh và đã có dữ liệu, không gọi API
                if (!forceRefresh && _readingProgress.value.isNotEmpty()) {
                    Log.d("MangaViewModel", "Using cached reading progress")
                    return@launch
                }

                val userId = firebaseAuth.currentUser?.uid ?: return@launch

                Log.d("MangaViewModel", "Starting to load reading progress")
                repository.getReadingProgress(userId).onSuccess { progress ->
                    Log.d("MangaViewModel", "Successfully loaded ${progress.size} reading progress items")
                    _readingProgress.value = progress

                    // Lấy lần đọc gần nhất của mỗi manga
                    val latestProgressByManga = progress
                        .groupBy { it.mangaId }
                        .mapValues { entry -> entry.value.maxByOrNull { it.timestamp }!! }
                        .values
                        .sortedByDescending { it.timestamp }

                    Log.d("MangaViewModel", "Found ${latestProgressByManga.size} latest progress items")

                    // Load manga details cho mỗi progress
                    latestProgressByManga.forEach { progressItem ->
                        if (!_mangaDetails.value.containsKey(progressItem.mangaId)) {
                            try {
                                Log.d("MangaViewModel", "Loading manga details for ${progressItem.mangaId}")
                                repository.getMangaById(progressItem.mangaId).onSuccess { response ->
                                    response.data?.let { mangaData ->
                                        _mangaDetails.update { currentMap ->
                                            currentMap + (mangaData.id to mangaData)
                                        }
                                        Log.d("MangaViewModel", "Successfully loaded manga details for ${mangaData.id}")
                                    }
                                }.onFailure { e ->
                                    Log.e("MangaViewModel", "Error loading manga details for ${progressItem.mangaId}", e)
                                }
                            } catch (e: Exception) {
                                Log.e("MangaViewModel", "Unexpected error loading manga details", e)
                            }
                        }
                    }

                    delay(500) // Đợi một chút để manga details được load xong

                    // Load chapter details cho mỗi progress
                    latestProgressByManga.forEach { progressItem ->
                        if (!_chapterDetails.value.containsKey(progressItem.chapterId)) {
                            try {
                                Log.d("MangaViewModel", "Loading chapters for ${progressItem.mangaId}")
                                // Thử load chapter cụ thể trước
                                loadChapterWithPagination(progressItem.mangaId, progressItem.language, progressItem.chapterId)
                            } catch (e: Exception) {
                                Log.e("MangaViewModel", "Unexpected error loading chapters", e)
                            }
                        }
                    }
                }.onFailure { e ->
                    Log.e("MangaViewModel", "Error loading reading progress", e)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Unexpected error in loadReadingProgress", e)
            }
        }
    }

    fun deleteReadingProgress(mangaId: String, chapterId: String, language: String) {
        viewModelScope.launch {
            try {
                val userId = firebaseAuth.currentUser?.uid ?: return@launch
                
                Log.d("MangaViewModel", "Deleting reading progress for manga=$mangaId, chapter=$chapterId")
                
                repository.deleteReadingProgress(userId, mangaId, chapterId, language).onSuccess {
                    Log.d("MangaViewModel", "Successfully deleted reading progress")
                    // Reload reading progress after deletion
                    loadReadingProgress(true)
                }.onFailure { e ->
                    Log.e("MangaViewModel", "Error deleting reading progress", e)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Unexpected error in deleteReadingProgress", e)
            }
        }
    }

    private suspend fun loadChapterWithPagination(mangaId: String, language: String, targetChapterId: String) {
        var offset = 0
        val limit = 100
        var found = false
        var hasMore = true

        while (hasMore && !found) {
            repository.getMangaChapters(mangaId, language, limit, offset).onSuccess { chapters ->
                if (chapters.isEmpty()) {
                    hasMore = false
                    return@onSuccess
                }

                val chapterMap = chapters.associateBy { it.id }
                _chapterDetails.update { currentMap ->
                    currentMap + chapterMap
                }

                if (chapterMap.containsKey(targetChapterId)) {
                    found = true
                    Log.d("MangaViewModel", "Found target chapter $targetChapterId at offset $offset")
                } else {
                    offset += limit
                    Log.d("MangaViewModel", "Chapter $targetChapterId not found in current batch, trying next batch")
                }
            }.onFailure { e ->
                Log.e("MangaViewModel", "Error loading chapters for $mangaId at offset $offset", e)
                hasMore = false
            }
        }

        if (!found) {
            Log.e("MangaViewModel", "Chapter $targetChapterId not found in any batch for manga $mangaId")
        }
    }

    fun formatHistoryDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown Date"

        val now = Calendar.getInstance()
        val input = Calendar.getInstance().apply { timeInMillis = timestamp }

        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        val inputDate = sdf.format(input.time)
        val todayDate = sdf.format(now.time)

        now.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDate = sdf.format(now.time)

        // Reset lại now sau khi trừ ngày hôm qua
        now.add(Calendar.DAY_OF_YEAR, 1)

        val diffInMillis = now.timeInMillis - input.timeInMillis
        val daysDiff = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

        return when {
            inputDate == todayDate -> "Today"
            inputDate == yesterdayDate -> "Yesterday"
            daysDiff in 2..6 -> "$daysDiff days ago"
            else -> inputDate
        }
    }



}
