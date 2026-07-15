package com.toosarax.ts3client.protocol

import com.toosarax.ts3client.protocol.ts3j.Ts3jMicrophone
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ts3jMicrophoneTest {
    @Test
    fun readinessFollowsOutgoingSource() {
        val source = FakeOutgoingSource()
        val microphone = Ts3jMicrophone(source)
        assertFalse(microphone.isReady())
        source.ready = true
        source.packet = byteArrayOf(1, 2, 3)
        assertTrue(microphone.isReady())
        assertArrayEquals(byteArrayOf(1, 2, 3), microphone.provide())
        source.ready = false
        assertArrayEquals(Ts3jMicrophone.EMPTY_BYTES, microphone.provide())
    }

    private class FakeOutgoingSource : OutgoingVoiceSource {
        var ready = false
        var packet: ByteArray? = null
        override fun isReady(): Boolean = ready
        override fun pollPacket(): ByteArray? = packet
    }
}
