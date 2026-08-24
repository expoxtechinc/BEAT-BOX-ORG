package com.beatbox.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.data.dto.CategoryDto
import com.beatbox.data.dto.MusicDto
import com.beatbox.data.dto.SearchUserDto
import com.beatbox.data.repository.ApiResult
import com.beatbox.data.repository.MusicRepository
import com.beatbox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val trending: List<MusicDto> = emptyList(),
    val newReleases: List<MusicDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val popularArtists: List<SearchUserDto> = emptyList(),
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState(isLoading = true))
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val trendingResult = musicRepository.getTrending(20)
            val newReleasesResult = musicRepository.getNewReleases(20)
            val categoriesResult = socialRepository.getCategories()
            val artistsResult = socialRepository.getPopularArtists(20)

            val errors = mutableListOf<String>()
            listOf(trendingResult, newReleasesResult, categoriesResult, artistsResult).forEach { result ->
                if (result is ApiResult.Error) errors.add(result.message)
            }

            _state.value = DiscoverUiState(
                isLoading = false,
                error = errors.firstOrNull(),
                trending = (trendingResult as? ApiResult.Success)?.data ?: emptyList(),
                newReleases = (newReleasesResult as? ApiResult.Success)?.data ?: emptyList(),
                categories = (categoriesResult as? ApiResult.Success)?.data ?: emptyList(),
                popularArtists = (artistsResult as? ApiResult.Success)?.data ?: emptyList(),
            )
        }
    }
}
