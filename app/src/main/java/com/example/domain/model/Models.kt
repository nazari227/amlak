package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: Long,
    val username: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val branchId: Long,
    val branchName: String,
    val role: String,
    val avatarUrl: String? = null,
    val capabilities: Map<String, Boolean> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class DeviceSession(
    val sessionId: String,
    val deviceName: String,
    val lastActive: String,
    val ipAddress: String = "",
    val isCurrentDevice: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Property(
    val id: Long,
    val code: String,
    val title: String,
    val transactionType: String,
    val propertyType: String,
    val status: String,
    val branchId: Long,
    val branchName: String,
    val consultantName: String,
    val price: Long = 0,
    val mortgagePrice: Long = 0,
    val area: Double = 0.0,
    val rooms: Int = 0,
    val floor: Int? = null,
    val totalFloors: Int? = null,
    val yearBuilt: Int? = null,
    val city: String = "",
    val neighborhood: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val thumbnail: String? = null,
    val images: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val baseVersion: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PropertyFilter(
    val branchId: Long? = null,
    val transactionType: String? = null,
    val propertyType: String? = null,
    val status: String? = null,
    val consultantId: Long? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val minArea: Double? = null,
    val maxArea: Double? = null,
    val neighborhood: String? = null,
    val code: String? = null,
    val searchQuery: String? = null
)

@JsonClass(generateAdapter = true)
data class PropertyDraft(
    val idempotencyKey: String,
    val baseVersion: String = "",
    val title: String = "",
    val transactionType: String = "sale",
    val propertyType: String = "apartment",
    val price: Long = 0,
    val mortgagePrice: Long = 0,
    val area: Double = 0.0,
    val rooms: Int = 2,
    val yearBuilt: Int = 1400,
    val floor: Int = 1,
    val totalFloors: Int = 5,
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerNotes: String = "",
    val city: String = "ملایر",
    val neighborhood: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val localImagePaths: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Demand(
    val id: Long,
    val clientName: String,
    val clientPhone: String,
    val transactionType: String,
    val propertyType: String,
    val preferredNeighborhoods: List<String>,
    val minBudget: Long,
    val maxBudget: Long,
    val minArea: Double,
    val rooms: Int? = null,
    val status: String,
    val assignedConsultant: String,
    val matchingPropertiesCount: Int = 0,
    val followUpNotes: List<DemandFollowUpNote> = emptyList(),
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class DemandFollowUpNote(
    val id: Long,
    val author: String,
    val content: String,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class TaskItem(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val dueDate: String,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val relatedEntityId: Long? = null,
    val relatedEntityType: String? = null
)

@JsonClass(generateAdapter = true)
data class Appointment(
    val id: Long,
    val title: String,
    val clientName: String,
    val propertyTitle: String,
    val time: String,
    val location: String
)

@JsonClass(generateAdapter = true)
data class AppNotification(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val targetId: Long? = null,
    val isRead: Boolean,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class DashboardSummary(
    val todayTasksCount: Int,
    val todayAppointmentsCount: Int,
    val newDemandsCount: Int,
    val unreadNotificationsCount: Int,
    val activePropertiesCount: Int,
    val todayTasks: List<TaskItem> = emptyList(),
    val todayAppointments: List<Appointment> = emptyList(),
    val recentDemands: List<Demand> = emptyList(),
    val recentNotifications: List<AppNotification> = emptyList()
)
