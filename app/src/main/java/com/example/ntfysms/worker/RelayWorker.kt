package com.example.ntfysms.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.ntfysms.domain.RelayUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.core.app.NotificationCompat
import com.example.ntfysms.R

@HiltWorker
class RelayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val relayUseCase: RelayUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val logId = inputData.getLong(KEY_LOG_ID, -1L).takeIf { it != -1L }

        val success = relayUseCase.relay(sender, body, logId)

        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // This is only shown on Android versions older than 12 when the work is expedited.
        // On Android 12+, expedited jobs don't necessarily show a notification if they run quickly.
        val notificationId = 1001
        val channelId = "relay_worker_channel" // We should ensure this channel exists in App class

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(applicationContext.getString(R.string.notification_relay_title))
            .setContentText(applicationContext.getString(R.string.notification_relay_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_LOG_ID = "log_id"
    }
}
