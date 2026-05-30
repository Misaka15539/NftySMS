package com.example.ntfysms.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// ---------------------------------------------------------------------------
// AppDatabase — single Room database for the application
//
// exportSchema = true writes the schema JSON to the directory configured via
// the `room.schemaLocation` KSP argument in build.gradle.kts, enabling
// schema-migration history tracking in version control.
// ---------------------------------------------------------------------------

@Database(
    entities = [LogEntry::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LogOutcomeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    /** Provides access to all [LogEntry] table operations. */
    abstract fun logEntryDao(): LogEntryDao
}
