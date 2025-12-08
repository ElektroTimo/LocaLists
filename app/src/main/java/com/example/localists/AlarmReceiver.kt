package com.example.localists

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "LOCALISTS_CHANNEL_ID"
        const val NOTIFICATION_ID_BASE = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("taskName") ?: "Task Reminder"
        val taskDesc = intent.getStringExtra("taskDesc") ?: "It's time!"
        val taskId = intent.getStringExtra("taskId") ?: "0"

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openTaskId", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // create this tiny 24dp white icon later
            .setContentTitle(taskName)
            .setContentText(taskDesc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + taskId.hashCode(), notification)

        Log.d("AlarmReceiver", "Time-based alarm fired for: $taskName")
    }
}