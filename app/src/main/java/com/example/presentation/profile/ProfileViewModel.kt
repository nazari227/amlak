package com.example.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.DeviceSession
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthRepository
import com.example.security.EncryptedTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: UserProfile? = null,
    val sessions: List<DeviceSession> = emptyList(),
    val isScreenshotProtected: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: EncryptedTokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            userProfile = authRepository.getCurrentUser(),
            isScreenshotProtected = tokenStorage.isScreenshotProtectionEnabled()
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            val result = authRepository.getActiveSessions()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(sessions = result.getOrNull() ?: emptyList())
            }
        }
    }

    fun toggleScreenshotProtection(enabled: Boolean) {
        tokenStorage.setScreenshotProtection(enabled)
        _uiState.value = _uiState.value.copy(isScreenshotProtected = enabled)
    }

    fun logoutCurrentDevice() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            authRepository.logoutDevice()
            _uiState.value = _uiState.value.copy(isLoading = false, isLoggedOut = true)
        }
    }

    fun logoutAllDevices() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            authRepository.logoutAllDevices()
            _uiState.value = _uiState.value.copy(isLoading = false, isLoggedOut = true)
        }
    }
}
