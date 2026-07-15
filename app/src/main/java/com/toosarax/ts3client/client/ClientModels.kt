package com.toosarax.ts3client.client

import com.toosarax.ts3client.protocol.ChannelInfo
import com.toosarax.ts3client.protocol.RemoteClientInfo
import com.toosarax.ts3client.protocol.VoiceMode

enum class ConnectionStatus {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  DISCONNECTING,
}

data class ClientState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectedHost: String = "",
    val connectedPort: Int = 9987,
    val currentChannelName: String = "",
    val channels: List<ChannelTreeNode> = emptyList(),
    val clients: List<RemoteClientInfo> = emptyList(),
    val ownClientId: Int = -1,
    val currentChannelId: Int = -1,
    val talkingClientIds: Set<Int> = emptySet(),
    val voiceMode: VoiceMode = VoiceMode.PUSH_TO_TALK,
    val pushToTalkPressed: Boolean = false,
    val vadOpen: Boolean = false,
    val vadThresholdDbfs: Float = -42f,
    val unsupportedCodec: Boolean = false,
    val error: ClientError? = null,
    val debugLines: List<String> = emptyList(),
)

data class ChannelTreeNode(
    val channel: ChannelInfo,
    val children: List<ChannelTreeNode> = emptyList(),
)

data class VadConfig(
    val thresholdDbfs: Float = -42f,
    val hangoverMillis: Long = 350,
)
