package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkMonitor
import com.example.domain.model.DashboardSummary
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val userProfile: UserProfile? = null,
    val isOnline: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val dashboardRepository: DashboardRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        loadDashboard()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    fun loadDashboard() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val profile = authRepository.getCurrentUser()
        _uiState.value = _uiState.value.copy(userProfile = profile)

        viewModelScope.launch {
            val result = dashboardRepository.getDashboardSummary()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = result.getOrNull(),
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = (result as? com.example.core.network.NetworkResult.Error)?.message
                )
            }
        }
    }
}
