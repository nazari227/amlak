package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_properties")
data class PropertyEntity(
    @PrimaryKey val id: Long,
    val code: String,
    val title: String,
    val transactionType: String,
    val propertyType: String,
    val status: String,
    val branchId: Long,
    val branchName: String,
    val consultantName: String,
    val price: Long,
    val mortgagePrice: Long,
    val area: Double,
    val rooms: Int,
    val floor: Int?,
    val totalFloors: Int?,
    val yearBuilt: Int?,
    val city: String,
    val neighborhood: String,
    val thumbnail: String?,
    val baseVersion: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_demands")
data class DemandEntity(
    @PrimaryKey val id: Long,
    val clientName: String,
    val transactionType: String,
    val propertyType: String,
    val preferredNeighborhoodsCsv: String,
    val minBudget: Long,
    val maxBudget: Long,
    val minArea: Double,
    val rooms: Int?,
    val status: String,
    val assignedConsultant: String,
    val matchingPropertiesCount: Int,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_tasks")
data class TaskEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val dueDate: String,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val relatedEntityId: Long?,
    val relatedEntityType: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_notifications")
data class NotificationEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val targetId: Long?,
    val isRead: Boolean,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)
