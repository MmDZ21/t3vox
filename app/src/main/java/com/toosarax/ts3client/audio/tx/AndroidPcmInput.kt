package com.toosarax.ts3client.audio.tx

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.toosarax.ts3client.audio.AudioConstants
import com.toosarax.ts3client.audio.PcmInput
import com.toosarax.ts3client.client.ClientError

class AndroidPcmInput : PcmInput {
    private var recorder: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start() {
        val minBuffer = AudioRecord.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            throw ClientError.AudioInitialization("AudioRecord min buffer size invalid: $minBuffer")
        }
        val bufferSize = maxOf(minBuffer * 2, AudioConstants.FRAME_BYTES * 4)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            AudioConstants.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw ClientError.AudioInitialization("AudioRecord failed to initialize")
        }
        record.startRecording()
        recorder = record
    }

    override fun read(destination: ShortArray, offset: Int, length: Int): Int {
        val record = recorder ?: return -1
        val read = record.read(destination, offset, length)
        if (read < 0) {
            throw ClientError.AudioInitialization("AudioRecord read error: $read")
        }
        return read
    }

    override fun stop() {
        recorder?.run {
            runCatching { stop() }
            release()
        }
        recorder = null
    }

    override fun close() {
        stop()
    }
}
