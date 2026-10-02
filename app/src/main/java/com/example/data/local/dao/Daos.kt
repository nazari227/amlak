package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM cached_properties ORDER BY id DESC")
    fun getAllPropertiesFlow(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM cached_properties WHERE id = :id")
    suspend fun getPropertyById(id: Long): PropertyEntity?

    @Query("SELECT * FROM cached_properties WHERE title LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' OR neighborhood LIKE '%' || :query || '%'")
    fun searchProperties(query: String): Flow<List<PropertyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<PropertyEntity>)

    @Query("DELETE FROM cached_properties")
    suspend fun clearProperties()
}

@Dao
interface DemandDao {
    @Query("SELECT * FROM cached_demands ORDER BY id DESC")
    fun getAllDemandsFlow(): Flow<List<DemandEntity>>

    @Query("SELECT * FROM cached_demands WHERE id = :id")
    suspend fun getDemandById(id: Long): DemandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDemands(demands: List<DemandEntity>)

    @Query("DELETE FROM cached_demands")
    suspend fun clearDemands()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM cached_tasks ORDER BY dueDate ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE cached_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateTaskStatus(id: Long, completed: Boolean)

    @Query("DELETE FROM cached_tasks")
    suspend fun clearTasks()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM cached_notifications ORDER BY id DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM cached_notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE cached_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE cached_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM cached_notifications")
    suspend fun clearNotifications()
}
