package com.toosarax.ts3client.audio.rx

import com.toosarax.ts3client.audio.AudioConstants
import com.toosarax.ts3client.audio.VoiceDecoderFactory
import com.toosarax.ts3client.protocol.EncodedVoicePacket
import com.toosarax.ts3client.protocol.IncomingVoiceSink
import com.toosarax.ts3client.protocol.VoiceCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class IncomingVoiceEngine(
    private val output: AndroidPcmOutput,
    private val decoderFactory: VoiceDecoderFactory = ConcentusVoiceDecoderFactory,
    private val scope: CoroutineScope,
) : IncomingVoiceSink {
    private val ingress = ArrayBlockingQueue<EncodedVoicePacket>(AudioConstants.MAX_INGRESS_PACKETS)
    private val droppedPackets = AtomicInteger(0)
    private val streams = mutableMapOf<Int, SpeakerStream>()
    private var renderJob: Job? = null
    private var ownClientId: Int = -1
    private var currentChannelId: Int = -1
    private val clientChannelLookup = mutableMapOf<Int, Int>()
    var onTalkingChanged: ((Set<Int>) -> Unit)? = null

    override fun tryOffer(packet: EncodedVoicePacket): Boolean {
        if (packet.codec != VoiceCodec.OPUS_VOICE) return false
        if (packet.clientId == ownClientId) return false
        val clientChannel = clientChannelLookup[packet.clientId]
        if (clientChannel == null || clientChannel != currentChannelId) return false
        val accepted = ingress.offer(packet)
        if (!accepted) {
            droppedPackets.incrementAndGet()
        }
        return accepted
    }

    fun start() {
        output.start()
        renderJob = scope.launch(Dispatchers.Default) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val mixBuffer = ShortArray(AudioConstants.FRAME_SAMPLES)
            while (isActive) {
                drainIngress()
                val now = System.currentTimeMillis()
                val activeFrames = streams.values.mapNotNull { it.renderFrame(now) }
                PcmMixer.mix(activeFrames, mixBuffer, AudioConstants.FRAME_SAMPLES)
                output.write(mixBuffer, AudioConstants.FRAME_SAMPLES)
                publishTalkingState()
                delay(AudioConstants.FRAME_MILLIS)
            }
        }
    }

    fun stop() {
        renderJob?.cancel()
        renderJob = null
        ingress.clear()
        streams.values.forEach { it.closeStream() }
        streams.clear()
        output.stop()
    }

    fun reset() {
        ingress.clear()
        streams.values.forEach { it.closeStream() }
        streams.clear()
        publishTalkingState()
    }

    fun updateDirectoryContext(
        ownClientId: Int,
        currentChannelId: Int,
        clientChannels: Map<Int, Int>,
    ) {
        this.ownClientId = ownClientId
        this.currentChannelId = currentChannelId
        clientChannelLookup.clear()
        clientChannelLookup.putAll(clientChannels)
        val stale = streams.keys.filter { clientId ->
            clientChannelLookup[clientId] != currentChannelId
        }
        stale.forEach { clientId ->
            streams.remove(clientId)?.closeStream()
        }
        publishTalkingState()
    }

    fun droppedCount(): Int = droppedPackets.get()

    private fun drainIngress() {
        while (true) {
            val packet = ingress.poll() ?: break
            val stream = streams.getOrPut(packet.clientId) {
                SpeakerStream(packet.clientId, decoderFactory.create())
            }
            stream.offer(packet)
        }
    }

    private fun publishTalkingState() {
        val talking = streams.filterValues { it.isTalking() }.keys
        onTalkingChanged?.invoke(talking)
    }
}
