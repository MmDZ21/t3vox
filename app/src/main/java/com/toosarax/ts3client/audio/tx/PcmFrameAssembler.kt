package com.toosarax.ts3client.audio.tx

import com.toosarax.ts3client.audio.AudioConstants

class PcmFrameAssembler(private val frameSize: Int = AudioConstants.FRAME_SAMPLES) {
    private val buffer = ShortArray(frameSize)
    private var filled = 0

    fun push(read: ShortArray, offset: Int, count: Int, onFrame: (ShortArray) -> Unit) {
        var index = offset
        var remaining = count
        while (remaining > 0) {
            val toCopy = minOf(remaining, frameSize - filled)
            System.arraycopy(read, index, buffer, filled, toCopy)
            filled += toCopy
            index += toCopy
            remaining -= toCopy
            if (filled == frameSize) {
                onFrame(buffer.copyOf())
                filled = 0
            }
        }
    }

    fun reset() {
        filled = 0
    }
}
