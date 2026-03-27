package com.ordna.android.di

import android.content.Context
import com.ordna.android.data.local.TaskDao
import com.ordna.android.data.local.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase =
        TaskDatabase.getInstance(context)

    @Provides
    fun provideTaskDao(db: TaskDatabase): TaskDao = db.taskDao()
}
