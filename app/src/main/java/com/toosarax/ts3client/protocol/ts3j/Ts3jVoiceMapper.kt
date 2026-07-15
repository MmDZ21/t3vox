package com.toosarax.ts3client.protocol.ts3j

import com.github.manevolent.ts3j.enums.CodecType
import com.github.manevolent.ts3j.protocol.packet.PacketBody0Voice
import com.toosarax.ts3client.protocol.EncodedVoicePacket
import com.toosarax.ts3client.protocol.IncomingVoiceSink
import com.toosarax.ts3client.protocol.VoiceCodec

object Ts3jVoiceMapper {
    fun map(packet: PacketBody0Voice, sink: IncomingVoiceSink): Boolean {
        val codec = when (packet.codecType) {
            CodecType.OPUS_VOICE -> VoiceCodec.OPUS_VOICE
            else -> VoiceCodec.UNSUPPORTED
        }
        if (codec != VoiceCodec.OPUS_VOICE) return false

        val payload = packet.codecData ?: ByteArray(0)

        return sink.tryOffer(
            EncodedVoicePacket(
                sequence = packet.packetId,
                clientId = packet.clientId,
                codec = codec,
                payload = payload,
                sessionMarker = packet.serverFlag0,
            ),
        )
    }
}
