package com.example.smsntfyrelay.domain

// ---------------------------------------------------------------------------
// LogOutcome — all possible outcomes of a relay attempt
// ---------------------------------------------------------------------------

enum class LogOutcome {
    SUCCESS,
    HTTP_ERROR,
    TIMEOUT,
    NETWORK_UNAVAILABLE,
    MISSING_CONFIG,
    RELAY_DISABLED,
    PARSE_ERROR,
    SSL_WARNING,  // sent but SSL validation was off
    TEST_CONNECTION,
}

// ---------------------------------------------------------------------------
// AuthType — selector enum used by the Settings UI
// ---------------------------------------------------------------------------

enum class AuthType {
    NONE,
    BEARER,
    BASIC,
}

// ---------------------------------------------------------------------------
// AuthConfig — sealed hierarchy representing the configured auth credentials
// ---------------------------------------------------------------------------

sealed class AuthConfig {
    data object None : AuthConfig()
    data class Bearer(val token: String) : AuthConfig()
    data class Basic(val username: String, val password: String) : AuthConfig()
}

// ---------------------------------------------------------------------------
// RelayResult — outcome returned by NtfyRepository after an HTTP attempt
// ---------------------------------------------------------------------------

sealed class RelayResult {
    data class Success(val httpStatus: Int) : RelayResult()
    data class HttpError(val httpStatus: Int) : RelayResult()
    data object Timeout : RelayResult()
    data object NetworkUnavailable : RelayResult()
    data object MissingConfig : RelayResult()
}
