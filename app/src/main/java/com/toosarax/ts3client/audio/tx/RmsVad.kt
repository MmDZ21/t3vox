package com.toosarax.ts3client.audio.tx

import com.toosarax.ts3client.audio.AudioConstants
import kotlin.math.log10
import kotlin.math.sqrt

class RmsVad(
    private val thresholdDbfs: () -> Float,
    private val hangoverMillis: Long = 350L,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var open = false
    private var belowSince: Long? = null

    fun evaluate(frame: ShortArray, sampleCount: Int): Boolean {
        val rms = computeRmsDbfs(frame, sampleCount)
        val now = clock()
        if (rms >= thresholdDbfs()) {
            open = true
            belowSince = null
            return true
        }

        if (!open) return false

        if (belowSince == null) {
            belowSince = now
        }

        val elapsed = now - (belowSince ?: now)
        if (elapsed >= hangoverMillis) {
            open = false
            belowSince = null
            return false
        }
        return true
    }

    fun reset() {
        open = false
        belowSince = null
    }

    fun isOpen(): Boolean = open

    companion object {
        fun computeRmsDbfs(frame: ShortArray, sampleCount: Int): Float {
            if (sampleCount <= 0) return -120f
            var sum = 0L
            for (index in 0 until sampleCount) {
                val sample = frame[index].toLong()
                sum += sample * sample
            }
            val mean = sum.toDouble() / sampleCount
            val rms = sqrt(mean)
            if (rms <= 0.0) return -120f
            val normalized = rms / Short.MAX_VALUE
            return (20.0 * log10(normalized)).toFloat()
        }
    }
}
