package com.example.ntfysms.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.ntfysms.domain.AuthConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// CredentialKeys — string constants for EncryptedSharedPreferences keys
// ---------------------------------------------------------------------------

object CredentialKeys {
    const val AUTH_TYPE = "auth_type"       // "NONE" | "BEARER" | "BASIC"
    const val BEARER_TOKEN = "bearer_token"
    const val USERNAME = "username"
    const val PASSWORD = "password"
}

// ---------------------------------------------------------------------------
// CredentialRepository — interface for secure credential storage
// ---------------------------------------------------------------------------

interface CredentialRepository {

    /**
     * Read the currently stored [AuthConfig].
     * Returns null if no credentials have been saved yet.
     * This is a synchronous read because [EncryptedSharedPreferences] reads
     * are synchronous; always call this from a background coroutine.
     */
    fun getAuthConfig(): AuthConfig?

    /** Persist the given [AuthConfig] to the encrypted store. */
    suspend fun saveAuthConfig(config: AuthConfig)

    /** Remove all credential keys from the encrypted store. */
    suspend fun clearAuthConfig()
}

// ---------------------------------------------------------------------------
// CredentialRepositoryImpl — EncryptedSharedPreferences-backed implementation
// ---------------------------------------------------------------------------

@Singleton
class CredentialRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CredentialRepository {

    /**
     * Lazily initialise the [EncryptedSharedPreferences] instance.
     * The [MasterKey] uses AES-256-GCM backed by the Android Keystore.
     */
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getAuthConfig(): AuthConfig? {
        val authType = prefs.getString(CredentialKeys.AUTH_TYPE, null) ?: return null
        return when (authType) {
            "NONE" -> AuthConfig.None
            "BEARER" -> {
                val token = prefs.getString(CredentialKeys.BEARER_TOKEN, null) ?: return null
                AuthConfig.Bearer(token)
            }
            "BASIC" -> {
                val username = prefs.getString(CredentialKeys.USERNAME, null) ?: return null
                val password = prefs.getString(CredentialKeys.PASSWORD, null) ?: return null
                AuthConfig.Basic(username, password)
            }
            else -> null
        }
    }

    override suspend fun saveAuthConfig(config: AuthConfig) {
        withContext(Dispatchers.IO) {
            prefs.edit().apply {
                when (config) {
                    is AuthConfig.None -> {
                        putString(CredentialKeys.AUTH_TYPE, "NONE")
                        remove(CredentialKeys.BEARER_TOKEN)
                        remove(CredentialKeys.USERNAME)
                        remove(CredentialKeys.PASSWORD)
                    }
                    is AuthConfig.Bearer -> {
                        putString(CredentialKeys.AUTH_TYPE, "BEARER")
                        putString(CredentialKeys.BEARER_TOKEN, config.token)
                        remove(CredentialKeys.USERNAME)
                        remove(CredentialKeys.PASSWORD)
                    }
                    is AuthConfig.Basic -> {
                        putString(CredentialKeys.AUTH_TYPE, "BASIC")
                        remove(CredentialKeys.BEARER_TOKEN)
                        putString(CredentialKeys.USERNAME, config.username)
                        putString(CredentialKeys.PASSWORD, config.password)
                    }
                }
                apply()
            }
        }
    }

    override suspend fun clearAuthConfig() {
        withContext(Dispatchers.IO) {
            prefs.edit().apply {
                remove(CredentialKeys.AUTH_TYPE)
                remove(CredentialKeys.BEARER_TOKEN)
                remove(CredentialKeys.USERNAME)
                remove(CredentialKeys.PASSWORD)
                apply()
            }
        }
    }
}
