package com.android.dacs3.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.repository.FavouriteRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@HiltViewModel
class FavouriteViewModel @Inject constructor(
    private val repository: FavouriteRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _favourites = MutableLiveData<List<String>>()
    val favourites: LiveData<List<String>> = _favourites

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isFavourite = MutableLiveData<Boolean>()
    val isFavourite: LiveData<Boolean> = _isFavourite

    private val _isDeleting = MutableLiveData<Boolean>(false)
    val isDeleting: LiveData<Boolean> = _isDeleting

    // Thêm các state mới
    private val _isVip = MutableLiveData<Boolean>(false)
    val isVip: LiveData<Boolean> = _isVip

    private val _maxFavouritesReached = MutableLiveData<Boolean>(false)
    val maxFavouritesReached: LiveData<Boolean> = _maxFavouritesReached

    // Số lượng tối đa truyện yêu thích cho người dùng không phải VIP
    private val MAX_FREE_FAVOURITES = 3

    init {
        // Kiểm tra trạng thái VIP khi khởi tạo
        checkVipStatus()
    }

    private fun checkVipStatus() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                try {
                    // Sử dụng Firestore để kiểm tra trạng thái VIP
                    val db = FirebaseFirestore.getInstance()
                    val userDoc = db.collection("users").document(userId).get().await()
                    
                    val isVip = userDoc.getBoolean("isVip") ?: false
                    val vipExpireDate = when (val expireDate = userDoc.get("vipExpireDate")) {
                        is com.google.firebase.Timestamp -> expireDate.toDate().time
                        is Long -> expireDate
                        else -> 0L
                    }
                    
                    // Kiểm tra xem VIP có còn hiệu lực không
                    val isVipValid = isVip && vipExpireDate > System.currentTimeMillis()
                    _isVip.value = isVipValid
                    
                    Log.d("FavouriteViewModel", "User VIP status: $isVipValid")
                } catch (e: Exception) {
                    Log.e("FavouriteViewModel", "Error checking VIP status", e)
                    _isVip.value = false
                }
            }
        } else {
            _isVip.value = false
        }
    }

    fun loadFavourites() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _loading.value = true
                val result = repository.getFavourites(userId)
                if (result.isSuccess) {
                    val favList = result.getOrNull() ?: emptyList()
                    _favourites.value = favList
                    Log.d("FavouriteViewModel", "Favourites loaded: $favList")
                    _error.value = null
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
                _loading.value = false
            }
        } else {
            _favourites.value = emptyList()
        }
    }

    private val _mangaDetails = MutableLiveData<List<MangaData>>()
    val mangaDetails: LiveData<List<MangaData>> = _mangaDetails


    fun loadFavouriteDetails() {
        loadFavourites()
        val ids = _favourites.value ?: return
        viewModelScope.launch {
            _loading.value = true
            val mangaList = ids.map { id ->
                async {
                    repository.getMangaById(id).getOrNull()
                }
            }.awaitAll().filterNotNull() // Filter out null results
            _mangaDetails.value = mangaList
            _loading.value = false
        }
    }


    fun addToFavourite(mangaId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _loading.value = true
                
                // Kiểm tra số lượng truyện yêu thích hiện tại
                if (_isVip.value != true) {
                    val currentFavourites = _favourites.value ?: emptyList()
                    if (currentFavourites.size >= MAX_FREE_FAVOURITES && !currentFavourites.contains(mangaId)) {
                        // Hiển thị thông báo đã đạt giới hạn
                        _maxFavouritesReached.value = true
                        _loading.value = false
                        return@launch
                    }
                }
                
                val result = repository.addFavourite(userId, mangaId)
                if (result.isSuccess) {
                    // Update favorites list
                    loadFavourites()
                    // Immediately update UI state
                    _isFavourite.value = true

                    // Add to local favorites list as well if not already there
                    val currentList = _favourites.value ?: emptyList()
                    if (!currentList.contains(mangaId)) {
                        _favourites.value = currentList + mangaId
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
                _loading.value = false
            }
        }
    }

    fun removeFavourite(mangaId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _loading.value = true
                val result = repository.removeFavourite(userId, mangaId)
                if (result.isSuccess) {
                    // Update favorites list
                    loadFavourites()
                    // Immediately update UI state
                    _isFavourite.value = false

                    // Remove from local favorites list as well
                    val currentList = _favourites.value ?: emptyList()
                    if (currentList.contains(mangaId)) {
                        _favourites.value = currentList.filter { it != mangaId }
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
                _loading.value = false
            }
        }
    }

    fun checkIfFavourite(mangaId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _loading.value = true
                val result = repository.getFavourites(userId)
                if (result.isSuccess) {
                    val favList = result.getOrNull() ?: emptyList()
                    _isFavourite.value = favList.contains(mangaId)
                } else {
                    _isFavourite.value = false
                    _error.value = result.exceptionOrNull()?.message
                }
                _loading.value = false
            }
        } else {
            _isFavourite.value = false
        }
    }

    fun deleteAllFavourites() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _isDeleting.value = true
                _loading.value = true
                val result = repository.deleteAllFavourites(userId)
                if (result.isSuccess) {
                    // Clear local lists
                    _favourites.value = emptyList()
                    _mangaDetails.value = emptyList()
                    _error.value = null
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
                _loading.value = false
                _isDeleting.value = false
            }
        }
    }

    // Clear error message
    fun clearError() {
        _error.value = null
    }

    // Reset thông báo giới hạn
    fun resetMaxFavouritesReached() {
        _maxFavouritesReached.value = false
    }

    // Refresh trạng thái VIP
    fun refreshVipStatus() {
        checkVipStatus()
    }
}
