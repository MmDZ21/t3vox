package com.toosarax.ts3client.audio.tx

import com.toosarax.ts3client.audio.AudioConstants
import com.toosarax.ts3client.audio.VoiceEncoder
import io.github.jaredmdobson.concentus.OpusApplication
import io.github.jaredmdobson.concentus.OpusEncoder
import io.github.jaredmdobson.concentus.OpusSignal

class ConcentusVoiceEncoder : VoiceEncoder {
    private val encoder = OpusEncoder(
        AudioConstants.SAMPLE_RATE,
        AudioConstants.CHANNEL_COUNT,
        OpusApplication.OPUS_APPLICATION_VOIP,
    ).apply {
        setBitrate(AudioConstants.OPUS_BITRATE)
        setComplexity(AudioConstants.OPUS_COMPLEXITY)
        setUseVBR(true)
        setUseInbandFEC(true)
        setPacketLossPercent(AudioConstants.OPUS_PACKET_LOSS)
        setSignalType(OpusSignal.OPUS_SIGNAL_VOICE)
    }

    override fun encode(pcm: ShortArray, sampleCount: Int, output: ByteArray): Int {
        return encoder.encode(pcm, 0, sampleCount, output, 0, AudioConstants.OPUS_MAX_PACKET_BYTES)
    }
}
