package com.toosarax.ts3client.audio.rx

import com.toosarax.ts3client.audio.AudioConstants

object PcmMixer {
    fun mix(streams: Collection<ShortArray>, output: ShortArray, sampleCount: Int) {
        output.fill(0, 0, sampleCount)
        for (stream in streams) {
            val limit = minOf(sampleCount, stream.size)
            for (index in 0 until limit) {
                val mixed = output[index].toInt() + stream[index]
                output[index] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
    }
}
