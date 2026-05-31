# ---------------------------------------------------------------------------
# General Android / Hilt / Room Rules
# ---------------------------------------------------------------------------

# Specific components referenced in Manifest to avoid overly broad keep rules
-keep public class com.example.ntfysms.MainActivity
-keep public class com.example.ntfysms.NtfySMS
-keep public class com.example.ntfysms.receiver.SmsReceiver

# Maintain line numbers for easier debugging in release logs
-keepattributes SourceFile,LineNumberTable

# Hilt
-keep class dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Error Prone
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# ---------------------------------------------------------------------------
# Project Specific Protection
# ---------------------------------------------------------------------------

# Keep domain/data models (serialized to/from Database or JSON)
-keep class com.example.ntfysms.data.LogEntry { *; }
-keep class com.example.ntfysms.domain.LogOutcome { *; }

# Keep WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
