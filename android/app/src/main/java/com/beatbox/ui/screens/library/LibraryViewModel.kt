package com.beatbox.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.data.dto.MusicDto
import com.beatbox.data.dto.PlaylistDto
import com.beatbox.data.repository.ApiResult
import com.beatbox.data.repository.MusicRepository
import com.beatbox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoadingFavorites: Boolean = false,
    val isLoadingRecent: Boolean = false,
    val isLoadingPlaylists: Boolean = false,
    val isLoadingUploads: Boolean = false,
    val favorites: List<MusicDto> = emptyList(),
    val recentlyPlayed: List<MusicDto> = emptyList(),
    val playlists: List<PlaylistDto> = emptyList(),
    val myUploads: List<MusicDto> = emptyList(),
    val favoritesError: String? = null,
    val recentError: String? = null,
    val playlistsError: String? = null,
    val uploadsError: String? = null,
) {
    fun isLoading(tab: LibraryTab): Boolean = when (tab) {
        LibraryTab.Favorites -> isLoadingFavorites
        LibraryTab.Recent -> isLoadingRecent
        LibraryTab.Playlists -> isLoadingPlaylists
        LibraryTab.Uploads -> isLoadingUploads
    }

    fun error(tab: LibraryTab): String? = when (tab) {
        LibraryTab.Favorites -> favoritesError
        LibraryTab.Recent -> recentError
        LibraryTab.Playlists -> playlistsError
        LibraryTab.Uploads -> uploadsError
    }
}

enum class LibraryTab(val title: String) {
    Favorites("Favorites"),
    Recent("Recent"),
    Playlists("Playlists"),
    Uploads("Uploads"),
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LibraryUiState(
            isLoadingFavorites = true,
            isLoadingRecent = true,
            isLoadingPlaylists = true,
            isLoadingUploads = true,
        ),
    )
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        loadFavorites()
        loadRecentlyPlayed()
        loadPlaylists()
        loadMyUploads()
    }

    fun loadFavorites() {
        _state.value = _state.value.copy(isLoadingFavorites = true, favoritesError = null)
        viewModelScope.launch {
            when (val result = socialRepository.getFavoriteMusic()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    isLoadingFavorites = false,
                    favorites = result.data,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoadingFavorites = false,
                    favoritesError = result.message,
                )
                is ApiResult.Loading -> Unit
            }
        }
    }

    fun loadRecentlyPlayed() {
        _state.value = _state.value.copy(isLoadingRecent = true, recentError = null)
        viewModelScope.launch {
            when (val result = musicRepository.getRecentlyPlayed(50)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    isLoadingRecent = false,
                    recentlyPlayed = result.data,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoadingRecent = false,
                    recentError = result.message,
                )
                is ApiResult.Loading -> Unit
            }
        }
    }

    fun loadPlaylists() {
        _state.value = _state.value.copy(isLoadingPlaylists = true, playlistsError = null)
        viewModelScope.launch {
            when (val result = socialRepository.getMyPlaylists()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    isLoadingPlaylists = false,
                    playlists = result.data,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoadingPlaylists = false,
                    playlistsError = result.message,
                )
                is ApiResult.Loading -> Unit
            }
        }
    }

    fun loadMyUploads() {
        _state.value = _state.value.copy(isLoadingUploads = true, uploadsError = null)
        viewModelScope.launch {
            when (val result = musicRepository.getMyUploads()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    isLoadingUploads = false,
                    myUploads = result.data,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoadingUploads = false,
                    uploadsError = result.message,
                )
                is ApiResult.Loading -> Unit
            }
        }
    }
}
