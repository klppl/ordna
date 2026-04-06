package io.github.klppl.ordna.di

import android.content.Context
import io.github.klppl.ordna.data.local.TaskDao
import io.github.klppl.ordna.data.local.TaskDatabase
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
