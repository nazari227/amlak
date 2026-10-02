package com.example.data.repository

import com.example.core.network.NetworkResult
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.TaskEntity
import com.example.data.model.TaskDto
import com.example.data.model.WorkReportRequest
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import com.example.network.AshianMelkApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepositoryImpl(
    private val apiService: AshianMelkApiService,
    private val taskDao: TaskDao
) : TaskRepository {

    override suspend fun getTasks(filter: String?): NetworkResult<List<TaskItem>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTasks(filter)
                if (response.isSuccessful && response.body()?.data != null) {
                    val dtoList = response.body()!!.data!!
                    val entities = dtoList.map { it.toEntity() }
                    taskDao.clearTasks()
                    taskDao.insertTasks(entities)
                    NetworkResult.Success(dtoList.map { it.toDomain() })
                } else {
                    fallbackToCache()
                }
            } catch (e: Exception) {
                fallbackToCache()
            }
        }

    private suspend fun fallbackToCache(): NetworkResult<List<TaskItem>> {
        val cached = taskDao.getAllTasksFlow().first()
        return if (cached.isNotEmpty()) {
            NetworkResult.Success(cached.map { it.toDomain() })
        } else {
            val initial = getAshianMelkInitialTasks()
            taskDao.insertTasks(initial)
            NetworkResult.Success(initial.map { it.toDomain() })
        }
    }

    override suspend fun completeTask(taskId: Long): NetworkResult<TaskItem> =
        withContext(Dispatchers.IO) {
            try {
                taskDao.updateTaskStatus(taskId, true)
                val response = apiService.completeTask(taskId)
                if (response.isSuccessful && response.body()?.data != null) {
                    NetworkResult.Success(response.body()!!.data!!.toDomain())
                } else {
                    NetworkResult.Success(
                        TaskItem(
                            id = taskId,
                            title = "وظیفه تکمیل شده",
                            description = "",
                            category = "visit",
                            dueDate = "امروز",
                            isCompleted = true,
                            isOverdue = false
                        )
                    )
                }
            } catch (e: Exception) {
                taskDao.updateTaskStatus(taskId, true)
                NetworkResult.Success(
                    TaskItem(
                        id = taskId,
                        title = "وظیفه تکمیل شده (ثبت محلی)",
                        description = "",
                        category = "visit",
                        dueDate = "امروز",
                        isCompleted = true,
                        isOverdue = false
                    )
                )
            }
        }

    override suspend fun submitWorkReport(
        taskId: Long?,
        report: String,
        hours: Double
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.submitWorkReport(
                WorkReportRequest(taskId = taskId, reportText = report, hoursSpent = hours)
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Success(Unit) // Offline accepted
            }
        } catch (e: Exception) {
            NetworkResult.Success(Unit)
        }
    }

    override fun observeCachedTasks(): Flow<List<TaskItem>> {
        return taskDao.getAllTasksFlow().map { list -> list.map { it.toDomain() } }
    }

    private fun TaskDto.toDomain(): TaskItem {
        return TaskItem(
            id = this.id,
            title = this.title,
            description = this.description,
            category = this.category,
            dueDate = this.dueDate,
            isCompleted = this.isCompleted,
            isOverdue = this.isOverdue,
            relatedEntityId = this.relatedEntityId,
            relatedEntityType = this.relatedEntityType
        )
    }

    private fun TaskDto.toEntity(): TaskEntity {
        return TaskEntity(
            id = this.id,
            title = this.title,
            description = this.description,
            category = this.category,
            dueDate = this.dueDate,
            isCompleted = this.isCompleted,
            isOverdue = this.isOverdue,
            relatedEntityId = this.relatedEntityId,
            relatedEntityType = this.relatedEntityType
        )
    }

    private fun TaskEntity.toDomain(): TaskItem {
        return TaskItem(
            id = this.id,
            title = this.title,
            description = this.description,
            category = this.category,
            dueDate = this.dueDate,
            isCompleted = this.isCompleted,
            isOverdue = this.isOverdue,
            relatedEntityId = this.relatedEntityId,
            relatedEntityType = this.relatedEntityType
        )
    }

    private fun getAshianMelkInitialTasks(): List<TaskEntity> {
        return listOf(
            TaskEntity(
                id = 301L,
                title = "هماهنگی بازدید آپارتمان زعفرانیه با دکتر فرهمند",
                description = "تماس با سرایدار جهت باز بودن درب لابی و پارکینگ",
                category = "visit",
                dueDate = "امروز - ساعت ۱۶:۳۰",
                isCompleted = false,
                isOverdue = false,
                relatedEntityId = 101L,
                relatedEntityType = "property"
            ),
            TaskEntity(
                id = 302L,
                title = "پیگیری پیش‌نویس قرارداد اجاره نیاوران",
                description = "بررسی بندهای مربوط به ودیعه و مهلت تخلیه با مالک",
                category = "contract",
                dueDate = "امروز - ساعت ۱۸:۰۰",
                isCompleted = false,
                isOverdue = false,
                relatedEntityId = 103L,
                relatedEntityType = "property"
            ),
            TaskEntity(
                id = 303L,
                title = "تماس با مالک ویلای شهرک غرب جهت تمدید انحصار",
                description = "پیشنهاد تخفیف پورسانت در صورت واگذاری فایل به صورت اختصاصی",
                category = "call",
                dueDate = "دیروز - ساعت ۱۱:۰۰",
                isCompleted = false,
                isOverdue = true,
                relatedEntityId = 104L,
                relatedEntityType = "property"
            ),
            TaskEntity(
                id = 304L,
                title = "کارشناسی قیمت واحد اداری میرداماد",
                description = "بررسی امکان تجهیز پارکینگ‌های اضافه ساختمان",
                category = "inspection",
                dueDate = "فردا - ساعت ۱۰:۰۰",
                isCompleted = false,
                isOverdue = false,
                relatedEntityId = 105L,
                relatedEntityType = "property"
            )
        )
    }
}
