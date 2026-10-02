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
    val mfaToken: String? = null,
    val isLoggedIn: Boolean = false,
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(isLoggedIn = authRepository.isLoggedIn()))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "لطفاً نام کاربری و رمز عبور را وارد کنید.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.login(username, password)
            when (result) {
                is NetworkResult.Success -> {
                    when (val data = result.data) {
                        is LoginResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                userProfile = data.profile,
                                errorMessage = null
                            )
                        }
                        is LoginResult.MfaRequired -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isMfaRequired = true,
                                mfaToken = data.mfaToken,
                                errorMessage = null
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun verifyMfa(code: String) {
        val token = _uiState.value.mfaToken ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.verifyMfa(token, code)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userProfile = result.data,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState(isLoggedIn = authRepository.isLoggedIn())
    }
}
