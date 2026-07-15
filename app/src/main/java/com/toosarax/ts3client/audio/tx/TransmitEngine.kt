package com.toosarax.ts3client.audio.tx

import com.toosarax.ts3client.audio.AudioConstants
import com.toosarax.ts3client.audio.PcmInput
import com.toosarax.ts3client.protocol.OutgoingVoiceSource
import com.toosarax.ts3client.protocol.VoiceMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class TransmitEngine(
    private val pcmInput: PcmInput,
    private val encoder: ConcentusVoiceEncoder = ConcentusVoiceEncoder(),
    private val packetBuffer: EncodedPacketBuffer = EncodedPacketBuffer(),
    private val scope: CoroutineScope,
) : OutgoingVoiceSource {
    private val transmitGate = AtomicBoolean(false)
    private val assembler = PcmFrameAssembler()
    private var captureJob: Job? = null
    private var voiceMode: VoiceMode = VoiceMode.PUSH_TO_TALK
    private var pushToTalkPressed = false
    private var vadThresholdDbfs: () -> Float = { -42f }
    private val vad = RmsVad(thresholdDbfs = { vadThresholdDbfs() })
  private val readBuffer = ShortArray(AudioConstants.FRAME_SAMPLES)
    private val encodeBuffer = ByteArray(AudioConstants.OPUS_MAX_PACKET_BYTES)
    var onVadOpenChanged: ((Boolean) -> Unit)? = null

    fun start() {
        pcmInput.start()
        captureJob = scope.launch(Dispatchers.IO) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            while (isActive) {
                val read = pcmInput.read(readBuffer, 0, readBuffer.size)
                if (read <= 0) continue
                assembler.push(readBuffer, 0, read) { frame ->
                    handleFrame(frame)
                }
            }
        }
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        closeTransmitGate()
        assembler.reset()
        pcmInput.stop()
    }

    fun setVoiceMode(mode: VoiceMode) {
        if (voiceMode == mode) return
        closeTransmitGate()
        packetBuffer.clear()
        vad.reset()
        onVadOpenChanged?.invoke(false)
        voiceMode = mode
    }

    fun setPushToTalkPressed(pressed: Boolean) {
        pushToTalkPressed = pressed
        updateGate()
    }

    fun setVadThresholdDbfs(threshold: Float) {
        vadThresholdDbfs = { threshold }
    }

    fun closeTransmitGate() {
        transmitGate.set(false)
        packetBuffer.clear()
        vad.reset()
        onVadOpenChanged?.invoke(false)
    }

    override fun isReady(): Boolean = transmitGate.get()

    override fun pollPacket(): ByteArray? = packetBuffer.poll()

    private fun handleFrame(frame: ShortArray) {
        val shouldTransmit = when (voiceMode) {
            VoiceMode.PUSH_TO_TALK -> pushToTalkPressed
            VoiceMode.VOICE_ACTIVATION -> vad.evaluate(frame, AudioConstants.FRAME_SAMPLES)
        }
        onVadOpenChanged?.invoke(vad.isOpen())
        updateGate(shouldTransmit)
        if (!shouldTransmit) return

        val encodedSize = encoder.encode(frame, AudioConstants.FRAME_SAMPLES, encodeBuffer)
        if (encodedSize > 0) {
            packetBuffer.offer(encodeBuffer.copyOf(encodedSize))
        }
    }

    private fun updateGate(shouldTransmit: Boolean = computeShouldTransmit()) {
        transmitGate.set(shouldTransmit)
        if (!shouldTransmit) {
            packetBuffer.clear()
        }
    }

    private fun computeShouldTransmit(): Boolean = when (voiceMode) {
        VoiceMode.PUSH_TO_TALK -> pushToTalkPressed
        VoiceMode.VOICE_ACTIVATION -> vad.isOpen()
    }
}
