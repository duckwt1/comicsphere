package com.android.dacs3.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.model.*
import com.android.dacs3.data.repository.AuthRepository
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

@HiltViewModel
class MangaViewModel @Inject constructor(
    private val repository: MangaRepository,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _mangas = MutableStateFlow<List<MangaData>>(emptyList())
    val mangas: StateFlow<List<MangaData>> = _mangas

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _mangaDetail = MutableStateFlow(MangaDetailUiState())
    val mangaDetail: StateFlow<MangaDetailUiState> = _mangaDetail

    private val _chapters = MutableStateFlow<List<ChapterData>>(emptyList())
    val chapters: StateFlow<List<ChapterData>> = _chapters

    // Pagination for All Manga
    private var currentPage = 0
    private val pageSize = 15
    private var isLoading = false

    // Pagination for Trending Manga
    private var trendingPage = 0

    // Pagination for Recommended Manga
    private var recommendedPage = 0
    private var currentRecommendedTags: List<String> = emptyList()

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

    private val _lastReadChapter = MutableStateFlow<Pair<String, Int>?>(null)
    val lastReadChapter: StateFlow<Pair<String, Int>?> = _lastReadChapter.asStateFlow()

    private val _mangaDetails = MutableStateFlow<Map<String, MangaData>>(emptyMap())
    val mangaDetails: StateFlow<Map<String, MangaData>> = _mangaDetails


    // Load chapter details
    private val _chapterDetails = MutableStateFlow<Map<String, ChapterData>>(emptyMap())
    val chapterDetails: StateFlow<Map<String, ChapterData>> = _chapterDetails

    // Sửa lại các state flows cho comments
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    private val _commentActionInProgress = MutableStateFlow(false)
    val commentActionInProgress: StateFlow<Boolean> = _commentActionInProgress.asStateFlow()

    // Biến để lưu mangaId hiện tại
    private var currentMangaId: String? = null

    // Thêm các state để lưu thông tin người dùng
    private val _userDetails = MutableStateFlow<Map<String, User>>(emptyMap())
    val userDetails: StateFlow<Map<String, User>> = _userDetails.asStateFlow()

    // Thêm state để lưu trữ thông tin like của người dùng
    private val _userLikes = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val userLikes: StateFlow<Map<String, Boolean>> = _userLikes.asStateFlow()

    // Add a new state to track read chapters
    private val _readChapters = MutableStateFlow<Set<String>>(emptySet())
    val readChapters: StateFlow<Set<String>> = _readChapters.asStateFlow()

    // Add new states for chapter reading
    private val _isLoadingNextChapter = MutableStateFlow(false)
    val isLoadingNextChapter: StateFlow<Boolean> = _isLoadingNextChapter

    private val _showNextChapterButton = MutableStateFlow(false)
    val showNextChapterButton: StateFlow<Boolean> = _showNextChapterButton

    private val _showControls = MutableStateFlow(false)
    val showControls: StateFlow<Boolean> = _showControls

    private val _chapterTitle = MutableStateFlow("")
    val chapterTitle: StateFlow<String> = _chapterTitle

    private val _chapterError = MutableStateFlow<String?>(null)
    val chapterError: StateFlow<String?> = _chapterError

    // Thêm biến để theo dõi trạng thái tải dữ liệu từ Firestore
    private val _isLoadingFromFirestore = MutableStateFlow(false)
    val isLoadingFromFirestore: StateFlow<Boolean> = _isLoadingFromFirestore

    private val _firestoreTags = MutableStateFlow<List<Tag>>(emptyList())
    val firestoreTags: StateFlow<List<Tag>> = _firestoreTags

    init {
        fetchMangaList()
    }

    fun fetchMangaList(reset: Boolean = false) {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                _isLoadingFromFirestore.value = true
                _isRefreshing.value = true
                
                // Đảm bảo tags đã được tải
                if (_firestoreTags.value.isEmpty()) {
                    fetchTagsFromFirestore()
                }
                
                // Reset pagination nếu cần
                if (reset) {
                    currentPage = 0
                    _mangas.value = emptyList()
                }
                
                // Tải tất cả manga (sử dụng limit lớn)
                repository.fetchMangaListFromFirestore(reset, 1000).onSuccess { mangaList ->
                    _mangas.value = mangaList
                    Log.d("MangaViewModel", "Loaded ${mangaList.size} manga from Firestore")
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error fetching manga list from Firestore", exception)
                }
                
            } finally {
                _isLoadingFromFirestore.value = false
                _isRefreshing.value = false
                isLoading = false
            }
        }
    }

    fun fetchTrendingManga(reset: Boolean = false) {
        if (isLoading) return
        isLoading = true

        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                // Reset pagination if requested
                if (reset) {
                    trendingPage = 0
                    _mangas.value = emptyList()
                }

                // Đảm bảo tags đã được tải
                if (_firestoreTags.value.isEmpty()) {
                    fetchTagsFromFirestore()
                }

                // Tải tất cả manga trending (sử dụng limit lớn)
                repository.fetchTrendingMangaFromFirestore(1000).onSuccess { mangaList ->
                    _mangas.value = mangaList
                    Log.d("MangaViewModel", "Loaded ${mangaList.size} trending manga from Firestore")
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error fetching trending manga from Firestore", exception)
                }
            } finally {
                isLoading = false
                _isRefreshing.value = false
            }
        }
    }

    fun fetchRecommendedManga(reset: Boolean = false, includedTagIds: List<String>? = null) {
        if (isLoading) return
        isLoading = true

        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                // Reset pagination if requested or if new tags are provided
                val shouldReset = reset || (includedTagIds != null && includedTagIds != currentRecommendedTags)

                if (shouldReset) {
                    recommendedPage = 0
                    _mangas.value = emptyList()
                }

                // If no tags provided, try to get tags from reading history
                val tagsToUse = includedTagIds ?: currentRecommendedTags.takeIf { it.isNotEmpty() }

                // Save current tags for future pagination
                if (tagsToUse != currentRecommendedTags) {
                    currentRecommendedTags = tagsToUse ?: emptyList()
                }

                // Đảm bảo tags đã được tải
                if (_firestoreTags.value.isEmpty()) {
                    fetchTagsFromFirestore()
                }

                // Tải tất cả manga được đề xuất (sử dụng limit lớn)
                repository.fetchRecommendedMangaFromFirestore(tagsToUse, 1000).onSuccess { mangaList ->
                    _mangas.value = mangaList
                    Log.d("MangaViewModel", "Loaded ${mangaList.size} recommended manga from Firestore")
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error fetching recommended manga from Firestore", exception)
                }
            } finally {
                isLoading = false
                _isRefreshing.value = false
            }
        }
    }

    fun searchManga(title: String) {
        if (title.isBlank()) {
            fetchMangaList(reset = true)
            return
        }
        
        if (isLoading) return
        isLoading = true
        
        viewModelScope.launch {
            try {
                _isLoadingFromFirestore.value = true
                _isRefreshing.value = true
                
                // Đảm bảo tags đã được tải
                if (_firestoreTags.value.isEmpty()) {
                    fetchTagsFromFirestore()
                }
                
                repository.searchMangaFromFirestore(title).onSuccess { mangaList ->
                    _mangas.value = mangaList
                    Log.d("MangaViewModel", "Found ${mangaList.size} manga matching search term: $title")
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error searching manga", exception)
                }
                
            } finally {
                _isLoadingFromFirestore.value = false
                _isRefreshing.value = false
                isLoading = false
            }
        }
    }

    fun refreshMangaList() {
        _isRefreshing.value = true
        fetchMangaList(reset = true)
    }

    fun loadMangaDetails(mangaId: String) {
        viewModelScope.launch {
            try {
                Log.d("MangaViewModel", "Starting to load manga details for $mangaId")
                
                // Đảm bảo tags đã được tải
                if (_firestoreTags.value.isEmpty()) {
                    fetchTagsFromFirestore()
                }
                
                // Tăng viewCount khi xem chi tiết manga
                repository.incrementMangaViewCount(mangaId)
                
                repository.getMangaDetailsFromFirestore(mangaId).onSuccess { mangaData ->
                    // Log thông tin manga để debug
                    Log.d("MangaViewModel", "Loaded manga details: id=${mangaData.id}, author=${mangaData.attributes.author}")
                    
                    _mangaDetail.value = MangaDetailUiState(
                        title = mangaData.attributes.title["en"] ?: "",
                        titles = mangaData.attributes.title,
                        description = mangaData.attributes.description["en"] ?: "",
                        descriptions = mangaData.attributes.description,
                        author = mangaData.attributes.author ?: "",
                        status = mangaData.attributes.status ?: "",
                        genres = mangaData.attributes.tags.map { it.attributes.name["en"] ?: "" },
                        coverImageUrl = mangaData.displayCoverUrl
                    )
                    
                    // Log thông tin UI state để debug
                    Log.d("MangaViewModel", "Created UI state with author: ${_mangaDetail.value.author}")
                    
                    // Cập nhật danh sách ngôn ngữ có sẵn
                    val availableLanguages = mangaData.attributes.availableTranslatedLanguages
                    _availableLanguages.value = availableLanguages
                    
                    Log.d("MangaViewModel", "Available languages for manga $mangaId: $availableLanguages")
                    
                    // Nếu ngôn ngữ hiện tại không có trong danh sách, chọn ngôn ngữ đầu tiên
                    if (availableLanguages.isNotEmpty() && !availableLanguages.contains(_selectedLanguage.value)) {
                        _selectedLanguage.value = availableLanguages.first()
                        Log.d("MangaViewModel", "Changed selected language to: ${_selectedLanguage.value}")
                    }
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error loading manga details", exception)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Unexpected error in loadMangaDetails", e)
            }
        }
    }

    fun changeLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun loadChapters(mangaId: String, language: String) {
        viewModelScope.launch {
            try {
                Log.d("MangaViewModel", "Starting to load chapters for manga $mangaId with language $language")
                
                repository.getChaptersFromFirestore(mangaId, language).onSuccess { chaptersList ->
                    _chapters.value = chaptersList
                    Log.d("MangaViewModel", "Successfully loaded ${chaptersList.size} chapters for manga $mangaId")
                    
                    // Log chi tiết về 3 chapter đầu tiên (nếu có)
                    chaptersList.take(3).forEachIndexed { index, chapter ->
                        Log.d("MangaViewModel", "Chapter ${index + 1}: id=${chapter.id}, number=${chapter.attributes.chapter}, title=${chapter.attributes.title}")
                    }
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error loading chapters", exception)
                    _chapters.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Unexpected error loading chapters", e)
                _chapters.value = emptyList()
            }
        }
    }

    fun loadChapterContent(chapterId: String) {
        viewModelScope.launch {
            try {
                Log.d("MangaViewModel", "Starting to load content for chapter $chapterId")
                
                _chapterError.value = null
                _chapterImageUrls.value = emptyList()
                _totalPages.value = 0
                _currentPageReading.value = 1

                repository.getChapterContentFromFirestore(chapterId).onSuccess { (title, urls) ->
                    _chapterImageUrls.value = urls
                    _totalPages.value = urls.size
                    _currentPageReading.value = 1
                    _chapterTitle.value = title
                    
                    Log.d("MangaViewModel", "Successfully loaded chapter content: title=$title, ${urls.size} images")
                    
                    // Log một số URL hình ảnh đầu tiên (nếu có)
                    urls.take(2).forEachIndexed { index, url ->
                        Log.d("MangaViewModel", "Image ${index + 1}: $url")
                    }
                    
                    // Đánh dấu chapter đã đọc
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null) {
                        Log.d("MangaViewModel", "Marking chapter $chapterId as read")
                        
                        // Cập nhật danh sách chapter đã đọc
                        _readChapters.update { currentSet ->
                            currentSet + chapterId
                        }
                    }
                }.onFailure { exception ->
                    _chapterError.value = exception.message ?: "Failed to load chapter content"
                    Log.e("MangaViewModel", "Error loading chapter content: ${exception.message}", exception)
                }
            } catch (e: Exception) {
                _chapterError.value = "An unexpected error occurred"
                Log.e("MangaViewModel", "Unexpected error loading chapter content", e)
            }
        }
    }

    fun updateCurrentPage(page: Int) {
        if (page in 1.._totalPages.value) {
            _currentPageReading.value = page
        }
    }

    fun loadNextChapter(
        currentChapterId: String,
        mangaId: String,
        language: String,
        onChapterLoaded: (String?) -> Unit
    ) {
        if (_isLoadingNextChapter.value) return

        viewModelScope.launch {
            try {
                Log.d("MangaViewModel", "Starting to load next chapter after $currentChapterId in manga $mangaId")
                
                _isLoadingNextChapter.value = true
                _showNextChapterButton.value = false
                _chapterError.value = null

                repository.getNextChapterFromFirestore(mangaId, currentChapterId, language).onSuccess { nextChapter ->
                    if (nextChapter != null) {
                        Log.d("MangaViewModel", "Found next chapter: ${nextChapter.id}, number: ${nextChapter.attributes.chapter}")
                        
                        // Save reading progress for current chapter
                        saveReadingProgress(
                            mangaId = mangaId,
                            chapterId = currentChapterId,
                            language = language,
                            lastPageIndex = _totalPages.value
                        )

                        // Clear current chapter data
                        _chapterImageUrls.value = emptyList()
                        _totalPages.value = 0
                        _currentPageReading.value = 1  // Reset to page 1
                        _chapterTitle.value = ""
                        
                        // Notify UI to reload with next chapter
                        onChapterLoaded(nextChapter.id)
                    } else {
                        Log.d("MangaViewModel", "No next chapter available after $currentChapterId")
                        _chapterError.value = "No next chapter available"
                        onChapterLoaded(null)
                    }
                }.onFailure { exception ->
                    _chapterError.value = "Failed to load next chapter"
                    Log.e("MangaViewModel", "Error loading next chapter", exception)
                    onChapterLoaded(null)
                }
            } catch (e: Exception) {
                _chapterError.value = "Failed to load next chapter"
                Log.e("MangaViewModel", "Unexpected error loading next chapter", e)
                onChapterLoaded(null)
            } finally {
                _isLoadingNextChapter.value = false
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
            try {
                Log.d("MangaViewModel", "Getting last read chapter for manga $mangaId with language $language")
                
                repository.getLastReadChapter(userId, mangaId, language).onSuccess { result ->
                    if (result != null) {
                        val (chapterId, lastPageIndex) = result
                        _lastReadChapter.value = Pair(chapterId, lastPageIndex)
                        Log.d("MangaViewModel", "Found last read chapter: $chapterId, page: $lastPageIndex")
                    } else {
                        Log.d("MangaViewModel", "No last read chapter found for manga $mangaId")
                    }
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error getting last read chapter", exception)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Unexpected error getting last read chapter", e)
            }
        }
    }

    private val _readingProgress = MutableStateFlow<List<ReadingProgress>>(emptyList())
    val readingProgress: StateFlow<List<ReadingProgress>> = _readingProgress

    fun loadReadingProgress(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                // Nếu không cần refresh và đã có dữ liệu, trả về luôn
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

                    // Load manga details cho mỗi progress từ Firestore
                    latestProgressByManga.forEach { progressItem ->
                        if (!_mangaDetails.value.containsKey(progressItem.mangaId)) {
                            try {
                                Log.d("MangaViewModel", "Loading manga details for ${progressItem.mangaId}")
                                repository.getMangaDetailsFromFirestore(progressItem.mangaId).onSuccess { mangaData ->
                                    _mangaDetails.update { currentMap ->
                                        currentMap + (mangaData.id to mangaData)
                                    }
                                    Log.d("MangaViewModel", "Successfully loaded manga details for ${mangaData.id}")
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
                _isDeleting.value = true
                _deleteError.value = null
                
                val userId = firebaseAuth.currentUser?.uid ?: throw Exception("User not authenticated")
                
                repository.deleteReadingProgress(userId, mangaId, chapterId, language)
                
                // Reload reading progress
                loadReadingProgress(true)
            } catch (e: Exception) {
                _deleteError.value = e.message ?: "Failed to delete reading progress"
                Log.e("MangaViewModel", "Error deleting reading progress", e)
            } finally {
                _isDeleting.value = false
            }
        }
    }

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    fun deleteAllMangaReadingProgress() {
        viewModelScope.launch {
            try {
                _isDeleting.value = true
                _deleteError.value = null
                
                val userId = firebaseAuth.currentUser?.uid ?: throw Exception("User not authenticated")
                
                // Get all unique manga IDs from reading progress
                val mangaIds = _readingProgress.value
                    .groupBy { it.mangaId }
                    .keys
                    .toList()
                
                // Delete reading progress for each manga
                mangaIds.forEach { mangaId ->
                    repository.deleteAllMangaReadingProgress(userId, mangaId)
                }
                
                // Reload reading progress
                loadReadingProgress(true)
            } catch (e: Exception) {
                _deleteError.value = e.message ?: "Failed to delete reading history"
                Log.e("MangaViewModel", "Error deleting all reading progress", e)
            } finally {
                _isDeleting.value = false
            }
        }
    }

    private suspend fun loadChapterWithPagination(mangaId: String, language: String, targetChapterId: String) {
        try {
            Log.d("MangaViewModel", "Directly loading chapter $targetChapterId for manga $mangaId")
            
            // Try to load the specific chapter directly first
            val chapterDoc = FirebaseFirestore.getInstance()
                .collection("manga")
                .document(mangaId)
                .collection("chapters")
                .document(targetChapterId)
                .get()
                .await()
            
            if (chapterDoc.exists()) {
                // Convert the document to ChapterData
                val chapterNumber = chapterDoc.getString("chapter") ?: ""
                val title = chapterDoc.getString("title") ?: ""
                val translatedLanguage = chapterDoc.getString("language") ?: language
                val publishAt = chapterDoc.getString("publishAt") ?: ""
                
                val chapterData = ChapterData(
                    id = targetChapterId,
                    attributes = ChapterAttributes(
                        chapter = chapterNumber,
                        title = title,
                        translatedLanguage = translatedLanguage,
                        externalUrl = null,
                        publishAt = publishAt
                    )
                )
                
                // Add to chapter details
                _chapterDetails.update { currentMap ->
                    currentMap + (targetChapterId to chapterData)
                }
                
                Log.d("MangaViewModel", "Successfully loaded target chapter $targetChapterId directly")
                return
            } else {
                Log.d("MangaViewModel", "Chapter $targetChapterId not found directly, trying pagination")
            }
            
            // If direct loading fails, try pagination approach
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
                // If still not found, try to get it directly from Firestore
                Log.d("MangaViewModel", "Chapter $targetChapterId not found in any batch, trying direct Firestore query")
                
                try {
                    val chapterDoc = FirebaseFirestore.getInstance()
                        .collection("manga")
                        .document(mangaId)
                        .collection("chapters")
                        .document(targetChapterId)
                        .get()
                        .await()
                    
                    if (chapterDoc.exists()) {
                        // Convert the document to ChapterData
                        val chapterNumber = chapterDoc.getString("chapter") ?: ""
                        val title = chapterDoc.getString("title") ?: ""
                        val translatedLanguage = chapterDoc.getString("language") ?: language
                        val publishAt = chapterDoc.getString("publishAt") ?: ""
                        
                        val chapterData = ChapterData(
                            id = targetChapterId,
                            attributes = ChapterAttributes(
                                chapter = chapterNumber,
                                title = title,
                                translatedLanguage = translatedLanguage,
                                externalUrl = null,
                                publishAt = publishAt
                            )
                        )
                        
                        // Add to chapter details
                        _chapterDetails.update { currentMap ->
                            currentMap + (targetChapterId to chapterData)
                        }
                        
                        Log.d("MangaViewModel", "Successfully loaded target chapter $targetChapterId from Firestore")
                    } else {
                        Log.e("MangaViewModel", "Chapter $targetChapterId not found in Firestore")
                    }
                } catch (e: Exception) {
                    Log.e("MangaViewModel", "Error loading chapter $targetChapterId directly from Firestore", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MangaViewModel", "Error in loadChapterWithPagination for chapter $targetChapterId", e)
        }
    }

    fun formatHistoryDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown Date"

        // Generate current date
        val today = Calendar.getInstance()
        val input = Calendar.getInstance().apply { timeInMillis = timestamp }

        // Reset hour, minute, second, millisecond cho cả hai
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        input.set(Calendar.HOUR_OF_DAY, 0)
        input.set(Calendar.MINUTE, 0)
        input.set(Calendar.SECOND, 0)
        input.set(Calendar.MILLISECOND, 0)

        // Calculate difference in days
        val diffInDays = ((today.timeInMillis - input.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

        // Format date
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        return when (diffInDays) {
            0 -> "Today"
            1 -> "Yesterday"
            in 2..6 -> "$diffInDays days ago"
            else -> sdf.format(Date(timestamp))
        }
    }

    private val _tags = MutableStateFlow<List<TagWrapper>>(emptyList())
    val tags: StateFlow<List<TagWrapper>> = _tags

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags

    private val _tagFilterMode = MutableStateFlow("AND")
    val tagFilterMode: StateFlow<String> = _tagFilterMode

    var selectedTabIndex by mutableStateOf(0)
        private set

    fun updateSelectedTags(tagId: String, selected: Boolean) {
        _selectedTags.update { currentTags ->
            if (selected) {
                currentTags + tagId
            } else {
                currentTags - tagId
            }
        }
    }

    fun setTagFilterMode(mode: String) {
        if (mode in listOf("AND", "OR")) {
            _tagFilterMode.value = mode
        }
    }

    fun applyTagFilter() {
        if (_selectedTags.value.isEmpty()) {
            when (selectedTabIndex) {
                0 -> fetchMangaList(reset = true)
                1 -> fetchRecommendedManga(reset = true)
                2 -> fetchTrendingManga(reset = true)
            }
        } else {
            viewModelScope.launch {
                try {
                    _isLoadingFromFirestore.value = true
                    _isRefreshing.value = true
                    
                    // Đảm bảo tags đã được tải
                    if (_firestoreTags.value.isEmpty()) {
                        fetchTagsFromFirestore()
                    }
                    
                    repository.filterMangaByTags(
                        _selectedTags.value.toList(), 
                        _tagFilterMode.value
                    ).onSuccess { mangaList ->
                        _mangas.value = mangaList
                        Log.d("MangaViewModel", "Filtered to ${mangaList.size} manga based on tags")
                    }.onFailure { exception ->
                        Log.e("MangaViewModel", "Error filtering manga by tags", exception)
                    }
                    
                } finally {
                    _isLoadingFromFirestore.value = false
                    _isRefreshing.value = false
                    isLoading = false
                }
            }
        }
    }

    fun clearAllTags() {
        _selectedTags.value = emptyList()
    }

    // Hàm để lấy comments cho một manga
    fun loadComments(mangaId: String) {
        currentMangaId = mangaId
        viewModelScope.launch {
            _isLoadingComments.value = true
            _commentError.value = null

            try {
                repository.getComments(mangaId).onSuccess { commentList ->
                    Log.d("MangaViewModel", "Comments loaded successfully: ${commentList.size} comments")
                    _comments.value = commentList

                }.onFailure { exception ->
                    _commentError.value = exception.message ?: "Failed to load comments"
                    Log.e("MangaViewModel", "Error loading comments", exception)
                }
            } catch (e: Exception) {
                _commentError.value = e.message ?: "An unexpected error occurred"
                Log.e("MangaViewModel", "Exception loading comments", e)
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    // Hàm để thêm comment mới
    fun addComment(mangaId: String, content: String) {
        if (!isUserLoggedIn()) return
        
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        if (content.isBlank()) {
            _commentError.value = "Comment cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _commentActionInProgress.value = true
            _commentError.value = null
            
            try {
                // Lấy thông tin user từ AuthRepository
                authRepository.getUserInfo(userId).onSuccess { user ->
                    // Update userDetails immediately
                    _userDetails.update { currentMap ->
                        currentMap + (userId to user)
                    }
                    
                    repository.addComment(mangaId, userId, user.nickname, content).onSuccess {
                        // Reload comments to show the new one
                        loadComments(mangaId)
                    }.onFailure { exception ->
                        _commentError.value = exception.message ?: "Failed to add comment"
                        Log.e("MangaViewModel", "Error adding comment", exception)
                    }
                }.onFailure { exception ->
                    _commentError.value = exception.message ?: "Failed to get user info"
                    Log.e("MangaViewModel", "Error getting user info", exception)
                }
            } catch (e: Exception) {
                _commentError.value = e.message ?: "An unexpected error occurred"
                Log.e("MangaViewModel", "Exception adding comment", e)
            } finally {
                _commentActionInProgress.value = false
            }
        }
    }

    // Hàm để xóa comment
    fun deleteComment(commentId: String) {
        if (!isUserLoggedIn()) return
        
        val mangaId = currentMangaId ?: return
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _commentActionInProgress.value = true
            _commentError.value = null
            
            try {
                // Kiểm tra xem comment có tồn tại và thuộc về user hiện tại không
                val comment = _comments.value.find { it.id == commentId }
                if (comment == null) {
                    _commentError.value = "Comment not found"
                    return@launch
                }
                
                if (comment.userId != userId) {
                    _commentError.value = "You can only delete your own comments"
                    return@launch
                }
                
                repository.deleteComment(mangaId, commentId).onSuccess {
                    // Reload comments after deletion
                    loadComments(mangaId)
                }.onFailure { exception ->
                    _commentError.value = exception.message ?: "Failed to delete comment"
                    Log.e("MangaViewModel", "Error deleting comment", exception)
                }
            } catch (e: Exception) {
                _commentError.value = e.message ?: "An unexpected error occurred"
                Log.e("MangaViewModel", "Exception deleting comment", e)
            } finally {
                _commentActionInProgress.value = false
            }
        }
    }

    // Hàm để xóa lỗi comment
    fun clearCommentError() {
        _commentError.value = null
    }

    // Kiểm tra người dùng đã đăng nhập chưa
    private fun isUserLoggedIn(): Boolean {
        val isLoggedIn = firebaseAuth.currentUser != null
        if (!isLoggedIn) {
            _commentError.value = "You must be logged in to perform this action"
        }
        return isLoggedIn
    }

    // Add a function to load read chapters for a manga
    fun loadReadChapters(mangaId: String, language: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                Log.d("MangaViewModel", "Loading read chapters for manga $mangaId with language $language")
                
                repository.getReadChapters(userId, mangaId, language).onSuccess { readChapterIds ->
                    _readChapters.value = readChapterIds.toSet()
                    Log.d("MangaViewModel", "Successfully loaded ${readChapterIds.size} read chapters for manga $mangaId")
                    
                    // Log một số chapter đã đọc (nếu có)
                    readChapterIds.take(3).forEach { chapterId ->
                        Log.d("MangaViewModel", "Read chapter: $chapterId")
                    }
                }.onFailure { exception ->
                    Log.e("MangaViewModel", "Error loading read chapters", exception)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Unexpected error loading read chapters", e)
            }
        }
    }

    // Add new functions for chapter reading
    fun toggleControls() {
        _showControls.value = !_showControls.value
    }

    fun hideControls() {
        _showControls.value = false
    }

    fun updateNextChapterButtonVisibility(isNearEnd: Boolean) {
        _showNextChapterButton.value = isNearEnd && !_isLoadingNextChapter.value
    }

    fun clearChapterError() {
        _chapterError.value = null
    }

    fun fetchTagsFromFirestore() {
        viewModelScope.launch {
            try {
                repository.getTagsFromFirestore().onSuccess { tagList ->
                    _firestoreTags.value = tagList
                    Log.d("MangaViewModel", "Fetched ${tagList.size} tags from Firestore")
                    
                    // Log chi tiết về tags
                    tagList.groupBy { it.group }.forEach { (group, tagsInGroup) ->
                        Log.d("MangaViewModel", "Group: $group - ${tagsInGroup.size} tags")
                        tagsInGroup.take(5).forEach { tag ->
                            Log.d("MangaViewModel", "  - ${tag.id}: ${tag.name}")
                        }
                    }
                }.onFailure { e ->
                    Log.e("MangaViewModel", "Error fetching tags from Firestore", e)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Unexpected error in fetchTagsFromFirestore", e)
            }
        }
    }
}











