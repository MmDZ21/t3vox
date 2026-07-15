package com.toosarax.ts3client.audio.rx

import com.toosarax.ts3client.audio.AudioConstants
import com.toosarax.ts3client.audio.VoiceDecoder
import com.toosarax.ts3client.protocol.EncodedVoicePacket

class SpeakerStream(
    val clientId: Int,
    private val decoder: VoiceDecoder,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val jitter = ArrayDeque<EncodedVoicePacket>(AudioConstants.JITTER_FRAMES)
    private var expectedSequence: Int? = null
    private var lastActivityMs: Long = clock()
    private var sessionMarker: Byte? = null
    private var talking = false
    private val pcmBuffer = ShortArray(AudioConstants.FRAME_SAMPLES)

    fun isTalking(): Boolean = talking

    fun offer(packet: EncodedVoicePacket): Boolean {
        lastActivityMs = clock()
        if (packet.payload.isEmpty()) {
            closeStream()
            return false
        }
        if (sessionMarker != null && packet.sessionMarker != null && packet.sessionMarker != sessionMarker) {
            resetDecoder()
        }
        if (packet.sessionMarker != null) {
            sessionMarker = packet.sessionMarker
        }

        val expected = expectedSequence
        if (expected != null) {
            if (isDuplicate(packet.sequence, expected)) return false
            if (isLate(packet.sequence, expected)) return false
        }

        if (jitter.size >= AudioConstants.JITTER_FRAMES) {
            jitter.removeFirst()
        }
        jitter.addLast(packet)
        return true
    }

    fun renderFrame(nowMs: Long): ShortArray? {
        if (nowMs - lastActivityMs > AudioConstants.STREAM_IDLE_TIMEOUT_MS) {
            closeStream()
            return null
        }

        val packet = jitter.removeFirstOrNull()
        if (packet == null) {
            if (jitter.isEmpty() && expectedSequence == null) return null
            return decodePlc()
        }

        expectedSequence = (packet.sequence + 1) and 0xFFFF
        talking = true
        val decoded = decoder.decode(packet.payload, pcmBuffer)
        return if (decoded > 0) pcmBuffer.copyOf(decoded) else null
    }

    fun closeStream() {
        talking = false
        jitter.clear()
        expectedSequence = null
        sessionMarker = null
    }

    fun resetDecoder() {
        closeStream()
    }

    private fun decodePlc(): ShortArray? {
        val decoded = decoder.decode(null, pcmBuffer)
        return if (decoded > 0) pcmBuffer.copyOf(decoded) else null
    }

    companion object {
        fun isDuplicate(sequence: Int, expected: Int): Boolean {
            return sequence == ((expected - 1) and 0xFFFF)
        }

        fun isLate(sequence: Int, expected: Int): Boolean {
            val diff = (sequence - expected) and 0xFFFF
            return diff > 0x8000
        }

        fun gapFrames(from: Int, to: Int): Int {
            return (to - from) and 0xFFFF
        }
    }
}
