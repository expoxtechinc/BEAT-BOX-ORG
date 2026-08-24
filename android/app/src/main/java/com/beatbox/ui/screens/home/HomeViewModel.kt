package com.beatbox.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.data.dto.MusicDto
import com.beatbox.data.repository.ApiResult
import com.beatbox.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val featured: List<MusicDto> = emptyList(),
    val trending: List<MusicDto> = emptyList(),
    val newReleases: List<MusicDto> = emptyList(),
    val recentlyPlayed: List<MusicDto> = emptyList(),
    val recommended: List<MusicDto> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(isLoading = true))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { loadData() }

    fun loadData() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val results = listOf(
                musicRepository.getFeatured(10),
                musicRepository.getTrending(10),
                musicRepository.getNewReleases(10),
                musicRepository.getRecentlyPlayed(10),
                musicRepository.getRecommended(10),
            )

            val errors = mutableListOf<String>()
            results.forEach { result ->
                if (result is ApiResult.Error) errors.add(result.message)
            }

            _state.value = HomeUiState(
                isLoading = false,
                error = errors.firstOrNull(),
                featured = (results[0] as? ApiResult.Success)?.data ?: emptyList(),
                trending = (results[1] as? ApiResult.Success)?.data ?: emptyList(),
                newReleases = (results[2] as? ApiResult.Success)?.data ?: emptyList(),
                recentlyPlayed = (results[3] as? ApiResult.Success)?.data ?: emptyList(),
                recommended = (results[4] as? ApiResult.Success)?.data ?: emptyList(),
            )
        }
    }
}
