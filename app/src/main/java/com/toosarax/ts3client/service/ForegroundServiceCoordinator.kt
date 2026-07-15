package com.toosarax.ts3client.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicReference

class ForegroundServiceCoordinator(
    private val context: Context,
) : ForegroundServiceLease {
    private val readiness = AtomicReference<CompletableDeferred<Unit>?>(null)

    override suspend fun awaitReady(timeoutMillis: Long) {
        val deferred = CompletableDeferred<Unit>()
        readiness.set(deferred)
        val intent = Intent(context, VoiceForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)
        withTimeout(timeoutMillis) {
            deferred.await()
        }
    }

    override fun stop() {
        readiness.getAndSet(null)?.cancel()
        context.stopService(Intent(context, VoiceForegroundService::class.java))
    }

    internal fun markReady() {
        readiness.get()?.complete(Unit)
    }

    internal fun clearReady() {
        readiness.set(null)
    }
}
