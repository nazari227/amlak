package com.example.presentation.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkMonitor
import com.example.core.network.NetworkResult
import com.example.domain.model.Property
import com.example.domain.model.PropertyFilter
import com.example.domain.repository.PropertyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PropertyListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val properties: List<Property> = emptyList(),
    val currentPage: Int = 1,
    val hasReachedEnd: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: PropertyFilter = PropertyFilter(),
    val isFilterSheetVisible: Boolean = false,
    val errorMessage: String? = null,
    val isOnline: Boolean = true
)

class PropertyListViewModel(
    private val propertyRepository: PropertyRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyListUiState())
    val uiState: StateFlow<PropertyListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeNetwork()
        loadProperties(isRefresh = true)
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            val newFilter = _uiState.value.activeFilter.copy(searchQuery = query.ifBlank { null })
            _uiState.value = _uiState.value.copy(activeFilter = newFilter)
            loadProperties(isRefresh = true)
        }
    }

    fun applyFilter(filter: PropertyFilter) {
        _uiState.value = _uiState.value.copy(
            activeFilter = filter,
            isFilterSheetVisible = false
        )
        loadProperties(isRefresh = true)
    }

    fun resetFilter() {
        _uiState.value = _uiState.value.copy(
            activeFilter = PropertyFilter(),
            isFilterSheetVisible = false,
            searchQuery = ""
        )
        loadProperties(isRefresh = true)
    }

    fun toggleFilterSheet(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isFilterSheetVisible = visible)
    }

    fun loadProperties(isRefresh: Boolean = false) {
        if (isRefresh) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentPage = 1,
                hasReachedEnd = false,
                errorMessage = null
            )
        } else {
            if (_uiState.value.isLoading || _uiState.value.isLoadingMore || _uiState.value.hasReachedEnd) return
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }

        val page = if (isRefresh) 1 else _uiState.value.currentPage + 1

        viewModelScope.launch {
            val result = propertyRepository.getProperties(
                page = page,
                perPage = 10,
                filter = _uiState.value.activeFilter
            )

            when (result) {
                is NetworkResult.Success -> {
                    val newItems = result.data
                    val mergedList = if (isRefresh) newItems else _uiState.value.properties + newItems
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        properties = mergedList,
                        currentPage = page,
                        hasReachedEnd = newItems.isEmpty() || newItems.size < 10,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
