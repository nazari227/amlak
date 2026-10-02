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
    val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceSession(
    val sessionId: String,
    val deviceName: String,
    val lastActive: String,
    val ipAddress: String,
    val isCurrentDevice: Boolean
)

@JsonClass(generateAdapter = true)
data class Property(
    val id: Long,
    val code: String,
    val title: String,
    val transactionType: String, // "sale", "rent", "mortgage"
    val propertyType: String,    // "apartment", "villa", "office", "land", "store"
    val status: String,          // "active", "pending", "reserved", "sold"
    val branchId: Long,
    val branchName: String,
    val consultantName: String,
    val price: Long,             // Total price in Toman or Rent
    val mortgagePrice: Long = 0, // For rent/mortgage combinations
    val area: Double,            // Square meters
    val rooms: Int,
    val floor: Int? = null,
    val totalFloors: Int? = null,
    val yearBuilt: Int? = null,
    val city: String,
    val neighborhood: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val thumbnail: String? = null,
    val images: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val baseVersion: Int = 1,
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
    val baseVersion: Int = 1,
    // Step 1: Basic Property Data
    val title: String = "",
    val transactionType: String = "sale", // sale, rent, mortgage
    val propertyType: String = "apartment", // apartment, villa, office, land, store
    val price: Long = 0,
    val mortgagePrice: Long = 0,
    val area: Double = 0.0,
    val rooms: Int = 2,
    val yearBuilt: Int = 1400,
    val floor: Int = 1,
    val totalFloors: Int = 5,
    // Step 2: Owner & Contact Information (Sensitive)
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerNotes: String = "",
    // Step 3: Location
    val city: String = "تهران",
    val neighborhood: String = "",
    val address: String = "",
    val latitude: Double = 35.6892,
    val longitude: Double = 51.3890,
    // Step 4: Photos & Features
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
    val status: String, // "new", "in_progress", "matched", "closed"
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
    val category: String, // "call", "visit", "contract", "inspection"
    val dueDate: String,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val relatedEntityId: Long? = null,
    val relatedEntityType: String? = null // "property", "demand"
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
    val type: String, // "property", "demand", "task", "appointment", "system"
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
