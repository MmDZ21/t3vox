package com.toosarax.ts3client.audio

import com.toosarax.ts3client.audio.tx.EncodedPacketBuffer
import com.toosarax.ts3client.audio.tx.PcmFrameAssembler
import com.toosarax.ts3client.audio.tx.RmsVad
import com.toosarax.ts3client.audio.rx.PcmMixer
import com.toosarax.ts3client.audio.rx.SpeakerStream
import com.toosarax.ts3client.protocol.EncodedVoicePacket
import com.toosarax.ts3client.protocol.VoiceCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmFrameAssemblerTest {
    @Test
    fun emitsOnlyFullFrames() {
        val assembler = PcmFrameAssembler(frameSize = 4)
        val frames = mutableListOf<ShortArray>()
        assembler.push(shortArrayOf(1, 2, 3), 0, 3) { frames += it }
        assertTrue(frames.isEmpty())
        assembler.push(shortArrayOf(4), 0, 1) { frames += it }
        assertEquals(1, frames.size)
        assertEquals(listOf<Short>(1, 2, 3, 4), frames.first().toList())
    }
}

class EncodedPacketBufferTest {
    @Test
    fun dropsOldestWhenFull() {
        val buffer = EncodedPacketBuffer(capacity = 2)
        buffer.offer(byteArrayOf(1))
        buffer.offer(byteArrayOf(2))
        buffer.offer(byteArrayOf(3))
        assertEquals(2, buffer.size())
        assertEquals(2, buffer.poll()?.single()?.toInt())
        assertEquals(3, buffer.poll()?.single()?.toInt())
    }
}

class RmsVadTest {
    @Test
    fun opensAboveThresholdAndClosesAfterHangover() {
        var now = 0L
        val vad = RmsVad(thresholdDbfs = { -40f }, hangoverMillis = 100, clock = { now })
        val loud = ShortArray(960) { 8_000 }
        val quiet = ShortArray(960) { 0 }

        assertTrue(vad.evaluate(loud, loud.size))
        now = 10
        assertTrue(vad.evaluate(quiet, quiet.size))
        now = 109
        assertTrue(vad.evaluate(quiet, quiet.size))
        now = 111
        assertFalse(vad.evaluate(quiet, quiet.size))
    }
}

class SpeakerStreamTest {
    @Test
    fun detectsLateAndDuplicateSequences() {
        assertTrue(SpeakerStream.isDuplicate(9, 10))
        assertTrue(SpeakerStream.isLate(5, 10))
        assertFalse(SpeakerStream.isLate(11, 10))
    }
}

class PcmMixerTest {
    @Test
    fun clampsMixedOutput() {
        val output = ShortArray(2)
        PcmMixer.mix(
            listOf(shortArrayOf(30000, 30000), shortArrayOf(30000, 30000)),
            output,
            2,
        )
        assertEquals(Short.MAX_VALUE, output[0])
        assertEquals(Short.MAX_VALUE, output[1])
    }
}
