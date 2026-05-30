package com.example.ntfysms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ntfysms.domain.LogOutcome
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------------
// LogEntryDao — Room DAO for all log_entries table operations
// ---------------------------------------------------------------------------

@Dao
interface LogEntryDao {

    /**
     * Observe all log entries ordered by most-recent first.
     * Returns a [Flow] that emits a new list whenever the table changes.
     */
    @Query("SELECT * FROM log_entries ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<LogEntry>>

    /** Insert a single log entry. */
    @Insert
    suspend fun insert(entry: LogEntry): Long

    /** Update an existing log entry. */
    @Query("UPDATE log_entries SET outcome = :outcome, detail = :detail WHERE id = :id")
    suspend fun updateStatus(id: Long, outcome: LogOutcome, detail: String?)

    /** Delete every row in the table. */
    @Query("DELETE FROM log_entries")
    suspend fun deleteAll()

    /** Return the total number of rows currently in the table. */
    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun count(): Int

    /**
     * Delete the [n] oldest entries (by [LogEntry.timestampMs] ascending),
     * used by capacity-enforcement logic to keep the table at most 200 rows.
     */
    @Query(
        "DELETE FROM log_entries WHERE id IN " +
            "(SELECT id FROM log_entries ORDER BY timestampMs ASC LIMIT :n)",
    )
    suspend fun deleteOldest(n: Int)
}
