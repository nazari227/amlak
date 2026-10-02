package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BaseApiResponse<T>(
    @Json(name = "data") val data: T? = null,
    @Json(name = "meta") val meta: ApiMeta? = null
)

@JsonClass(generateAdapter = true)
data class ApiMeta(
    @Json(name = "api_version") val apiVersion: String? = null,
    @Json(name = "request_id") val requestId: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "login") val login: String,
    @Json(name = "password") val password: String,
    @Json(name = "mfa_code") val mfaCode: String = "",
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "platform") val platform: String = "android"
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "token_type") val tokenType: String = "Bearer",
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "refresh_expires_in") val refreshExpiresIn: Int
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "device_id") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long,
    @Json(name = "display_name") val displayName: String = "",
    @Json(name = "staff_type") val staffType: String = "",
    @Json(name = "branch_id") val branchId: Long = 0,
    @Json(name = "capabilities") val capabilities: Map<String, Boolean> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class MeResponse(@Json(name = "user") val user: UserDto)

@JsonClass(generateAdapter = true)
data class DeviceSessionDto(
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "device_name") val deviceName: String = "",
    @Json(name = "platform") val platform: String = "",
    @Json(name = "issued_at") val issuedAt: String = "",
    @Json(name = "last_used_at") val lastUsedAt: String = "",
    @Json(name = "access_expires_at") val accessExpiresAt: String = "",
    @Json(name = "refresh_expires_at") val refreshExpiresAt: String = ""
)

@JsonClass(generateAdapter = true)
data class SessionsResponse(@Json(name = "sessions") val sessions: List<DeviceSessionDto> = emptyList())

@JsonClass(generateAdapter = true)
data class PaginationDto(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "per_page") val perPage: Int = 20,
    @Json(name = "total") val total: Int = 0,
    @Json(name = "pages") val pages: Int = 1
)

@JsonClass(generateAdapter = true)
data class CasePropertyDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "code") val code: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "type") val type: String = "",
    @Json(name = "area") val area: Double = 0.0,
    @Json(name = "land_area") val landArea: Double = 0.0,
    @Json(name = "bedrooms") val bedrooms: Int = 0
)

@JsonClass(generateAdapter = true)
data class CaseLocationDto(
    @Json(name = "district") val district: String = "",
    @Json(name = "neighborhood") val neighborhood: String = ""
)

@JsonClass(generateAdapter = true)
data class CaseDto(
    @Json(name = "id") val id: Long,
    @Json(name = "case_code") val caseCode: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "transaction_type") val transactionType: String = "",
    @Json(name = "priority") val priority: String = "",
    @Json(name = "branch_id") val branchId: Long = 0,
    @Json(name = "branch_name") val branchName: String = "",
    @Json(name = "assigned_agent_user_id") val assignedAgentUserId: Long = 0,
    @Json(name = "property") val property: CasePropertyDto = CasePropertyDto(),
    @Json(name = "location") val location: CaseLocationDto = CaseLocationDto(),
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "closed_at") val closedAt: String = "",
    @Json(name = "version") val version: String = ""
)

@JsonClass(generateAdapter = true)
data class CasesResponse(
    @Json(name = "items") val items: List<CaseDto> = emptyList(),
    @Json(name = "pagination") val pagination: PaginationDto = PaginationDto()
)

@JsonClass(generateAdapter = true)
data class CaseDetailResponse(
    @Json(name = "case") val caseItem: CaseDto,
    @Json(name = "server_version") val serverVersion: String = ""
)

@JsonClass(generateAdapter = true)
data class CaseMutationResponse(
    @Json(name = "case") val caseItem: CaseDto,
    @Json(name = "server_version") val serverVersion: String? = null,
    @Json(name = "idempotent_replay") val idempotentReplay: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CasePayloadRequest(
    @Json(name = "title") val title: String,
    @Json(name = "transaction_type") val transactionType: String,
    @Json(name = "property_type") val propertyType: String,
    @Json(name = "price_display_mode") val priceDisplayMode: String = "numeric",
    @Json(name = "price") val price: Long? = null,
    @Json(name = "deposit_amount") val depositAmount: Long? = null,
    @Json(name = "rent_amount") val rentAmount: Long? = null,
    @Json(name = "area") val area: Double? = null,
    @Json(name = "bedrooms") val bedrooms: Int? = null,
    @Json(name = "build_year") val buildYear: Int? = null,
    @Json(name = "floor_no") val floorNo: Int? = null,
    @Json(name = "total_floors") val totalFloors: Int? = null,
    @Json(name = "owner_name") val ownerName: String,
    @Json(name = "owner_mobile") val ownerMobile: String,
    @Json(name = "owner_phone") val ownerPhone: String? = null,
    @Json(name = "city_name") val cityName: String,
    @Json(name = "district_name") val districtName: String = "",
    @Json(name = "neighborhood_name") val neighborhoodName: String = "",
    @Json(name = "exact_address") val exactAddress: String? = null,
    @Json(name = "exact_lat") val exactLat: Double? = null,
    @Json(name = "exact_lng") val exactLng: Double? = null,
    @Json(name = "public_description") val publicDescription: String? = null,
    @Json(name = "internal_summary") val internalSummary: String? = null,
    @Json(name = "priority") val priority: String = "normal"
)

@JsonClass(generateAdapter = true)
data class CreateCaseRequest(
    @Json(name = "payload") val payload: CasePayloadRequest,
    @Json(name = "draft_client_key") val draftClientKey: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateCaseRequest(
    @Json(name = "base_version") val baseVersion: String,
    @Json(name = "payload") val payload: CasePayloadRequest
)

@JsonClass(generateAdapter = true)
data class BeginUploadRequest(
    @Json(name = "file_name") val fileName: String,
    @Json(name = "mime_type") val mimeType: String,
    @Json(name = "total_bytes") val totalBytes: Long,
    @Json(name = "case_id") val caseId: Long? = null,
    @Json(name = "draft_client_key") val draftClientKey: String? = null,
    @Json(name = "sha256") val sha256: String
)

@JsonClass(generateAdapter = true)
data class UploadDto(
    @Json(name = "upload_id") val uploadId: String,
    @Json(name = "status") val status: String? = null,
    @Json(name = "offset") val offset: Long = 0,
    @Json(name = "total_bytes") val totalBytes: Long = 0,
    @Json(name = "chunk_max_bytes") val chunkMaxBytes: Long? = null,
    @Json(name = "case_id") val caseId: Long = 0,
    @Json(name = "draft_client_key") val draftClientKey: String = "",
    @Json(name = "attachment_id") val attachmentId: Long = 0,
    @Json(name = "expires_at") val expiresAt: String = "",
    @Json(name = "complete") val complete: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UploadResponse(@Json(name = "upload") val upload: UploadDto)

@JsonClass(generateAdapter = true)
data class DemandLocationDto(
    @Json(name = "city") val city: String = "",
    @Json(name = "district") val district: String = "",
    @Json(name = "neighborhood") val neighborhood: String = ""
)

@JsonClass(generateAdapter = true)
data class DemandBudgetDto(
    @Json(name = "min") val min: Double = 0.0,
    @Json(name = "max") val max: Double = 0.0,
    @Json(name = "deposit_max") val depositMax: Double = 0.0,
    @Json(name = "rent_max") val rentMax: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DemandRequirementsDto(
    @Json(name = "area_min") val areaMin: Double = 0.0,
    @Json(name = "area_max") val areaMax: Double = 0.0,
    @Json(name = "bedrooms_min") val bedroomsMin: Int = 0
)

@JsonClass(generateAdapter = true)
data class DemandDto(
    @Json(name = "id") val id: Long,
    @Json(name = "status") val status: String = "",
    @Json(name = "transaction_type") val transactionType: String = "",
    @Json(name = "property_type") val propertyType: String = "",
    @Json(name = "assigned_branch_id") val assignedBranchId: Long = 0,
    @Json(name = "assigned_agent_user_id") val assignedAgentUserId: Long = 0,
    @Json(name = "intake_recipient") val intakeRecipient: String = "",
    @Json(name = "location") val location: DemandLocationDto = DemandLocationDto(),
    @Json(name = "budget") val budget: DemandBudgetDto = DemandBudgetDto(),
    @Json(name = "requirements") val requirements: DemandRequirementsDto = DemandRequirementsDto(),
    @Json(name = "match_count") val matchCount: Int = 0,
    @Json(name = "proposal_count") val proposalCount: Int = 0,
    @Json(name = "last_interaction_at") val lastInteractionAt: String = "",
    @Json(name = "next_follow_up_at") val nextFollowUpAt: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class DemandMatchDto(
    @Json(name = "listing_id") val listingId: Long = 0,
    @Json(name = "listing_public_id") val listingPublicId: String = "",
    @Json(name = "property_id") val propertyId: Long = 0,
    @Json(name = "score") val score: Double = 0.0,
    @Json(name = "status") val status: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "property_type") val propertyType: String = "",
    @Json(name = "area") val area: Double = 0.0,
    @Json(name = "bedrooms") val bedrooms: Int = 0,
    @Json(name = "district") val district: String = "",
    @Json(name = "neighborhood") val neighborhood: String = "",
    @Json(name = "amount") val amount: Double? = null
)

@JsonClass(generateAdapter = true)
data class DemandsResponse(
    @Json(name = "items") val items: List<DemandDto> = emptyList(),
    @Json(name = "pagination") val pagination: PaginationDto = PaginationDto()
)

@JsonClass(generateAdapter = true)
data class DemandDetailResponse(
    @Json(name = "demand") val demand: DemandDto,
    @Json(name = "matches") val matches: List<DemandMatchDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TaskDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "priority") val priority: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "due_at") val dueAt: String = "",
    @Json(name = "assigned_to") val assignedTo: Long = 0,
    @Json(name = "branch_id") val branchId: Long = 0,
    @Json(name = "branch_name") val branchName: String = "",
    @Json(name = "case_id") val caseId: Long = 0,
    @Json(name = "case_code") val caseCode: String = "",
    @Json(name = "demand_id") val demandId: Long = 0
)

@JsonClass(generateAdapter = true)
data class TasksResponse(@Json(name = "items") val items: List<TaskDto> = emptyList())

@JsonClass(generateAdapter = true)
data class TaskResponse(@Json(name = "task") val task: TaskDto)

@JsonClass(generateAdapter = true)
data class TaskUpdateRequest(
    @Json(name = "status") val status: String,
    @Json(name = "note") val note: String = "",
    @Json(name = "details") val details: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "id") val id: Long,
    @Json(name = "type") val type: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "body") val body: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "entity_type") val entityType: String = "",
    @Json(name = "entity_id") val entityId: Long = 0,
    @Json(name = "read") val read: Boolean = false,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class NotificationsResponse(
    @Json(name = "items") val items: List<NotificationDto> = emptyList(),
    @Json(name = "unread") val unread: Int = 0,
    @Json(name = "pagination") val pagination: PaginationDto = PaginationDto()
)

@JsonClass(generateAdapter = true)
data class BootstrapSummaryDto(
    @Json(name = "active_cases") val activeCases: Int = 0,
    @Json(name = "active_demands") val activeDemands: Int = 0,
    @Json(name = "upcoming_appointments") val upcomingAppointments: Int = 0,
    @Json(name = "unread_notifications") val unreadNotifications: Int = 0
)

@JsonClass(generateAdapter = true)
data class BootstrapDto(
    @Json(name = "user") val user: UserDto,
    @Json(name = "summary") val summary: BootstrapSummaryDto = BootstrapSummaryDto(),
    @Json(name = "recent_cases") val recentCases: List<CaseDto> = emptyList(),
    @Json(name = "recent_demands") val recentDemands: List<DemandDto> = emptyList(),
    @Json(name = "tasks") val tasks: List<TaskDto> = emptyList(),
    @Json(name = "api_features") val apiFeatures: Map<String, Boolean> = emptyMap()
)
