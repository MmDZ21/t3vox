package com.toosarax.ts3client.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.toosarax.ts3client.MainActivity
import com.toosarax.ts3client.R

object VoiceNotification {
    const val CHANNEL_ID = "ts3_voice_session"
    const val NOTIFICATION_ID = 1001
    const val ACTION_DISCONNECT = "com.toosarax.ts3client.action.DISCONNECT"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        connected: Boolean,
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(
            context.getString(
                if (connected) {
                    R.string.notification_title_connected
                } else {
                    R.string.notification_title_connecting
                },
            ),
        )
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .addAction(
            R.drawable.ic_notification,
            context.getString(R.string.notification_disconnect),
            PendingIntent.getBroadcast(
                context,
                1,
                Intent(ACTION_DISCONNECT).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()
}
