package com.toosarax.ts3client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toosarax.ts3client.audio.AudioConstants
import io.github.jaredmdobson.concentus.OpusApplication
import io.github.jaredmdobson.concentus.OpusDecoder
import io.github.jaredmdobson.concentus.OpusEncoder
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime

@RunWith(AndroidJUnit4::class)
class CodecPerformanceSmokeTest {
    @Test
    fun encodeAndDecodeWithinBudget() {
        val encoder = OpusEncoder(48_000, 1, OpusApplication.OPUS_APPLICATION_VOIP)
        val decoders = List(3) { OpusDecoder(48_000, 1) }
        val pcm = ShortArray(AudioConstants.FRAME_SAMPLES) { index ->
            (kotlin.math.sin(index / 20.0) * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        val encoded = ByteArray(1275)
        val decoded = ShortArray(AudioConstants.FRAME_SAMPLES)

        repeat(100) {
            val size = encoder.encode(pcm, 0, pcm.size, encoded, 0, encoded.size)
            decoders.forEach { decoder ->
                decoder.decode(encoded, 0, size, decoded, 0, decoded.size, false)
            }
        }

        val durations = LongArray(500)
        repeat(500) { iteration ->
            durations[iteration] = measureNanoTime {
                val size = encoder.encode(pcm, 0, pcm.size, encoded, 0, encoded.size)
                decoders.forEach { decoder ->
                    decoder.decode(encoded, 0, size, decoded, 0, decoded.size, false)
                }
            }
        }
        durations.sort()
        val p95Millis = durations[(durations.size * 0.95).toInt()] / 1_000_000.0
        assertTrue("p95 ${p95Millis}ms exceeds 15ms budget", p95Millis < 15.0)
    }
}
