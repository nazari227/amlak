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
                if (response.isSuccessful && response.body()?.data != null) {
                    val dtoList = response.body()!!.data!!
                    val entities = dtoList.map { it.toEntity() }
                    notificationDao.clearNotifications()
                    notificationDao.insertNotifications(entities)
                    NetworkResult.Success(dtoList.map { it.toDomain() })
                } else {
                    fallbackToCache()
                }
            } catch (e: Exception) {
                fallbackToCache()
            }
        }

    private suspend fun fallbackToCache(): NetworkResult<List<AppNotification>> {
        val cached = notificationDao.getAllNotificationsFlow().first()
        return if (cached.isNotEmpty()) {
            NetworkResult.Success(cached.map { it.toDomain() })
        } else {
            val initial = getAshianMelkInitialNotifications()
            notificationDao.insertNotifications(initial)
            NetworkResult.Success(initial.map { it.toDomain() })
        }
    }

    override suspend fun markAsRead(id: Long): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
        try {
            apiService.markNotificationAsRead(id)
        } catch (_: Exception) {}
        NetworkResult.Success(Unit)
    }

    override suspend fun markAllAsRead(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead()
        try {
            apiService.markAllNotificationsAsRead()
        } catch (_: Exception) {}
        NetworkResult.Success(Unit)
    }

    override fun observeCachedNotifications(): Flow<List<AppNotification>> {
        return notificationDao.getAllNotificationsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun observeUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCountFlow()
    }

    private fun NotificationDto.toDomain(): AppNotification {
        return AppNotification(
            id = this.id,
            title = this.title,
            message = this.message,
            type = this.type,
            targetId = this.targetId,
            isRead = this.isRead,
            createdAt = this.createdAt
        )
    }

    private fun NotificationDto.toEntity(): NotificationEntity {
        return NotificationEntity(
            id = this.id,
            title = this.title,
            message = this.message,
            type = this.type,
            targetId = this.targetId,
            isRead = this.isRead,
            createdAt = this.createdAt
        )
    }

    private fun NotificationEntity.toDomain(): AppNotification {
        return AppNotification(
            id = this.id,
            title = this.title,
            message = this.message,
            type = this.type,
            targetId = this.targetId,
            isRead = this.isRead,
            createdAt = this.createdAt
        )
    }

    private fun getAshianMelkInitialNotifications(): List<NotificationEntity> {
        return listOf(
            NotificationEntity(
                id = 401L,
                title = "متقاضی جدید برای منطقه زعفرانیه",
                message = "متقاضی جدید با بودجه ۳۵ میلیارد تومان به شما اختصاص داده شد.",
                type = "demand",
                targetId = 201L,
                isRead = false,
                createdAt = "۱۰ دقیقه پیش"
            ),
            NotificationEntity(
                id = 402L,
                title = "یادآوری قرار بازدید ملک",
                message = "قرار بازدید با دکتر فرهمند در پنت‌هاوس زعفرانیه ساعت ۱۶:۳۰ هماهنگ شده است.",
                type = "appointment",
                targetId = 101L,
                isRead = false,
                createdAt = "۴۵ دقیقه پیش"
            ),
            NotificationEntity(
                id = 403L,
                title = "تغییر قیمت ملک سعادت‌آباد",
                message = "مالک کد AM-7319 قیمت ملک را به ۴۵ میلیارد تومان اصلاح کرد.",
                type = "property",
                targetId = 102L,
                isRead = true,
                createdAt = "دیروز"
            )
        )
    }
}
