package com.example.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.AppNotification
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        observeUnread()
    }

    private fun observeUnread() {
        viewModelScope.launch {
            notificationRepository.observeUnreadCount().collect { count ->
                _uiState.value = _uiState.value.copy(unreadCount = count)
            }
        }
    }

    fun loadNotifications() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = notificationRepository.getNotifications()) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    notifications = result.data,
                    unreadCount = result.data.count { !it.isRead },
                    errorMessage = null
                )
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            when (val result = notificationRepository.markAsRead(id)) {
                is NetworkResult.Success -> {
                    val updated = _uiState.value.notifications.map {
                        if (it.id == id) it.copy(isRead = true) else it
                    }
                    _uiState.value = _uiState.value.copy(
                        notifications = updated,
                        unreadCount = updated.count { !it.isRead },
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (val result = notificationRepository.markAllAsRead()) {
                is NetworkResult.Success -> {
                    val updated = _uiState.value.notifications.map { it.copy(isRead = true) }
                    _uiState.value = _uiState.value.copy(notifications = updated, unreadCount = 0, errorMessage = null)
                }
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
