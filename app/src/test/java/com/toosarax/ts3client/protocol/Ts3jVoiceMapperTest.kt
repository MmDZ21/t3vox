package com.toosarax.ts3client.protocol

import com.toosarax.ts3client.protocol.ts3j.Ts3jVoiceMapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ts3jVoiceMapperTest {
    @Test
    fun rejectsUnsupportedCodecPackets() {
        val sink = RecordingSink()
        val packet = FakeVoicePacket(codecIndex = 5, clientId = 2, packetId = 10, payload = byteArrayOf(1))
        assertFalse(Ts3jVoiceMapper.map(packet, sink))
        assertTrue(sink.packets.isEmpty())
    }

    @Test
    fun acceptsOpusVoicePackets() {
        val sink = RecordingSink()
        val packet = FakeVoicePacket(codecIndex = 4, clientId = 2, packetId = 10, payload = byteArrayOf(1, 2))
        assertTrue(Ts3jVoiceMapper.map(packet, sink))
        assertEquals(1, sink.packets.size)
        assertEquals(10, sink.packets.first().sequence)
    }

    private class RecordingSink : IncomingVoiceSink {
        val packets = mutableListOf<EncodedVoicePacket>()
        override fun tryOffer(packet: EncodedVoicePacket): Boolean {
            packets += packet
            return true
        }
    }

    private class FakeVoicePacket(
        codecIndex: Int,
        clientId: Int,
        packetId: Int,
        payload: ByteArray,
    ) : com.github.manevolent.ts3j.protocol.packet.PacketBody0Voice(
        com.github.manevolent.ts3j.protocol.ProtocolRole.SERVER,
    ) {
        init {
            setCodecType(com.github.manevolent.ts3j.enums.CodecType.fromId(codecIndex))
            setCodecData(payload)
            setPacketId(packetId)
            setClientId(clientId)
        }
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
