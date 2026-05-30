package com.example.ntfysms.di

import com.example.ntfysms.data.CredentialRepository
import com.example.ntfysms.data.CredentialRepositoryImpl
import com.example.ntfysms.data.LogRepository
import com.example.ntfysms.data.LogRepositoryImpl
import com.example.ntfysms.data.NtfyRepository
import com.example.ntfysms.data.NtfyRepositoryImpl
import com.example.ntfysms.data.SettingsRepository
import com.example.ntfysms.data.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLogRepository(impl: LogRepositoryImpl): LogRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCredentialRepository(impl: CredentialRepositoryImpl): CredentialRepository

    @Binds
    @Singleton
    abstract fun bindNtfyRepository(impl: NtfyRepositoryImpl): NtfyRepository
}
