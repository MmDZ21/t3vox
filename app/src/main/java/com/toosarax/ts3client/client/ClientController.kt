package com.toosarax.ts3client.client

import com.toosarax.ts3client.protocol.ConnectRequest
import com.toosarax.ts3client.protocol.VoiceMode
import kotlinx.coroutines.flow.StateFlow

interface ClientController {
    val state: StateFlow<ClientState>
    suspend fun connect(request: ConnectRequest): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun refreshDirectory(): Result<Unit>
    suspend fun joinChannel(channelId: Int, password: String? = null): Result<Unit>
    fun setVoiceMode(mode: VoiceMode)
    fun setPushToTalkPressed(pressed: Boolean)
    fun setVadThresholdDbfs(threshold: Float)
}
