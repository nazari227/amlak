package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BaseApiResponse<T>(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "code") val code: String? = null
)

// --- Auth DTOs ---

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "device_name") val deviceName: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "status") val status: String, // "success" or "mfa_required"
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "mfa_token") val mfaToken: String? = null,
    @Json(name = "user") val user: UserDto? = null
)

@JsonClass(generateAdapter = true)
data class MfaVerifyRequest(
    @Json(name = "mfa_token") val mfaToken: String,
    @Json(name = "code") val code: String,
    @Json(name = "device_id") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "device_id") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class LogoutDeviceRequest(
    @Json(name = "device_id") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long,
    @Json(name = "username") val username: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "branch_id") val branchId: Long,
    @Json(name = "branch_name") val branchName: String,
    @Json(name = "role") val role: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceSessionDto(
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "last_active") val lastActive: String,
    @Json(name = "ip_address") val ipAddress: String,
    @Json(name = "is_current") val isCurrent: Boolean
)

// --- Property DTOs ---

@JsonClass(generateAdapter = true)
data class PropertyListResponse(
    @Json(name = "items") val items: List<PropertyDto> = emptyList(),
    @Json(name = "total") val total: Int = 0,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "total_pages") val totalPages: Int = 1
)

@JsonClass(generateAdapter = true)
data class PropertyDto(
    @Json(name = "id") val id: Long,
    @Json(name = "code") val code: String,
    @Json(name = "title") val title: String,
    @Json(name = "transaction_type") val transactionType: String,
    @Json(name = "property_type") val propertyType: String,
    @Json(name = "status") val status: String,
    @Json(name = "branch_id") val branchId: Long,
    @Json(name = "branch_name") val branchName: String,
    @Json(name = "consultant_name") val consultantName: String,
    @Json(name = "price") val price: Long,
    @Json(name = "mortgage_price") val mortgagePrice: Long = 0,
    @Json(name = "area") val area: Double,
    @Json(name = "rooms") val rooms: Int,
    @Json(name = "floor") val floor: Int? = null,
    @Json(name = "total_floors") val totalFloors: Int? = null,
    @Json(name = "year_built") val yearBuilt: Int? = null,
    @Json(name = "city") val city: String,
    @Json(name = "neighborhood") val neighborhood: String,
    @Json(name = "address") val address: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "images") val images: List<String> = emptyList(),
    @Json(name = "features") val features: List<String> = emptyList(),
    @Json(name = "base_version") val baseVersion: Int = 1,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreatePropertyRequest(
    @Json(name = "idempotency_key") val idempotencyKey: String,
    @Json(name = "base_version") val baseVersion: Int = 1,
    @Json(name = "title") val title: String,
    @Json(name = "transaction_type") val transactionType: String,
    @Json(name = "property_type") val propertyType: String,
    @Json(name = "price") val price: Long,
    @Json(name = "mortgage_price") val mortgagePrice: Long = 0,
    @Json(name = "area") val area: Double,
    @Json(name = "rooms") val rooms: Int,
    @Json(name = "floor") val floor: Int? = null,
    @Json(name = "total_floors") val totalFloors: Int? = null,
    @Json(name = "year_built") val yearBuilt: Int? = null,
    @Json(name = "owner_name") val ownerName: String,
    @Json(name = "owner_phone") val ownerPhone: String,
    @Json(name = "owner_notes") val ownerNotes: String? = null,
    @Json(name = "city") val city: String,
    @Json(name = "neighborhood") val neighborhood: String,
    @Json(name = "address") val address: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "features") val features: List<String> = emptyList(),
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ImageUploadChunkResponse(
    @Json(name = "image_id") val imageId: String,
    @Json(name = "chunk_index") val chunkIndex: Int,
    @Json(name = "is_completed") val isCompleted: Boolean,
    @Json(name = "url") val url: String? = null
)

// --- Demand DTOs ---

@JsonClass(generateAdapter = true)
data class DemandDto(
    @Json(name = "id") val id: Long,
    @Json(name = "client_name") val clientName: String,
    @Json(name = "client_phone") val clientPhone: String,
    @Json(name = "transaction_type") val transactionType: String,
    @Json(name = "property_type") val propertyType: String,
    @Json(name = "preferred_neighborhoods") val preferredNeighborhoods: List<String> = emptyList(),
    @Json(name = "min_budget") val minBudget: Long,
    @Json(name = "max_budget") val maxBudget: Long,
    @Json(name = "min_area") val minArea: Double,
    @Json(name = "rooms") val rooms: Int? = null,
    @Json(name = "status") val status: String,
    @Json(name = "assigned_consultant") val assignedConsultant: String,
    @Json(name = "matching_properties_count") val matchingPropertiesCount: Int = 0,
    @Json(name = "follow_up_notes") val followUpNotes: List<DemandFollowUpNoteDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class DemandFollowUpNoteDto(
    @Json(name = "id") val id: Long,
    @Json(name = "author") val author: String,
    @Json(name = "content") val content: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class AddFollowUpNoteRequest(
    @Json(name = "content") val content: String
)

// --- Task & Appointment DTOs ---

@JsonClass(generateAdapter = true)
data class TaskDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "category") val category: String,
    @Json(name = "due_date") val dueDate: String,
    @Json(name = "is_completed") val isCompleted: Boolean,
    @Json(name = "is_overdue") val isOverdue: Boolean,
    @Json(name = "related_entity_id") val relatedEntityId: Long? = null,
    @Json(name = "related_entity_type") val relatedEntityType: String? = null
)

@JsonClass(generateAdapter = true)
data class AppointmentDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "client_name") val clientName: String,
    @Json(name = "property_title") val propertyTitle: String,
    @Json(name = "time") val time: String,
    @Json(name = "location") val location: String
)

@JsonClass(generateAdapter = true)
data class WorkReportRequest(
    @Json(name = "task_id") val taskId: Long? = null,
    @Json(name = "report_text") val reportText: String,
    @Json(name = "hours_spent") val hoursSpent: Double = 1.0
)

// --- Notification DTOs ---

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "type") val type: String,
    @Json(name = "target_id") val targetId: Long? = null,
    @Json(name = "is_read") val isRead: Boolean,
    @Json(name = "created_at") val createdAt: String
)

// --- Dashboard Summary DTO ---

@JsonClass(generateAdapter = true)
data class DashboardSummaryDto(
    @Json(name = "today_tasks_count") val todayTasksCount: Int = 0,
    @Json(name = "today_appointments_count") val todayAppointmentsCount: Int = 0,
    @Json(name = "new_demands_count") val newDemandsCount: Int = 0,
    @Json(name = "unread_notifications_count") val unreadNotificationsCount: Int = 0,
    @Json(name = "active_properties_count") val activePropertiesCount: Int = 0,
    @Json(name = "today_tasks") val todayTasks: List<TaskDto> = emptyList(),
    @Json(name = "today_appointments") val todayAppointments: List<AppointmentDto> = emptyList(),
    @Json(name = "recent_demands") val recentDemands: List<DemandDto> = emptyList(),
    @Json(name = "recent_notifications") val recentNotifications: List<NotificationDto> = emptyList()
)
