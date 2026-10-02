package com.example.presentation.demands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.Demand
import com.example.domain.model.DemandFollowUpNote
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

    init {
        loadDemands()
    }

    fun loadDemands(status: String? = _uiState.value.selectedStatus) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedStatus = status, errorMessage = null)
        viewModelScope.launch {
            val result = demandRepository.getDemands(page = 1, status = status)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    demands = result.getOrNull() ?: emptyList(),
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = (result as? NetworkResult.Error)?.message
                )
            }
        }
    }

    fun selectDemand(demandId: Long) {
        val demand = _uiState.value.demands.find { it.id == demandId }
        _uiState.value = _uiState.value.copy(selectedDemand = demand, isMatchingLoading = true)
        viewModelScope.launch {
            val detailResult = demandRepository.getDemandDetail(demandId)
            if (detailResult.isSuccess) {
                _uiState.value = _uiState.value.copy(selectedDemand = detailResult.getOrNull())
            }

            val matchingResult = demandRepository.getMatchingProperties(demandId)
            _uiState.value = _uiState.value.copy(
                isMatchingLoading = false,
                matchingProperties = matchingResult.getOrNull() ?: emptyList()
            )
        }
    }

    fun addFollowUpNote(demandId: Long, noteText: String) {
        if (noteText.isBlank()) return
        viewModelScope.launch {
            val result = demandRepository.addFollowUpNote(demandId, noteText)
            if (result.isSuccess) {
                val newNote = result.getOrNull()
                val currentDemand = _uiState.value.selectedDemand
                if (currentDemand != null && newNote != null) {
                    val updatedNotes = listOf(newNote) + currentDemand.followUpNotes
                    _uiState.value = _uiState.value.copy(
                        selectedDemand = currentDemand.copy(followUpNotes = updatedNotes)
                    )
                }
            }
        }
    }
}
