package com.toosarax.ts3client.protocol

import android.util.Log
import com.toosarax.ts3client.BuildConfig

object Ts3DebugLog {
    private const val TAG = "Ts3Client"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            runCatching { Log.d(TAG, message) }
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            runCatching {
                if (throwable != null) {
                    Log.e(TAG, message, throwable)
                } else {
                    Log.e(TAG, message)
                }
            }
        }
    }
}
