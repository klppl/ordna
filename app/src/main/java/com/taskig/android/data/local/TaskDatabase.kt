package com.taskig.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var widgetInstance: TaskDatabase? = null

        fun getWidgetInstance(context: Context): TaskDatabase {
            return widgetInstance ?: synchronized(this) {
                widgetInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "taskig.db",
                ).fallbackToDestructiveMigration().build().also { widgetInstance = it }
            }
        }
    }
}
