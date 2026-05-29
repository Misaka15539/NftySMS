package com.example.smsntfyrelay.di

import com.example.smsntfyrelay.data.CredentialRepository
import com.example.smsntfyrelay.data.CredentialRepositoryImpl
import com.example.smsntfyrelay.data.LogRepository
import com.example.smsntfyrelay.data.LogRepositoryImpl
import com.example.smsntfyrelay.data.NtfyRepository
import com.example.smsntfyrelay.data.NtfyRepositoryImpl
import com.example.smsntfyrelay.data.SettingsRepository
import com.example.smsntfyrelay.data.SettingsRepositoryImpl
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
