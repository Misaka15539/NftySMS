package com.example.ntfysms.data

import com.example.ntfysms.domain.AuthConfig
import com.example.ntfysms.domain.RelayResult

// ---------------------------------------------------------------------------
// NtfyRepository — interface for posting notifications to an ntfy server
// ---------------------------------------------------------------------------

interface NtfyRepository {

    /**
     * Send an HTTP POST to the given [topicUrl] with the provided [title] and
     * [body].
     *
     * @param topicUrl      Full URL of the ntfy topic (e.g. `https://ntfy.sh/my-topic`).
     * @param title         Value for the `Title` HTTP header.
     * @param body          Plain-text request body (the full SMS message).
     * @param auth          Optional auth credentials; `null` or [AuthConfig.None] omits the header.
     * @param sslValidation When `false`, TLS certificate validation is skipped.
     * @return              A [RelayResult] describing the outcome of the request.
     */
    suspend fun post(
        topicUrl: String,
        title: String,
        body: String,
        auth: AuthConfig?,
        sslValidation: Boolean,
    ): RelayResult
}
