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
    val activeTab: String = "today", // "today", "overdue", "upcoming"
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

    init {
        loadTasks()
    }

    fun selectTab(tab: String) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        loadTasks(tab)
    }

    fun loadTasks(tab: String = _uiState.value.activeTab) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = taskRepository.getTasks(tab)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tasks = result.getOrNull() ?: emptyList(),
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

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
            // Update in-memory list
            val updated = _uiState.value.tasks.map {
                if (it.id == taskId) it.copy(isCompleted = true) else it
            }
            _uiState.value = _uiState.value.copy(tasks = updated)
        }
    }

    fun openWorkReportDialog(task: TaskItem? = null) {
        _uiState.value = _uiState.value.copy(
            isReportDialogOpen = true,
            selectedTaskForReport = task,
            reportSubmittedSuccess = false
        )
    }

    fun closeWorkReportDialog() {
        _uiState.value = _uiState.value.copy(isReportDialogOpen = false)
    }

    fun submitWorkReport(reportText: String, hours: Double) {
        if (reportText.isBlank()) return
        viewModelScope.launch {
            val taskId = _uiState.value.selectedTaskForReport?.id
            taskRepository.submitWorkReport(taskId, reportText, hours)
            _uiState.value = _uiState.value.copy(
                isReportDialogOpen = false,
                reportSubmittedSuccess = true
            )
        }
    }
}
