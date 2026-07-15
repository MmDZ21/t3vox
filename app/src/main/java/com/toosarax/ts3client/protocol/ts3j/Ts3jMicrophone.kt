package com.toosarax.ts3client.protocol.ts3j

import com.github.manevolent.ts3j.audio.Microphone
import com.github.manevolent.ts3j.enums.CodecType
import com.toosarax.ts3client.protocol.OutgoingVoiceSource
import java.util.concurrent.atomic.AtomicBoolean

class Ts3jMicrophone(
    private val source: OutgoingVoiceSource,
) : Microphone {
    private val muted = AtomicBoolean(false)

    override fun isReady(): Boolean = !muted.get() && source.isReady()

    override fun getCodec(): CodecType = CodecType.OPUS_VOICE

    override fun provide(): ByteArray {
        if (!isReady()) return EMPTY_BYTES
        return source.pollPacket() ?: EMPTY_BYTES
    }

    override fun isMuted(): Boolean = muted.get()

    fun setMuted(value: Boolean) {
        muted.set(value)
    }

    companion object {
        val EMPTY_BYTES = ByteArray(0)
    }
}
