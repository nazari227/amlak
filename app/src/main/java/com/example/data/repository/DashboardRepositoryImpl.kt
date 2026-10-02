package com.example.data.repository

import com.example.core.network.NetworkResult
import com.example.data.model.AppointmentDto
import com.example.data.model.DemandDto
import com.example.data.model.NotificationDto
import com.example.data.model.TaskDto
import com.example.domain.model.AppNotification
import com.example.domain.model.Appointment
import com.example.domain.model.DashboardSummary
import com.example.domain.model.Demand
import com.example.domain.model.TaskItem
import com.example.domain.repository.DashboardRepository
import com.example.domain.repository.DemandRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.PropertyRepository
import com.example.domain.repository.TaskRepository
import com.example.network.AshianMelkApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DashboardRepositoryImpl(
    private val apiService: AshianMelkApiService,
    private val propertyRepository: PropertyRepository,
    private val demandRepository: DemandRepository,
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository
) : DashboardRepository {

    override suspend fun getDashboardSummary(): NetworkResult<DashboardSummary> =
        withContext(Dispatchers.IO) {
            try {
                val bootstrapResponse = apiService.getBootstrap()
                val bootstrap = bootstrapResponse.body()?.data
                if (!bootstrapResponse.isSuccessful || bootstrap == null) {
                    return@withContext cachedDashboardOrError(bootstrapResponse.code())
                }

                val todayTasksResponse = apiService.getTasks(limit = 30, focus = "today")
                val todayTasks = todayTasksResponse.body()?.data?.items.orEmpty()

                val newDemandsResponse = apiService.getDemands(page = 1, perPage = 5, status = "new")
                val newDemandsData = newDemandsResponse.body()?.data

                val notificationsResponse = apiService.getNotifications(page = 1, perPage = 8)
                val notificationsData = notificationsResponse.body()?.data

                NetworkResult.Success(
                    DashboardSummary(
                        todayTasksCount = todayTasks.size,
                        todayAppointmentsCount = bootstrap.summary.upcomingAppointments,
                        newDemandsCount = newDemandsData?.pagination?.total ?: 0,
                        unreadNotificationsCount = notificationsData?.unread ?: bootstrap.summary.unreadNotifications,
                        activePropertiesCount = bootstrap.summary.activeCases,
                        todayTasks = todayTasks.map { it.toDomain() },
                        todayAppointments = bootstrap.appointments.map { it.toDomain() },
                        recentDemands = bootstrap.recentDemands.map { it.toDomain() },
                        recentNotifications = notificationsData?.items.orEmpty().map { it.toDomain() }
                    )
                )
            } catch (e: Exception) {
                cachedDashboardOrError(cause = e)
            }
        }

    private suspend fun cachedDashboardOrError(
        statusCode: Int? = null,
        cause: Throwable? = null
    ): NetworkResult<DashboardSummary> {
        val tasks = taskRepository.getTasks("today").getOrNull().orEmpty()
        val demands = demandRepository.getDemands(1, "new").getOrNull().orEmpty()
        val notifications = notificationRepository.getNotifications().getOrNull().orEmpty()
        val properties = propertyRepository.getProperties(1, 10, null).getOrNull().orEmpty()

        if (tasks.isEmpty() && demands.isEmpty() && notifications.isEmpty() && properties.isEmpty()) {
            return NetworkResult.Error(
                "داشبورد در حالت آفلاین هنوز داده ذخیره‌شده‌ای ندارد.",
                statusCode = statusCode,
                cause = cause
            )
        }

        return NetworkResult.Success(
            DashboardSummary(
                todayTasksCount = tasks.count { !it.isCompleted },
                todayAppointmentsCount = 0,
                newDemandsCount = demands.count { it.status == "new" },
                unreadNotificationsCount = notifications.count { !it.isRead },
                activePropertiesCount = properties.size,
                todayTasks = tasks.take(8),
                todayAppointments = emptyList(),
                recentDemands = demands.take(8),
                recentNotifications = notifications.take(8)
            )
        )
    }

    private fun TaskDto.toDomain(): TaskItem {
        val completed = status in setOf("done", "closed", "cancelled")
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
            isOverdue = !completed && isOverdue(dueAt),
            relatedEntityId = relatedId,
            relatedEntityType = relatedType
        )
    }

    private fun DemandDto.toDomain(): Demand {
        val places = listOf(location.neighborhood, location.district, location.city)
            .filter { it.isNotBlank() }
            .distinct()
        return Demand(
            id = id,
            clientName = "تقاضا #$id",
            clientPhone = "",
            transactionType = transactionType,
            propertyType = propertyType,
            preferredNeighborhoods = places,
            minBudget = budget.min.toLong(),
            maxBudget = budget.max.toLong(),
            minArea = requirements.areaMin,
            rooms = requirements.bedroomsMin.takeIf { it > 0 },
            status = status,
            assignedConsultant = assignedAgentUserId.takeIf { it > 0 }?.let { "کاربر #$it" }.orEmpty(),
            matchingPropertiesCount = matchCount,
            createdAt = createdAt
        )
    }

    private fun AppointmentDto.toDomain(): Appointment = Appointment(
        id = id,
        title = title,
        clientName = "",
        propertyTitle = caseCode,
        time = startAt,
        location = branchName
    )

    private fun NotificationDto.toDomain(): AppNotification = AppNotification(
        id = id,
        title = title,
        message = body,
        type = entityType.ifBlank { type },
        targetId = entityId.takeIf { it > 0 },
        isRead = read,
        createdAt = createdAt
    )

    private fun isOverdue(value: String): Boolean {
        if (value.isBlank()) return false
        return try {
            LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .toInstant(ZoneOffset.UTC)
                .isBefore(java.time.Instant.now())
        } catch (_: Exception) {
            false
        }
    }
}
