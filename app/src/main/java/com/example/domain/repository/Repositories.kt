package com.example.domain.repository

import com.example.core.network.NetworkResult
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): NetworkResult<LoginResult>
    suspend fun verifyMfa(mfaToken: String, code: String): NetworkResult<UserProfile>
    suspend fun logoutDevice(): NetworkResult<Unit>
    suspend fun logoutAllDevices(): NetworkResult<Unit>
    suspend fun getActiveSessions(): NetworkResult<List<DeviceSession>>
    fun isLoggedIn(): Boolean
    fun getCurrentUser(): UserProfile
}

sealed class LoginResult {
    data class Success(val profile: UserProfile) : LoginResult()
    data class MfaRequired(val mfaToken: String) : LoginResult()
}

interface PropertyRepository {
    suspend fun getProperties(
        page: Int = 1,
        perPage: Int = 15,
        filter: PropertyFilter? = null
    ): NetworkResult<List<Property>>

    suspend fun getPropertyDetail(id: Long): NetworkResult<Property>

    suspend fun createProperty(
        draft: PropertyDraft,
        onProgress: (Float) -> Unit = {}
    ): NetworkResult<Property>

    suspend fun updateProperty(
        id: Long,
        draft: PropertyDraft,
        baseVersion: Int
    ): NetworkResult<Property>

    fun observeCachedProperties(): Flow<List<Property>>
    fun searchCachedProperties(query: String): Flow<List<Property>>

    // Offline encrypted drafts management
    suspend fun saveDraft(draft: PropertyDraft)
    suspend fun getDraft(idempotencyKey: String): PropertyDraft?
    suspend fun getLatestDraft(): PropertyDraft?
    suspend fun getAllDrafts(): List<PropertyDraft>
    suspend fun deleteDraft(idempotencyKey: String)
}

interface DemandRepository {
    suspend fun getDemands(page: Int = 1, status: String? = null): NetworkResult<List<Demand>>
    suspend fun getDemandDetail(id: Long): NetworkResult<Demand>
    suspend fun getMatchingProperties(demandId: Long): NetworkResult<List<Property>>
    suspend fun addFollowUpNote(demandId: Long, note: String): NetworkResult<DemandFollowUpNote>
    fun observeCachedDemands(): Flow<List<Demand>>
}

interface TaskRepository {
    suspend fun getTasks(filter: String? = null): NetworkResult<List<TaskItem>>
    suspend fun completeTask(taskId: Long): NetworkResult<TaskItem>
    suspend fun submitWorkReport(taskId: Long?, report: String, hours: Double): NetworkResult<Unit>
    fun observeCachedTasks(): Flow<List<TaskItem>>
}

interface NotificationRepository {
    suspend fun getNotifications(): NetworkResult<List<AppNotification>>
    suspend fun markAsRead(id: Long): NetworkResult<Unit>
    suspend fun markAllAsRead(): NetworkResult<Unit>
    fun observeCachedNotifications(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
}

interface DashboardRepository {
    suspend fun getDashboardSummary(): NetworkResult<DashboardSummary>
}
