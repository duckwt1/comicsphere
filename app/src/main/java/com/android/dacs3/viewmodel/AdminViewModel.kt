package com.android.dacs3.viewmodel

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

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
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

    private val firestore = FirebaseFirestore.getInstance()

    // Add this property to store chapters for the selected manga
    private val _mangaChapters = MutableStateFlow<List<ChapterData>>(emptyList())
    val mangaChapters: StateFlow<List<ChapterData>> = _mangaChapters

    init {
        loadAllUsers()
        fetchTagsFromFirestore()
        loadMangaList()
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
            try {
                Log.d("AdminViewModel", "Fetching tags from Firestore")
                
                val querySnapshot = firestore.collection("tags")
                    .get()
                    .await()
                
                Log.d("AdminViewModel", "Fetched ${querySnapshot.documents.size} tag documents")
                
                val tags = querySnapshot.documents.mapNotNull { document ->
                    try {
                        val id = document.id
                        val name = document.getString("name") ?: return@mapNotNull null
                        val group = document.getString("group") ?: "unknown"
                        
                        Tag(id = id, name = name, group = group)
                    } catch (e: Exception) {
                        Log.e("AdminViewModel", "Error parsing tag document: ${document.id}", e)
                        null
                    }
                }
                
                _firestoreTags.value = tags
                
                // Chuyển đổi từ Tag sang TagWrapper
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
                
                Log.d("AdminViewModel", "Successfully parsed ${tags.size} tags")
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error fetching tags from Firestore", e)
                _errorMessage.value = "Failed to load tags: ${e.message}"
            }
        }
    }

    fun getTagById(tagId: String): TagWrapper? {
        return _tags.value.find { it.id == tagId }
    }

    fun getTagsByIds(tagIds: List<String>): List<TagWrapper> {
        return _tags.value.filter { tagIds.contains(it.id) }
    }

    fun getTagNameById(tagId: String): String {
        val tag = _tags.value.find { it.id == tagId }
        return tag?.attributes?.name?.get("en") ?: "Unknown Tag"
    }

    fun loadMangaList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Đảm bảo tags đã được tải
                if (_firestoreTags.value.isEmpty()) {
                    fetchTagsFromFirestore()
                    kotlinx.coroutines.delay(500) // Đợi tags được tải
                }
                
                // Lấy tất cả dữ liệu từ Firestore
                val mangaCollection = firestore.collection("manga")
                val query = mangaCollection.orderBy("lastUpdated", Query.Direction.DESCENDING)
                
                val querySnapshot = query.get().await()
                
                Log.d("AdminViewModel", "Fetched ${querySnapshot.documents.size} manga documents from Firestore")
                
                // Tạo map từ tagId đến Tag object để dễ dàng tra cứu
                val tagMap = _firestoreTags.value.associateBy { it.id }
                
                // Chuyển đổi dữ liệu Firestore thành MangaData
                val mangaList = querySnapshot.documents.mapNotNull { document ->
                    try {
                        val id = document.id
                        
                        // Xử lý ảnh bìa - ưu tiên coverImageUrl
                        val coverImageUrl = document.getString("coverImageUrl") ?: ""
                        val coverUrl = document.getString("coverUrl") ?: ""
                        
                        // Xác định URL cuối cùng theo thứ tự ưu tiên
                        val finalCoverUrl = when {
                            // 1. Ưu tiên coverImageUrl nếu có
                            coverImageUrl.isNotEmpty() -> coverImageUrl
                            // 2. Thử coverUrl nếu có
                            coverUrl.isNotEmpty() && coverUrl.startsWith("http") -> coverUrl
                            // 3. Nếu không có cả hai, sử dụng placeholder
                            else -> "https://via.placeholder.com/512x768?text=No+Cover"
                        }
                        
                        // Lấy thông tin cơ bản
                        val title = document.getString("title") ?: "Unknown"
                        val description = document.getString("description") ?: ""
                        val status = document.getString("status") ?: "ongoing"
                        
                        // Xử lý tags
                        val tagIds = document.get("tagIds") as? List<String> ?: emptyList()
                        
                        // Chuyển đổi tagIds thành đối tượng TagWrapper
                        val tags = tagIds.mapNotNull { tagId ->
                            tagMap[tagId]?.let { tag ->
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
                        }
                        
                        // Tạo đối tượng MangaData với tags
                        MangaData(
                            id = id,
                            attributes = MangaAttributes(
                                title = mapOf("en" to title),
                                description = mapOf("en" to description),
                                status = status,
                                availableTranslatedLanguages = listOf("en"),
                                altTitles = emptyList(),
                                tags = tags
                            ),
                            relationships = listOf(
                                Relationship(
                                    id = id,
                                    type = "cover_art",
                                    attributes = RelationshipAttributes(
                                        fileName = finalCoverUrl
                                    )
                                )
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("AdminViewModel", "Error parsing manga document: ${document.id}", e)
                        null
                    }
                }
                
                // Cập nhật danh sách manga
                _mangas.value = mangaList
                
                Log.d("AdminViewModel", "Loaded ${mangaList.size} manga from Firestore")
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading manga list", e)
                _errorMessage.value = "Failed to load manga list: ${e.message}"
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
        tags: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mangaId = UUID.randomUUID().toString()
                
                val mangaData = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "coverUrl" to coverUrl,
                    "status" to status,
                    "tagIds" to tags,
                    "lastUpdated" to System.currentTimeMillis()
                )
                
                firestore.collection("manga").document(mangaId)
                    .set(mangaData)
                    .await()
                
                Log.d("AdminViewModel", "Added new manga: $title")
                
                // Reload manga list
                loadMangaList()
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error adding manga", e)
                _errorMessage.value = "Failed to add manga: ${e.message}"
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
        tags: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mangaData = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "coverUrl" to coverUrl,
                    "status" to status,
                    "tagIds" to tags,
                    "lastUpdated" to System.currentTimeMillis()
                )
                
                firestore.collection("manga").document(mangaId)
                    .update(mangaData as Map<String, Any>)
                    .await()
                
                Log.d("AdminViewModel", "Updated manga: $title")
                
                // Reload manga list
                loadMangaList()
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error updating manga", e)
                _errorMessage.value = "Failed to update manga: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteManga(mangaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Delete manga document
                firestore.collection("manga").document(mangaId)
                    .delete()
                    .await()
                
                // Delete all chapters associated with this manga
                val chaptersQuery = firestore.collection("manga").document(mangaId)
                    .collection("chapters").get().await()
                
                for (chapterDoc in chaptersQuery.documents) {
                    firestore.collection("manga").document(mangaId)
                        .collection("chapters").document(chapterDoc.id)
                        .delete()
                        .await()
                }
                
                Log.d("AdminViewModel", "Deleted manga: $mangaId")
                
                // Reload manga list
                loadMangaList()
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error deleting manga", e)
                _errorMessage.value = "Failed to delete manga: ${e.message}"
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
}


