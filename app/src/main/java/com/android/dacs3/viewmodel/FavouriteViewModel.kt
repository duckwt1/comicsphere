package com.android.dacs3.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.repository.FavouriteRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    fun addToFavourite(mangaId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _loading.value = true
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

    // Clear error message
    fun clearError() {
        _error.value = null
    }
}