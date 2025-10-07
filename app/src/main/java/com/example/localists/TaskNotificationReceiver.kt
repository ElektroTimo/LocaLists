package com.example.localists

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Safely retrieve extras with null checks
        val taskName = intent.getStringExtra("taskName") ?: "Unnamed Task"
        val taskDesc = intent.getStringExtra("taskDesc") ?: "No description"
        val taskId = intent.getStringExtra("taskId") ?: "0"

        // Build the notification
        val builder = NotificationCompat.Builder(context, "LOCALISTS_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Task Reminder: $taskName")
            .setContentText(taskDesc.takeIf { it.isNotEmpty() } ?: "Time to complete this task!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Send the notification
        try {
            with(NotificationManagerCompat.from(context)) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notify(taskId.hashCode(), builder.build())
                }
            }
        } catch (e: SecurityException) {
            // Log the error if permission is missing
            android.util.Log.e("TaskNotificationReceiver", "Notification permission not granted", e)
        }
    }
}