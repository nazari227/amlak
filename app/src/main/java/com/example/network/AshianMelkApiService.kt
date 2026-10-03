package com.example.network

import com.example.data.model.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface AshianMelkApiService {

    companion object {
        const val BASE_URL = "https://my.ashianmelk.ir/wp-json/iranamlak-mobile/v1/"
    }

    @GET("health")
    suspend fun health(): Response<BaseApiResponse<Map<String, String>>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<BaseApiResponse<TokenResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<BaseApiResponse<TokenResponse>>

    @POST("auth/logout")
    suspend fun logoutDevice(): Response<BaseApiResponse<Map<String, Boolean>>>

    @POST("auth/logout-all")
    suspend fun logoutAllDevices(): Response<BaseApiResponse<Map<String, Any>>>

    @GET("me")
    suspend fun getUserProfile(): Response<BaseApiResponse<MeResponse>>

    @GET("sessions")
    suspend fun getActiveSessions(): Response<BaseApiResponse<SessionsResponse>>

    @GET("bootstrap")
    suspend fun getBootstrap(): Response<BaseApiResponse<BootstrapDto>>

    @GET("cases")
    suspend fun getCases(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("q") search: String? = null,
        @Query("status") status: String? = null,
        @Query("transaction_type") transactionType: String? = null,
        @Query("lifecycle") lifecycle: String? = null
    ): Response<BaseApiResponse<CasesResponse>>

    @GET("cases/{id}")
    suspend fun getCaseDetail(@Path("id") id: Long): Response<BaseApiResponse<CaseDetailResponse>>

    @POST("cases")
    suspend fun createCase(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateCaseRequest
    ): Response<BaseApiResponse<CaseMutationResponse>>

    @PATCH("cases/{id}")
    suspend fun updateCase(
        @Path("id") id: Long,
        @Body request: UpdateCaseRequest
    ): Response<BaseApiResponse<CaseMutationResponse>>

    @POST("uploads")
    suspend fun beginUpload(@Body request: BeginUploadRequest): Response<BaseApiResponse<UploadResponse>>

    @GET("uploads/{uploadId}")
    suspend fun getUploadStatus(@Path("uploadId") uploadId: String): Response<BaseApiResponse<UploadResponse>>

    @PUT("uploads/{uploadId}")
    suspend fun uploadChunk(
        @Path("uploadId") uploadId: String,
        @Header("X-Upload-Offset") offset: Long,
        @Header("Content-Type") contentType: String = "application/octet-stream",
        @Body bytes: RequestBody
    ): Response<BaseApiResponse<UploadResponse>>

    @POST("uploads/{uploadId}/complete")
    suspend fun completeUpload(@Path("uploadId") uploadId: String): Response<BaseApiResponse<UploadResponse>>

    @GET("demands")
    suspend fun getDemands(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("status") status: String? = null,
        @Query("transaction_type") transactionType: String? = null,
        @Query("property_type") propertyType: String? = null,
        @Query("lifecycle") lifecycle: String = "active",
        @Query("q") query: String? = null
    ): Response<BaseApiResponse<DemandsResponse>>

    @GET("demands/{id}")
    suspend fun getDemandDetail(@Path("id") id: Long): Response<BaseApiResponse<DemandDetailResponse>>

    @GET("tasks")
    suspend fun getTasks(
        @Query("limit") limit: Int = 30,
        @Query("lifecycle") lifecycle: String = "active",
        @Query("focus") focus: String = "all"
    ): Response<BaseApiResponse<TasksResponse>>

    @PATCH("tasks/{id}")
    suspend fun updateTask(
        @Path("id") taskId: Long,
        @Body request: TaskUpdateRequest
    ): Response<BaseApiResponse<TaskResponse>>

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): Response<BaseApiResponse<NotificationsResponse>>

    @POST("notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") id: Long
    ): Response<BaseApiResponse<Map<String, Boolean>>>
}
