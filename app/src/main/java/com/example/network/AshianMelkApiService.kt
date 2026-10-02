package com.example.network

import com.example.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Ashian Melk Mobile REST API Client
 * Base URL: https://my.ashianmelk.ir/wp-json/iranamlak-mobile/v1/
 *
 * All endpoints interact directly with the production WordPress REST API namespace.
 */
interface AshianMelkApiService {

    companion object {
        const val BASE_URL = "https://my.ashianmelk.ir/wp-json/iranamlak-mobile/v1/"
    }

    // ==========================================
    // 1. Authentication & Device Sessions
    // ==========================================

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<BaseApiResponse<LoginResponse>>

    @POST("auth/verify-mfa")
    suspend fun verifyMfa(
        @Body request: MfaVerifyRequest
    ): Response<BaseApiResponse<LoginResponse>>

    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<BaseApiResponse<RefreshTokenResponse>>

    @POST("auth/logout-device")
    suspend fun logoutDevice(
        @Body request: LogoutDeviceRequest
    ): Response<BaseApiResponse<Unit>>

    @POST("auth/logout-all")
    suspend fun logoutAllDevices(): Response<BaseApiResponse<Unit>>

    @GET("user/sessions")
    suspend fun getActiveSessions(): Response<BaseApiResponse<List<DeviceSessionDto>>>

    @GET("user/profile")
    suspend fun getUserProfile(): Response<BaseApiResponse<UserDto>>

    // ==========================================
    // 2. Dashboard / Summary
    // ==========================================

    @GET("dashboard/summary")
    suspend fun getDashboardSummary(): Response<BaseApiResponse<DashboardSummaryDto>>

    // ==========================================
    // 3. Properties (CRUD, Pagination, Filters)
    // ==========================================

    @GET("properties")
    suspend fun getProperties(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
        @Query("search") search: String? = null,
        @Query("branch_id") branchId: Long? = null,
        @Query("transaction_type") transactionType: String? = null,
        @Query("property_type") propertyType: String? = null,
        @Query("status") status: String? = null,
        @Query("consultant_id") consultantId: Long? = null,
        @Query("min_price") minPrice: Long? = null,
        @Query("max_price") maxPrice: Long? = null,
        @Query("min_area") minArea: Double? = null,
        @Query("max_area") maxArea: Double? = null,
        @Query("neighborhood") neighborhood: String? = null,
        @Query("code") code: String? = null
    ): Response<BaseApiResponse<PropertyListResponse>>

    @GET("properties/{id}")
    suspend fun getPropertyDetail(
        @Path("id") id: Long
    ): Response<BaseApiResponse<PropertyDto>>

    @POST("properties")
    suspend fun createProperty(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: CreatePropertyRequest
    ): Response<BaseApiResponse<PropertyDto>>

    @PUT("properties/{id}")
    suspend fun updateProperty(
        @Path("id") id: Long,
        @Header("If-Match") baseVersion: String,
        @Body request: CreatePropertyRequest
    ): Response<BaseApiResponse<PropertyDto>>

    @Multipart
    @POST("properties/{id}/images")
    suspend fun uploadPropertyImageChunk(
        @Path("id") propertyId: Long,
        @Part image: MultipartBody.Part,
        @Part("chunk_index") chunkIndex: RequestBody,
        @Part("total_chunks") totalChunks: RequestBody,
        @Part("sha256") sha256: RequestBody
    ): Response<BaseApiResponse<ImageUploadChunkResponse>>

    // ==========================================
    // 4. Demands (متقاضیان)
    // ==========================================

    @GET("demands")
    suspend fun getDemands(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("status") status: String? = null,
        @Query("consultant_id") consultantId: Long? = null
    ): Response<BaseApiResponse<List<DemandDto>>>

    @GET("demands/{id}")
    suspend fun getDemandDetail(
        @Path("id") id: Long
    ): Response<BaseApiResponse<DemandDto>>

    @GET("demands/{id}/matching-properties")
    suspend fun getMatchingProperties(
        @Path("id") id: Long
    ): Response<BaseApiResponse<List<PropertyDto>>>

    @POST("demands/{id}/follow-up")
    suspend fun addDemandFollowUp(
        @Path("id") demandId: Long,
        @Body request: AddFollowUpNoteRequest
    ): Response<BaseApiResponse<DemandFollowUpNoteDto>>

    // ==========================================
    // 5. Tasks & Appointments (وظایف و قرارها)
    // ==========================================

    @GET("tasks")
    suspend fun getTasks(
        @Query("status") filter: String? = null // "today", "overdue", "upcoming", "completed"
    ): Response<BaseApiResponse<List<TaskDto>>>

    @POST("tasks/{id}/complete")
    suspend fun completeTask(
        @Path("id") taskId: Long
    ): Response<BaseApiResponse<TaskDto>>

    @POST("tasks/report")
    suspend fun submitWorkReport(
        @Body request: WorkReportRequest
    ): Response<BaseApiResponse<Unit>>

    // ==========================================
    // 6. Notifications (اعلان‌ها)
    // ==========================================

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1
    ): Response<BaseApiResponse<List<NotificationDto>>>

    @POST("notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") id: Long
    ): Response<BaseApiResponse<Unit>>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<BaseApiResponse<Unit>>
}
