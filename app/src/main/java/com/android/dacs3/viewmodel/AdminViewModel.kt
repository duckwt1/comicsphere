package com.android.dacs3.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.model.ChapterAttributes
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.User
import com.android.dacs3.data.model.MangaAttributes
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.Relationship
import com.android.dacs3.data.model.RelationshipAttributes
import com.android.dacs3.data.model.Tag
import com.android.dacs3.data.model.TagAttributes
import com.android.dacs3.data.model.TagWrapper
import com.android.dacs3.data.repository.AdminRepository
import com.android.dacs3.data.repository.CloudinaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    private val _tags = MutableStateFlow<List<TagWrapper>>(emptyList())
    val tags: StateFlow<List<TagWrapper>> = _tags.asStateFlow()

    private val _firestoreTags = MutableStateFlow<List<Tag>>(emptyList())
    val firestoreTags: StateFlow<List<Tag>> = _firestoreTags.asStateFlow()

    private val _mangas = MutableStateFlow<List<MangaData>>(emptyList())
    val mangas: StateFlow<List<MangaData>> = _mangas.asStateFlow()

    // Add this property to store chapters for the selected manga
    private val _mangaChapters = MutableStateFlow<List<ChapterData>>(emptyList())
    val mangaChapters: StateFlow<List<ChapterData>> = _mangaChapters

    // Add new state for image uploads
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()
    
    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()

    init {
        loadAllUsers()
        fetchTagsFromFirestore()
        loadMangaList() // Make sure this is called in init
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.getAllUsers()
                    .onSuccess { userList ->
                        _users.value = userList
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to load users"
                        Log.e("AdminViewModel", "Error loading users", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception loading users", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun getFilteredUsers(): List<User> {
        val query = searchQuery.trim().lowercase()
        return if (query.isEmpty()) {
            _users.value
        } else {
            _users.value.filter {
                it.email.lowercase().contains(query) ||
                it.fullname.lowercase().contains(query) ||
                it.nickname.lowercase().contains(query)
            }
        }
    }

    fun toggleVipStatus(user: User, months: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val calendar = Calendar.getInstance()
                
                // Nếu đang là VIP, hủy VIP
                if (user.isVip) {
                    adminRepository.updateUserVipStatus(user.uid, false, 0)
                        .onSuccess {
                            loadAllUsers() // Tải lại danh sách người dùng
                        }
                        .onFailure { e ->
                            _errorMessage.value = e.message ?: "Failed to update VIP status"
                        }
                } else {
                    // Nếu chưa là VIP, thêm thời hạn VIP
                    calendar.add(Calendar.MONTH, months)
                    val expireDate = calendar.timeInMillis
                    
                    adminRepository.updateUserVipStatus(user.uid, true, expireDate)
                        .onSuccess {
                            loadAllUsers() // Tải lại danh sách người dùng
                        }
                        .onFailure { e ->
                            _errorMessage.value = e.message ?: "Failed to update VIP status"
                        }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleAdminStatus(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.updateUserAdminStatus(user.uid, !user.isAdmin)
                    .onSuccess {
                        loadAllUsers() // Tải lại danh sách người dùng
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to update admin status"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.deleteUser(user.uid)
                    .onSuccess {
                        loadAllUsers() // Tải lại danh sách người dùng
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to delete user"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserInfo(user: User, fullname: String, nickname: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.updateUserInfo(user.uid, fullname, nickname)
                    .onSuccess {
                        loadAllUsers() // Tải lại danh sách người dùng
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to update user info"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchTagsFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                adminRepository.getAllTags()
                    .onSuccess { tags ->
                        _firestoreTags.value = tags
                        
                        // Convert to TagWrapper
                        val tagWrappers = tags.map { tag ->
                            TagWrapper(
                                id = tag.id,
                                type = "tag",
                                attributes = TagAttributes(
                                    name = mapOf("en" to tag.name),
                                    group = tag.group,
                                    description = emptyMap()
                                )
                            )
                        }
                        
                        _tags.value = tagWrappers
                        Log.d("AdminViewModel", "Loaded ${tags.size} tags from repository")
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to load tags"
                        Log.e("AdminViewModel", "Error loading tags", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception loading tags", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMangaList() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                Log.d("AdminViewModel", "Starting to load manga list")
                adminRepository.getAllMangas()
                    .onSuccess { mangaList ->
                        _mangas.value = mangaList
                        Log.d("AdminViewModel", "Successfully loaded ${mangaList.size} manga from repository")
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to load manga list"
                        Log.e("AdminViewModel", "Error loading manga list", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception loading manga list", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getFilteredMangas(): List<MangaData> {
        return if (searchQuery.isEmpty()) {
            _mangas.value
        } else {
            _mangas.value.filter { manga ->
                val title = manga.attributes.title["en"] ?: ""
                title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun addManga(
        title: String,
        description: String,
        coverUrl: String,
        status: String,
        author: String,
        tagIds: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                adminRepository.addManga(title, description, coverUrl, status, author, tagIds)
                    .onSuccess { mangaId ->
                        Log.d("AdminViewModel", "Added manga with ID: $mangaId")
                        loadMangaList() // Reload manga list
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to add manga"
                        Log.e("AdminViewModel", "Error adding manga", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception adding manga", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateManga(
        mangaId: String,
        title: String,
        description: String,
        coverUrl: String,
        status: String,
        author: String,
        tagIds: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                adminRepository.updateManga(mangaId, title, description, coverUrl, status, author, tagIds)
                    .onSuccess {
                        Log.d("AdminViewModel", "Updated manga with cover url: $coverUrl")
                        loadMangaList() // Reload manga list
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to update manga"
                        Log.e("AdminViewModel", "Error updating manga", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception updating manga", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteManga(mangaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                adminRepository.deleteManga(mangaId)
                    .onSuccess {
                        Log.d("AdminViewModel", "Deleted manga with ID: $mangaId")
                        loadMangaList() // Reload manga list
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to delete manga"
                        Log.e("AdminViewModel", "Error deleting manga", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception deleting manga", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadChaptersForManga(mangaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val chaptersCollection = firestore.collection("manga")
                    .document(mangaId)
                    .collection("chapters")
                    .orderBy("chapter", Query.Direction.ASCENDING)
                    .get()
                    .await()
                
                val chaptersList = chaptersCollection.documents.mapNotNull { doc ->
                    try {
                        val chapterId = doc.id
                        val chapterNumber = doc.getString("chapter") ?: ""
                        val title = doc.getString("title") ?: ""
                        val language = doc.getString("language") ?: "en"
                        val pages = doc.getLong("pages")?.toInt() ?: 0
                        
                        ChapterData(
                            id = chapterId,
                            attributes = ChapterAttributes(
                                chapter = chapterNumber,
                                title = title,
                                translatedLanguage = language,
                                externalUrl = null,
                                publishAt = doc.getString("publishAt") ?: ""
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("AdminViewModel", "Error parsing chapter", e)
                        null
                    }
                }
                
                _mangaChapters.value = chaptersList
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading chapters", e)
                _errorMessage.value = "Failed to load chapters: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addChapter(
        mangaId: String,
        chapterNumber: String,
        title: String,
        language: String,
        imageUrls: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val chapterId = UUID.randomUUID().toString()
                
                // Create chapter document without imageUrls
                val chapterData = hashMapOf(
                    "chapter" to chapterNumber,
                    "title" to title,
                    "language" to language,
                    "pages" to imageUrls.size,
                    "publishAt" to System.currentTimeMillis().toString()
                )
                
                // First, create the chapter document
                firestore.collection("manga").document(mangaId)
                    .collection("chapters").document(chapterId)
                    .set(chapterData)
                    .await()
                
                // Then create a subcollection "images" with document "imageList"
                val imageListData = hashMapOf(
                    "count" to imageUrls.size,
                    "lastUpdated" to System.currentTimeMillis(),
                    "urls" to imageUrls
                )
                
                firestore.collection("manga").document(mangaId)
                    .collection("chapters").document(chapterId)
                    .collection("images").document("imageList")
                    .set(imageListData)
                    .await()
                
                Log.d("AdminViewModel", "Added new chapter: $title to manga $mangaId with ${imageUrls.size} images")
                
                // Reload chapter list
                loadChaptersForManga(mangaId)
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error adding chapter", e)
                _errorMessage.value = "Failed to add chapter: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteChapter(mangaId: String, chapterId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First delete the images subcollection
                val imagesCollection = firestore.collection("manga").document(mangaId)
                    .collection("chapters").document(chapterId)
                    .collection("images")
                
                // Get all documents in the images subcollection
                val imagesDocs = imagesCollection.get().await()
                
                // Delete each document in the subcollection
                for (doc in imagesDocs.documents) {
                    imagesCollection.document(doc.id).delete().await()
                }
                
                // Then delete the chapter document
                firestore.collection("manga").document(mangaId)
                    .collection("chapters").document(chapterId)
                    .delete()
                    .await()
                
                Log.d("AdminViewModel", "Deleted chapter: $chapterId from manga $mangaId")
                
                // Reload chapter list
                loadChaptersForManga(mangaId)
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error deleting chapter", e)
                _errorMessage.value = "Failed to delete chapter: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // New methods for image upload
    fun uploadMangaCover(imageUri: Uri): Flow<Result<String>> = flow {
        _isUploading.value = true
        _uploadProgress.value = 0f
        _uploadedImageUrl.value = null
        
        try {
            Log.d("AdminViewModel", "Starting manga cover upload")
            emit(Result.Loading(0f))
            
            // Use CloudinaryRepository to upload the image
            val result = cloudinaryRepository.uploadCoverImage(imageUri)
            
            result.onSuccess { url ->
                Log.d("AdminViewModel", "Cover upload successful: $url")
                _uploadedImageUrl.value = url
                emit(Result.Success(url))
            }.onFailure { e ->
                Log.e("AdminViewModel", "Cover upload failed", e)
                emit(Result.Failure(e.message ?: "Failed to upload cover image"))
            }
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Exception during cover upload", e)
            emit(Result.Failure(e.message ?: "An unexpected error occurred"))
        } finally {
            _isUploading.value = false
        }
    }
    
    fun uploadChapterImage(imageUri: Uri): Flow<Result<String>> = flow {
        _isUploading.value = true
        _uploadProgress.value = 0f
        
        try {
            Log.d("AdminViewModel", "Starting chapter image upload")
            emit(Result.Loading(0f))
            
            // Use CloudinaryRepository to upload the image
            // For chapter images, we can use the regular uploadImage method
            val result = cloudinaryRepository.uploadImage(imageUri)
            
            result.onSuccess { url ->
                Log.d("AdminViewModel", "Chapter image upload successful: $url")
                _uploadedImageUrl.value = url
                emit(Result.Success(url))
            }.onFailure { e ->
                Log.e("AdminViewModel", "Chapter image upload failed", e)
                emit(Result.Failure(e.message ?: "Failed to upload chapter image"))
            }
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Exception during chapter image upload", e)
            emit(Result.Failure(e.message ?: "An unexpected error occurred"))
        } finally {
            _isUploading.value = false
        }
    }
    
    // Helper sealed class for upload results
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Failure(val message: String) : Result<Nothing>()
        data class Loading(val progress: Float) : Result<Nothing>()
    }

    // Add this method to reset upload state
    fun resetUploadState() {
        _isUploading.value = false
        _uploadProgress.value = 0f
        _uploadedImageUrl.value = null
    }

    fun createUser(
        email: String,
        password: String,
        fullname: String,
        nickname: String,
        isVip: Boolean = false,
        isAdmin: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                adminRepository.createUser(email, password, fullname, nickname, isVip, isAdmin)
                    .onSuccess { userId ->
                        Log.d("AdminViewModel", "Created user with ID: $userId")
                        loadAllUsers() // Reload user list
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to create user"
                        Log.e("AdminViewModel", "Error creating user", e)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e("AdminViewModel", "Exception creating user", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}



