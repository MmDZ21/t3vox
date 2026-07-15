package com.toosarax.ts3client.protocol

import kotlinx.coroutines.flow.Flow

interface Ts3Protocol : AutoCloseable {
    val events: Flow<Ts3ProtocolEvent>
    suspend fun connect(request: ConnectRequest)
    suspend fun fetchDirectory(): DirectorySnapshot
    suspend fun joinChannel(channelId: Int, password: String?)
    suspend fun disconnect(reason: String? = null)
}

interface Ts3ProtocolFactory {
    suspend fun create(
        incomingVoiceSink: IncomingVoiceSink,
        outgoingVoiceSource: OutgoingVoiceSource,
    ): Ts3Protocol
}

fun interface IncomingVoiceSink {
    fun tryOffer(packet: EncodedVoicePacket): Boolean
}

interface OutgoingVoiceSource {
    fun isReady(): Boolean
    fun pollPacket(): ByteArray?
}

data class ConnectRequest(
    val host: String,
    val port: Int = 9987,
    val nickname: String,
    val serverPassword: String? = null,
    val importedIdentityIni: String? = null,
)

enum class VoiceMode {
    PUSH_TO_TALK,
    VOICE_ACTIVATION,
}

data class EncodedVoicePacket(
    val sequence: Int,
    val clientId: Int,
    val codec: VoiceCodec,
    val payload: ByteArray,
    val sessionMarker: Byte?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncodedVoicePacket) return false
        return sequence == other.sequence &&
            clientId == other.clientId &&
            codec == other.codec &&
            payload.contentEquals(other.payload) &&
            sessionMarker == other.sessionMarker
    }

    override fun hashCode(): Int {
        var result = sequence
        result = 31 * result + clientId
        result = 31 * result + codec.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (sessionMarker ?: 0)
        return result
    }
}

data class DirectorySnapshot(
    val channels: List<ChannelInfo>,
    val clients: List<RemoteClientInfo>,
    val ownClientId: Int,
    val currentChannelId: Int,
)

sealed interface Ts3ProtocolEvent {
    data class Disconnected(
        val message: String?,
        val cause: Throwable? = null,
    ) : Ts3ProtocolEvent
}

enum class VoiceCodec {
    OPUS_VOICE,
    UNSUPPORTED,
}

data class ChannelInfo(
    val id: Int,
    val parentId: Int,
    val predecessorId: Int,
    val name: String,
    val hasPassword: Boolean,
    val codec: VoiceCodec,
)

data class RemoteClientInfo(
    val id: Int,
    val channelId: Int,
    val nickname: String,
)
