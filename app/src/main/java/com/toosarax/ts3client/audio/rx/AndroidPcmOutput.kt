package com.toosarax.ts3client.audio.rx

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.toosarax.ts3client.audio.AudioConstants
import com.toosarax.ts3client.audio.PcmOutput
import com.toosarax.ts3client.client.ClientError

class AndroidPcmOutput : PcmOutput {
    private var track: AudioTrack? = null

    override fun start() {
        val minBuffer = AudioTrack.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            throw ClientError.AudioInitialization("AudioTrack min buffer size invalid: $minBuffer")
        }
        val bufferSize = maxOf(minBuffer * 2, AudioConstants.FRAME_BYTES * 4)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AudioConstants.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize)
            .build()
        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            throw ClientError.AudioInitialization("AudioTrack failed to initialize")
        }
        audioTrack.play()
        track = audioTrack
    }

    override fun write(samples: ShortArray, count: Int) {
        val audioTrack = track ?: return
        val written = audioTrack.write(samples, 0, count)
        if (written < 0) {
            throw ClientError.AudioInitialization("AudioTrack write error: $written")
        }
    }

    override fun stop() {
        track?.run {
            runCatching { stop() }
            release()
        }
        track = null
    }

    override fun close() {
        stop()
    }
}
