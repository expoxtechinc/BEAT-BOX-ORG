package com.beatbox.ui.screens.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.data.dto.MusicDto
import com.beatbox.data.repository.ApiResult
import com.beatbox.data.repository.MusicRepository
import com.beatbox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val uploadedMusic: MusicDto? = null,
    val audioFileName: String? = null,
    val artworkFileName: String? = null,
    val audioUri: Uri? = null,
    val artworkUri: Uri? = null,
    val title: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val genre: String = "",
    val description: String = "",
    val categoryId: String? = null,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UploadUiState())
    val state: StateFlow<UploadUiState> = _state.asStateFlow()

    // CRITICAL: Music upload is FREE. This method does NOT call any premium endpoint.
    // It does NOT consume premium free uses.
    // It does NOT trigger any subscription paywall.
    fun uploadMusic(
        audioFile: java.io.File,
        artworkFile: java.io.File?,
        title: String,
        artistName: String,
        albumName: String?,
        genre: String?,
        description: String?,
        categoryId: String?,
    ) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            // Upload music - this is FREE and does NOT consume premium uses
            val result = musicRepository.uploadMusic(
                audioFile = audioFile,
                title = title,
                artistName = artistName,
                albumName = albumName,
                genre = genre,
                description = description,
                categoryId = categoryId,
                artworkFile = artworkFile,
            )

            when (result) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        success = true,
                        uploadedMusic = result.data,
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message,
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun setAudioFile(uri: Uri, fileName: String) {
        _state.value = _state.value.copy(audioUri = uri, audioFileName = fileName)
    }

    fun setArtworkFile(uri: Uri, fileName: String) {
        _state.value = _state.value.copy(artworkUri = uri, artworkFileName = fileName)
    }

    fun updateField(field: String, value: String) {
        _state.value = _state.value.let { state ->
            when (field) {
                "title" -> state.copy(title = value)
                "artistName" -> state.copy(artistName = value)
                "albumName" -> state.copy(albumName = value)
                "genre" -> state.copy(genre = value)
                "description" -> state.copy(description = value)
                else -> state
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun reset() {
        _state.value = UploadUiState()
    }

    fun isValid(): Boolean {
        val s = _state.value
        return s.audioUri != null && s.title.isNotBlank() && s.artistName.isNotBlank()
    }
}
