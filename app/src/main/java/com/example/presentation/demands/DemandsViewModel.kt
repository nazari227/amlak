package com.example.presentation.demands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.Demand
import com.example.domain.model.Property
import com.example.domain.repository.DemandRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemandsUiState(
    val isLoading: Boolean = false,
    val demands: List<Demand> = emptyList(),
    val selectedStatus: String? = null,
    val selectedDemand: Demand? = null,
    val matchingProperties: List<Property> = emptyList(),
    val isMatchingLoading: Boolean = false,
    val errorMessage: String? = null
)

class DemandsViewModel(
    private val demandRepository: DemandRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DemandsUiState())
    val uiState: StateFlow<DemandsUiState> = _uiState.asStateFlow()

    init { loadDemands() }

    fun loadDemands(status: String? = _uiState.value.selectedStatus) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedStatus = status, errorMessage = null)
        viewModelScope.launch {
            when (val result = demandRepository.getDemands(page = 1, status = status)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false, demands = result.data, errorMessage = null
                )
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun selectDemand(demandId: Long) {
        _uiState.value = _uiState.value.copy(isMatchingLoading = true, errorMessage = null)
        viewModelScope.launch {
            val detailResult = demandRepository.getDemandDetail(demandId)
            val matchingResult = demandRepository.getMatchingProperties(demandId)
            _uiState.value = _uiState.value.copy(
                selectedDemand = detailResult.getOrNull() ?: _uiState.value.selectedDemand,
                isMatchingLoading = false,
                matchingProperties = matchingResult.getOrNull().orEmpty(),
                errorMessage = (detailResult as? NetworkResult.Error)?.message
                    ?: (matchingResult as? NetworkResult.Error)?.message
            )
        }
    }

    fun addFollowUpNote(demandId: Long, noteText: String) {
        if (noteText.isBlank()) return
        viewModelScope.launch {
            when (val result = demandRepository.addFollowUpNote(demandId, noteText)) {
                is NetworkResult.Success -> {
                    val current = _uiState.value.selectedDemand ?: return@launch
                    _uiState.value = _uiState.value.copy(
                        selectedDemand = current.copy(followUpNotes = listOf(result.data) + current.followUpNotes),
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
