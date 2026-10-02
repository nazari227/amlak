package com.example.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksUiState(
    val isLoading: Boolean = false,
    val tasks: List<TaskItem> = emptyList(),
    val activeTab: String = "today",
    val isReportDialogOpen: Boolean = false,
    val selectedTaskForReport: TaskItem? = null,
    val reportSubmittedSuccess: Boolean = false,
    val errorMessage: String? = null
)

class TasksViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init { loadTasks() }

    fun selectTab(tab: String) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        loadTasks(tab)
    }

    fun loadTasks(tab: String = _uiState.value.activeTab) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = taskRepository.getTasks(tab)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tasks = result.data,
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

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            when (val result = taskRepository.completeTask(taskId)) {
                is NetworkResult.Success -> {
                    val updated = _uiState.value.tasks.map {
                        if (it.id == taskId) result.data else it
                    }
                    _uiState.value = _uiState.value.copy(tasks = updated, errorMessage = null)
                }
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun openWorkReportDialog(task: TaskItem? = null) {
        _uiState.value = _uiState.value.copy(
            isReportDialogOpen = true,
            selectedTaskForReport = task,
            reportSubmittedSuccess = false,
            errorMessage = null
        )
    }

    fun closeWorkReportDialog() {
        _uiState.value = _uiState.value.copy(isReportDialogOpen = false)
    }

    fun submitWorkReport(reportText: String, hours: Double) {
        if (reportText.isBlank()) return
        viewModelScope.launch {
            val taskId = _uiState.value.selectedTaskForReport?.id
            when (val result = taskRepository.submitWorkReport(taskId, reportText, hours)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    isReportDialogOpen = false,
                    reportSubmittedSuccess = true,
                    errorMessage = null
                )
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
