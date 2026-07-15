package com.toosarax.ts3client.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.toosarax.ts3client.Ts3Application

class VoiceForegroundService : Service() {
    private var disconnectReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        VoiceNotification.ensureChannel(this)
        val flags = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        ServiceCompat.startForeground(
            this,
            VoiceNotification.NOTIFICATION_ID,
            VoiceNotification.build(this, connected = false),
            flags,
        )
        val app = application as Ts3Application
        app.container.foregroundCoordinator.markReady()
        disconnectReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val runtime = (application as Ts3Application).container.clientRuntime
                runtime.requestDisconnectFromService()
            }
        }
        ContextCompat.registerReceiver(
            this,
            disconnectReceiver,
            IntentFilter(VoiceNotification.ACTION_DISCONNECT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        disconnectReceiver?.let { unregisterReceiver(it) }
        disconnectReceiver = null
        val app = application as Ts3Application
        app.container.foregroundCoordinator.clearReady()
        app.container.clientRuntime.requestDisconnectFromService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun updateConnected(connected: Boolean) {
        val flags = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        ServiceCompat.startForeground(
            this,
            VoiceNotification.NOTIFICATION_ID,
            VoiceNotification.build(this, connected),
            flags,
        )
    }
}
