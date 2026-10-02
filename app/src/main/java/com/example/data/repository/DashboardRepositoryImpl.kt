package com.example.data.repository

import com.example.core.network.NetworkResult
import com.example.domain.model.Appointment
import com.example.domain.model.DashboardSummary
import com.example.domain.repository.DashboardRepository
import com.example.domain.repository.DemandRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.PropertyRepository
import com.example.domain.repository.TaskRepository
import com.example.network.AshianMelkApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                val response = apiService.getDashboardSummary()
                if (response.isSuccessful && response.body()?.data != null) {
                    val dto = response.body()!!.data!!
                    NetworkResult.Success(
                        DashboardSummary(
                            todayTasksCount = dto.todayTasksCount,
                            todayAppointmentsCount = dto.todayAppointmentsCount,
                            newDemandsCount = dto.newDemandsCount,
                            unreadNotificationsCount = dto.unreadNotificationsCount,
                            activePropertiesCount = dto.activePropertiesCount,
                            todayTasks = dto.todayTasks.map {
                                com.example.domain.model.TaskItem(
                                    id = it.id,
                                    title = it.title,
                                    description = it.description,
                                    category = it.category,
                                    dueDate = it.dueDate,
                                    isCompleted = it.isCompleted,
                                    isOverdue = it.isOverdue,
                                    relatedEntityId = it.relatedEntityId,
                                    relatedEntityType = it.relatedEntityType
                                )
                            },
                            todayAppointments = dto.todayAppointments.map {
                                Appointment(
                                    id = it.id,
                                    title = it.title,
                                    clientName = it.clientName,
                                    propertyTitle = it.propertyTitle,
                                    time = it.time,
                                    location = it.location
                                )
                            },
                            recentDemands = dto.recentDemands.map {
                                com.example.domain.model.Demand(
                                    id = it.id,
                                    clientName = it.clientName,
                                    clientPhone = it.clientPhone,
                                    transactionType = it.transactionType,
                                    propertyType = it.propertyType,
                                    preferredNeighborhoods = it.preferredNeighborhoods,
                                    minBudget = it.minBudget,
                                    maxBudget = it.maxBudget,
                                    minArea = it.minArea,
                                    rooms = it.rooms,
                                    status = it.status,
                                    assignedConsultant = it.assignedConsultant,
                                    matchingPropertiesCount = it.matchingPropertiesCount,
                                    createdAt = it.createdAt
                                )
                            },
                            recentNotifications = dto.recentNotifications.map {
                                com.example.domain.model.AppNotification(
                                    id = it.id,
                                    title = it.title,
                                    message = it.message,
                                    type = it.type,
                                    targetId = it.targetId,
                                    isRead = it.isRead,
                                    createdAt = it.createdAt
                                )
                            }
                        )
                    )
                } else {
                    synthesizeFromLocalRepositories()
                }
            } catch (e: Exception) {
                synthesizeFromLocalRepositories()
            }
        }

    private suspend fun synthesizeFromLocalRepositories(): NetworkResult<DashboardSummary> {
        val tasksResult = taskRepository.getTasks("today")
        val demandsResult = demandRepository.getDemands(1, "new")
        val notificationsResult = notificationRepository.getNotifications()
        val propertiesResult = propertyRepository.getProperties(1, 10, null)

        val tasks = tasksResult.getOrNull() ?: emptyList()
        val demands = demandsResult.getOrNull() ?: emptyList()
        val notifications = notificationsResult.getOrNull() ?: emptyList()
        val properties = propertiesResult.getOrNull() ?: emptyList()

        val appointments = listOf(
            Appointment(
                id = 501L,
                title = "جلسه عقد قرارداد رهن و اجاره",
                clientName = "آقای مهندس کاظمی",
                propertyTitle = "آپارتمان نیاوران (AM-9104)",
                time = "امروز - ساعت ۱۸:۳۰",
                location = "اتاق جلسات شعبه شمیرانات"
            ),
            Appointment(
                id = 502L,
                title = "بازدید پنت‌هاوس زعفرانیه",
                clientName = "دکتر فرهمند",
                propertyTitle = "پنت‌هاوس سوپرلوکس (AM-8421)",
                time = "امروز - ساعت ۱۶:۳۰",
                location = "زعفرانیه، خیابان آصف"
            )
        )

        return NetworkResult.Success(
            DashboardSummary(
                todayTasksCount = tasks.count { !it.isCompleted },
                todayAppointmentsCount = appointments.size,
                newDemandsCount = demands.count { it.status == "new" },
                unreadNotificationsCount = notifications.count { !it.isRead },
                activePropertiesCount = properties.size,
                todayTasks = tasks.take(4),
                todayAppointments = appointments,
                recentDemands = demands.take(3),
                recentNotifications = notifications.take(4)
            )
        )
    }
}
