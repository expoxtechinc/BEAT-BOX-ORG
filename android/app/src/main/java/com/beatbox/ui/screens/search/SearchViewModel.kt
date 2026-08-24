package com.beatbox.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.data.dto.SearchHistoryDto
import com.beatbox.data.dto.SearchResultsDto
import com.beatbox.data.repository.ApiResult
import com.beatbox.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: SearchResultsDto? = null,
    val history: List<SearchHistoryDto> = emptyList(),
    val error: String? = null,
    val hasSearched: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHistory()
    }

    fun onQueryChange(newQuery: String) {
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _state.value = _state.value.copy(
                query = "",
                results = null,
                hasSearched = false,
                isLoading = false,
                error = null,
            )
            return
        }
        _state.value = _state.value.copy(query = newQuery, error = null, isLoading = true)
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            performSearch(newQuery)
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(query.trim())
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(
                results = null,
                hasSearched = false,
                isLoading = false,
                error = null,
            )
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null, hasSearched = true)
        when (val result = searchRepository.search(query)) {
            is ApiResult.Success -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    results = result.data,
                    error = null,
                )
                // Persist to search history, then refresh the list
                searchRepository.saveSearchHistory(query)
                loadHistory()
            }
            is ApiResult.Error -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.message,
                )
            }
            is ApiResult.Loading -> Unit
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            when (val result = searchRepository.getSearchHistory()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(history = result.data)
                }
                else -> Unit
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            when (searchRepository.clearSearchHistory()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(history = emptyList())
                }
                else -> Unit
            }
        }
    }

    fun selectHistoryItem(query: String) {
        _state.value = _state.value.copy(query = query)
        search(query)
    }
}
