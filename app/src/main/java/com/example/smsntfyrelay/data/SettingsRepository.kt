package com.example.smsntfyrelay.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// SettingsKeys — typed DataStore preference keys
// ---------------------------------------------------------------------------

object SettingsKeys {
    val TOPIC_URL = stringPreferencesKey("topic_url")
    val RELAY_ENABLED = booleanPreferencesKey("relay_enabled")
    val SSL_VALIDATION_ENABLED = booleanPreferencesKey("ssl_validation_enabled")
}

// ---------------------------------------------------------------------------
// SettingsRepository — interface for reading and writing app settings
// ---------------------------------------------------------------------------

interface SettingsRepository {

    /** Emits the currently saved ntfy topic URL; defaults to empty string. */
    val topicUrl: Flow<String>

    /** Emits whether SMS relaying is enabled; defaults to false. */
    val relayEnabled: Flow<Boolean>

    /** Emits whether SSL/TLS certificate validation is enabled; defaults to true. */
    val sslValidationEnabled: Flow<Boolean>

    /** Persist a new topic URL to storage. */
    suspend fun saveTopicUrl(url: String)

    /** Enable or disable SMS relaying. */
    suspend fun setRelayEnabled(enabled: Boolean)

    /** Enable or disable SSL/TLS certificate validation. */
    suspend fun setSslValidationEnabled(enabled: Boolean)
}

// ---------------------------------------------------------------------------
// SettingsRepositoryImpl — DataStore-backed implementation
// ---------------------------------------------------------------------------

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val topicUrl: Flow<String> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.TOPIC_URL] ?: "" }

    override val relayEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.RELAY_ENABLED] ?: false }

    override val sslValidationEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.SSL_VALIDATION_ENABLED] ?: true }

    override suspend fun saveTopicUrl(url: String) {
        dataStore.edit { prefs -> prefs[SettingsKeys.TOPIC_URL] = url }
    }

    override suspend fun setRelayEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.RELAY_ENABLED] = enabled }
    }

    override suspend fun setSslValidationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.SSL_VALIDATION_ENABLED] = enabled }
    }
}
