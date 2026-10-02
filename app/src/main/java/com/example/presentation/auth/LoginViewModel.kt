package com.example.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isMfaRequired: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(isLoggedIn = authRepository.isLoggedIn()))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pendingLogin: String = ""
    private var pendingPassword: String = ""

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "لطفاً نام کاربری و رمز عبور را وارد کنید.")
            return
        }
        pendingLogin = username.trim()
        pendingPassword = password
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = authRepository.login(pendingLogin, pendingPassword)) {
                is NetworkResult.Success -> when (val data = result.data) {
                    is LoginResult.Success -> completeLogin(data.profile)
                    LoginResult.MfaRequired -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isMfaRequired = true,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun verifyMfa(code: String) {
        if (pendingLogin.isBlank() || pendingPassword.isBlank()) {
            resetState()
            return
        }
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "کد احراز هویت را وارد کنید.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = authRepository.verifyMfa(pendingLogin, pendingPassword, code.trim())) {
                is NetworkResult.Success -> completeLogin(result.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                is NetworkResult.Loading -> Unit
            }
        }
    }

    private fun completeLogin(profile: UserProfile) {
        pendingPassword = ""
        pendingLogin = ""
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isMfaRequired = false,
            isLoggedIn = true,
            userProfile = profile,
            errorMessage = null
        )
    }

    fun resetState() {
        pendingPassword = ""
        pendingLogin = ""
        _uiState.value = LoginUiState(isLoggedIn = authRepository.isLoggedIn())
    }

    override fun onCleared() {
        pendingPassword = ""
        pendingLogin = ""
        super.onCleared()
    }
}
