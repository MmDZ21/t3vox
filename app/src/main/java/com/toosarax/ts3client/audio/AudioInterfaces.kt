package com.toosarax.ts3client.audio

interface PcmInput : AutoCloseable {
    fun start()
    fun read(destination: ShortArray, offset: Int, length: Int): Int
    fun stop()
}

interface PcmOutput : AutoCloseable {
    fun start()
    fun write(samples: ShortArray, count: Int)
    fun stop()
}

interface VoiceEncoder {
    fun encode(pcm: ShortArray, sampleCount: Int, output: ByteArray): Int
}

interface VoiceDecoder {
    fun decode(packet: ByteArray?, output: ShortArray): Int
}

fun interface VoiceDecoderFactory {
    fun create(): VoiceDecoder
}
