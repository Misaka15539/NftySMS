package com.example.ntfysms.di

import android.content.Context
import androidx.room.Room
import com.example.ntfysms.data.AppDatabase
import com.example.ntfysms.data.LogEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "sms_ntfy_relay.db",
    ).build()

    @Provides
    @Singleton
    fun provideLogEntryDao(database: AppDatabase): LogEntryDao =
        database.logEntryDao()
}
