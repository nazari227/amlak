package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DemandDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.PropertyDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.DemandEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PropertyEntity
import com.example.data.local.entity.TaskEntity

@Database(
    entities = [
        PropertyEntity::class,
        DemandEntity::class,
        TaskEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AshianMelkDatabase : RoomDatabase() {

    abstract fun propertyDao(): PropertyDao
    abstract fun demandDao(): DemandDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AshianMelkDatabase? = null

        fun getInstance(context: Context): AshianMelkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AshianMelkDatabase::class.java,
                    "ashian_melk_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
