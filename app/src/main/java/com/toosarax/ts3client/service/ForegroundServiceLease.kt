package com.toosarax.ts3client.service

interface ForegroundServiceLease {
    suspend fun awaitReady(timeoutMillis: Long = 5_000)
    fun stop()
}
