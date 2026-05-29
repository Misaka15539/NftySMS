package com.example.smsntfyrelay.domain

import com.example.smsntfyrelay.data.LogEntry
import com.example.smsntfyrelay.data.LogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case exposing log observation and management operations.
 * Thin wrapper over [LogRepository] for use by ViewModels.
 */
class LogUseCase @Inject constructor(
    private val logRepository: LogRepository,
) {

    /** Observe all log entries ordered by most-recent first. */
    fun observeEntries(): Flow<List<LogEntry>> = logRepository.observeEntries()

    /** Delete all log entries. */
    suspend fun clearAll() = logRepository.clearAll()
}
