package com.toosarax.ts3client.audio.rx

import com.toosarax.ts3client.audio.AudioConstants
import com.toosarax.ts3client.audio.VoiceDecoder
import com.toosarax.ts3client.audio.VoiceDecoderFactory
import io.github.jaredmdobson.concentus.OpusApplication
import io.github.jaredmdobson.concentus.OpusDecoder
import io.github.jaredmdobson.concentus.OpusEncoder
import io.github.jaredmdobson.concentus.OpusSignal

class ConcentusVoiceDecoder : VoiceDecoder {
    private val decoder = OpusDecoder(
        AudioConstants.SAMPLE_RATE,
        AudioConstants.CHANNEL_COUNT,
    )

    override fun decode(packet: ByteArray?, output: ShortArray): Int {
        return decoder.decode(packet, 0, packet?.size ?: 0, output, 0, AudioConstants.FRAME_SAMPLES, false)
    }
}

object ConcentusVoiceDecoderFactory : VoiceDecoderFactory {
    override fun create(): VoiceDecoder = ConcentusVoiceDecoder()
}
