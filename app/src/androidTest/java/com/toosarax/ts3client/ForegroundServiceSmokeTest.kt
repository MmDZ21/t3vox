package com.toosarax.ts3client

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toosarax.ts3client.service.VoiceForegroundService
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForegroundServiceSmokeTest {
    @Test
    fun startsForegroundServiceIntent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, VoiceForegroundService::class.java)
        context.startForegroundService(intent)
        context.stopService(intent)
    }
}
