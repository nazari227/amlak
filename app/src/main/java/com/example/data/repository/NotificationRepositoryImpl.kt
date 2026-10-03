package com.example.data.repository

import com.example.core.network.NetworkResult
import com.example.data.local.dao.NotificationDao
import com.example.data.local.entity.NotificationEntity
import com.example.data.model.NotificationDto
import com.example.domain.model.AppNotification
import com.example.domain.repository.NotificationRepository
import com.example.network.AshianMelkApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NotificationRepositoryImpl(
    private val apiService: AshianMelkApiService,
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override suspend fun getNotifications(): NetworkResult<List<AppNotification>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getNotifications()
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    val items = data.items.map { it.toDomain() }
                    notificationDao.clearNotifications()
                    notificationDao.insertNotifications(items.map { it.toEntity() })
                    NetworkResult.Success(items)
                } else {
                    fallbackToCache(response.code())
                }
            } catch (e: Exception) {
                val cached = notificationDao.getAllNotificationsFlow().first()
                if (cached.isNotEmpty()) NetworkResult.Success(cached.map { it.toDomain() })
                else NetworkResult.Error("اعلان‌ها در حالت آفلاین قبلاً روی دستگاه ذخیره نشده‌اند.", cause = e)
            }
        }

    private suspend fun fallbackToCache(code: Int): NetworkResult<List<AppNotification>> {
        val cached = notificationDao.getAllNotificationsFlow().first()
        return if (cached.isNotEmpty()) NetworkResult.Success(cached.map { it.toDomain() })
        else NetworkResult.Error("دریافت اعلان‌ها از سرور انجام نشد.", code)
    }

    override suspend fun markAsRead(id: Long): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.markNotificationAsRead(id)
            if (response.isSuccessful) {
                notificationDao.markAsRead(id)
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("ثبت مشاهده اعلان روی سرور انجام نشد.", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("ثبت مشاهده اعلان نیاز به اتصال آنلاین دارد.", cause = e)
        }
    }

    override suspend fun markAllAsRead(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val unread = notificationDao.getAllNotificationsFlow().first().filter { !it.isRead }
        for (item in unread) {
            try {
                val response = apiService.markNotificationAsRead(item.id)
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error("همه اعلان‌ها روی سرور خوانده نشدند.", response.code())
                }
                notificationDao.markAsRead(item.id)
            } catch (e: Exception) {
                return@withContext NetworkResult.Error("خواندن همه اعلان‌ها نیاز به اتصال آنلاین دارد.", cause = e)
            }
        }
        NetworkResult.Success(Unit)
    }

    override fun observeCachedNotifications(): Flow<List<AppNotification>> =
        notificationDao.getAllNotificationsFlow().map { list -> list.map { it.toDomain() } }

    override fun observeUnreadCount(): Flow<Int> = notificationDao.getUnreadCountFlow()

    private fun NotificationDto.toDomain(): AppNotification = AppNotification(
        id = id,
        title = title,
        message = body,
        type = entityType.ifBlank { type },
        targetId = entityId.takeIf { it > 0 },
        isRead = read,
        createdAt = createdAt
    )

    private fun AppNotification.toEntity(): NotificationEntity = NotificationEntity(
        id = id,
        title = title,
        message = message,
        type = type,
        targetId = targetId,
        isRead = isRead,
        createdAt = createdAt
    )

    private fun NotificationEntity.toDomain(): AppNotification = AppNotification(
        id = id,
        title = title,
        message = message,
        type = type,
        targetId = targetId,
        isRead = isRead,
        createdAt = createdAt
    )
}
