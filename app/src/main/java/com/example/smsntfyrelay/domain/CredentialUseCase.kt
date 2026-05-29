package com.example.smsntfyrelay.domain

import com.example.smsntfyrelay.data.CredentialRepository
import javax.inject.Inject

/**
 * Use case exposing credential read/write operations.
 * Thin wrapper over [CredentialRepository] for use by ViewModels.
 */
class CredentialUseCase @Inject constructor(
    private val credentialRepository: CredentialRepository,
) {

    /** Read the currently stored [AuthConfig], or null if none has been saved. */
    fun getAuthConfig(): AuthConfig? = credentialRepository.getAuthConfig()

    /** Persist the given [AuthConfig] to the encrypted store. */
    suspend fun saveAuthConfig(config: AuthConfig) = credentialRepository.saveAuthConfig(config)

    /** Remove all credential keys from the encrypted store. */
    suspend fun clearAuthConfig() = credentialRepository.clearAuthConfig()
}
