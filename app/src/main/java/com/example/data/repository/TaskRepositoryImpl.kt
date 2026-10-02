package com.example.data.repository

import com.example.core.network.NetworkResult
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.TaskEntity
import com.example.data.model.TaskDto
import com.example.data.model.TaskUpdateRequest
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import com.example.network.AshianMelkApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TaskRepositoryImpl(
    private val apiService: AshianMelkApiService,
    private val taskDao: TaskDao
) : TaskRepository {

    override suspend fun getTasks(filter: String?): NetworkResult<List<TaskItem>> =
        withContext(Dispatchers.IO) {
            val focus = when (filter) {
                "today", "overdue", "waiting", "revision" -> filter
                else -> "all"
            }
            try {
                val response = apiService.getTasks(focus = focus)
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    val tasks = data.items.map { it.toDomain() }
                    taskDao.clearTasks()
                    taskDao.insertTasks(tasks.map { it.toEntity() })
                    NetworkResult.Success(tasks)
                } else {
                    fallbackToCache(response.code())
                }
            } catch (e: Exception) {
                val cached = taskDao.getAllTasksFlow().first()
                if (cached.isNotEmpty()) NetworkResult.Success(cached.map { it.toDomain() })
                else NetworkResult.Error("وظایف در حالت آفلاین قبلاً روی دستگاه ذخیره نشده‌اند.", cause = e)
            }
        }

    private suspend fun fallbackToCache(code: Int): NetworkResult<List<TaskItem>> {
        val cached = taskDao.getAllTasksFlow().first()
        return if (cached.isNotEmpty()) NetworkResult.Success(cached.map { it.toDomain() })
        else NetworkResult.Error("دریافت وظایف از سرور انجام نشد.", code)
    }

    override suspend fun completeTask(taskId: Long): NetworkResult<TaskItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateTask(
                    taskId,
                    TaskUpdateRequest(status = "done", note = "تکمیل از اپ موبایل")
                )
                val task = response.body()?.data?.task
                if (response.isSuccessful && task != null) {
                    val mapped = task.toDomain()
                    taskDao.insertTasks(listOf(mapped.toEntity()))
                    NetworkResult.Success(mapped)
                } else {
                    NetworkResult.Error("تکمیل وظیفه روی سرور ثبت نشد.", response.code())
                }
            } catch (e: Exception) {
                NetworkResult.Error("تکمیل وظیفه نیاز به اتصال آنلاین دارد.", cause = e)
            }
        }

    override suspend fun submitWorkReport(
        taskId: Long?,
        report: String,
        hours: Double
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        if (taskId == null || taskId <= 0) {
            return@withContext NetworkResult.Error("برای ثبت گزارش کار، وظیفه مرتبط باید مشخص باشد.")
        }
        try {
            val response = apiService.updateTask(
                taskId,
                TaskUpdateRequest(
                    status = "in_progress",
                    note = report.trim(),
                    details = mapOf("hours_spent" to hours.toString())
                )
            )
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error("گزارش کار روی سرور ثبت نشد.", response.code())
        } catch (e: Exception) {
            NetworkResult.Error("ثبت گزارش کار نیاز به اتصال آنلاین دارد.", cause = e)
        }
    }

    override fun observeCachedTasks(): Flow<List<TaskItem>> =
        taskDao.getAllTasksFlow().map { list -> list.map { it.toDomain() } }

    private fun TaskDto.toDomain(): TaskItem {
        val completed = status in setOf("done", "closed", "cancelled")
        val overdue = !completed && isOverdue(dueAt)
        val relatedId = when {
            caseId > 0 -> caseId
            demandId > 0 -> demandId
            else -> null
        }
        val relatedType = when {
            caseId > 0 -> "property"
            demandId > 0 -> "demand"
            else -> null
        }
        return TaskItem(
            id = id,
            title = title,
            description = "",
            category = category,
            dueDate = dueAt,
            isCompleted = completed,
            isOverdue = overdue,
            relatedEntityId = relatedId,
            relatedEntityType = relatedType
        )
    }

    private fun isOverdue(value: String): Boolean {
        if (value.isBlank()) return false
        return try {
            val dt = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            dt.toInstant(ZoneOffset.UTC).isBefore(java.time.Instant.now())
        } catch (_: Exception) {
            false
        }
    }

    private fun TaskItem.toEntity(): TaskEntity = TaskEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        dueDate = dueDate,
        isCompleted = isCompleted,
        isOverdue = isOverdue,
        relatedEntityId = relatedEntityId,
        relatedEntityType = relatedEntityType
    )

    private fun TaskEntity.toDomain(): TaskItem = TaskItem(
        id = id,
        title = title,
        description = description,
        category = category,
        dueDate = dueDate,
        isCompleted = isCompleted,
        isOverdue = isOverdue,
        relatedEntityId = relatedEntityId,
        relatedEntityType = relatedEntityType
    )
}
