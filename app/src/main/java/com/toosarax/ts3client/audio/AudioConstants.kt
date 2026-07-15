package com.toosarax.ts3client.audio

object AudioConstants {
    const val SAMPLE_RATE = 48_000
    const val FRAME_SAMPLES = 960
    const val FRAME_MILLIS = 20L
    const val FRAME_BYTES = FRAME_SAMPLES * 2
    const val CHANNEL_COUNT = 1
    const val MAX_INGRESS_PACKETS = 256
    const val JITTER_FRAMES = 10
    const val INITIAL_PLAYOUT_FRAMES = 3
    const val MAX_PLC_GAP = 3
    const val STREAM_IDLE_TIMEOUT_MS = 500L
    const val ENCODED_PACKET_CAPACITY = 5
    const val OPUS_BITRATE = 24_000
    const val OPUS_COMPLEXITY = 5
    const val OPUS_PACKET_LOSS = 10
    const val OPUS_MAX_PACKET_BYTES = 1275
}
