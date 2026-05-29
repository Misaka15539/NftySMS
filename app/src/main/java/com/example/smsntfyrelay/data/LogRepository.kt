package com.example.smsntfyrelay.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// LogRepository — interface and implementation for log entry persistence
// ---------------------------------------------------------------------------

interface LogRepository {

    /** Observe all log entries ordered by most-recent first. */
    fun observeEntries(): Flow<List<LogEntry>>

    /** Insert a log entry and enforce the 200-row capacity limit. */
    suspend fun insert(entry: LogEntry)

    /** Delete all log entries from the table. */
    suspend fun clearAll()

    /**
     * Enforce a maximum row count by deleting the oldest entries.
     * Defaults to 200 rows; any excess is removed oldest-first.
     */
    suspend fun enforceCapacity(maxEntries: Int = 200)
}

// ---------------------------------------------------------------------------
// LogRepositoryImpl — Room-backed implementation
// ---------------------------------------------------------------------------

@Singleton
class LogRepositoryImpl @Inject constructor(
    private val dao: LogEntryDao,
) : LogRepository {

    override fun observeEntries(): Flow<List<LogEntry>> = dao.observeAll()

    override suspend fun insert(entry: LogEntry) {
        dao.insert(entry)
        enforceCapacity()
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }

    override suspend fun enforceCapacity(maxEntries: Int) {
        val count = dao.count()
        if (count > maxEntries) {
            dao.deleteOldest(count - maxEntries)
        }
    }
}
