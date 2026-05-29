package com.example.smsntfyrelay.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.smsntfyrelay.domain.LogOutcome

// ---------------------------------------------------------------------------
// LogEntry — Room entity persisting each relay attempt to the local database
// ---------------------------------------------------------------------------

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,          // System.currentTimeMillis() at insertion time
    val sender: String,             // phone number
    val messagePreview: String,     // first 60 chars of the message body
    val outcome: LogOutcome,        // relay outcome enum value
    val detail: String? = null,     // HTTP status code, error message, etc.
)

// ---------------------------------------------------------------------------
// LogOutcomeConverter — Room type converter for LogOutcome ↔ String
// ---------------------------------------------------------------------------

@TypeConverters
class LogOutcomeConverter {

    @TypeConverter
    fun fromLogOutcome(outcome: LogOutcome): String = outcome.name

    @TypeConverter
    fun toLogOutcome(value: String): LogOutcome = LogOutcome.valueOf(value)
}
