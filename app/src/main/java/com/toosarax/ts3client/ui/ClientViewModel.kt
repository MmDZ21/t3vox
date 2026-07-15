package com.toosarax.ts3client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toosarax.ts3client.client.ClientController
import com.toosarax.ts3client.protocol.ConnectRequest
import com.toosarax.ts3client.protocol.VoiceMode
import com.toosarax.ts3client.settings.SavedServer
import com.toosarax.ts3client.settings.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientViewModel(
    private val controller: ClientController,
    private val settingsStore: SettingsStore,
    identityStore: com.toosarax.ts3client.protocol.ts3j.Ts3IdentityStore,
) : ViewModel() {
    val state = controller.state
    val lastHost = settingsStore.lastHost.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val lastPort = settingsStore.lastPort.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val lastNickname = settingsStore.lastNickname.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val savedServers = settingsStore.savedServers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val identityPreparing = identityStore.isPreparing.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun connect(
        request: ConnectRequest,
        rememberServer: Boolean,
        serverDisplayName: String,
    ) {
        viewModelScope.launch {
            val result = controller.connect(request)
            if (result.isSuccess && rememberServer) {
                val label = serverDisplayName.trim().ifBlank { request.host }
                settingsStore.upsertSavedServer(
                    SavedServer(
                        displayName = label,
                        host = request.host,
                        port = request.port,
                    ),
                )
            }
        }
    }

    fun connectSaved(
        server: SavedServer,
        nickname: String,
        serverPassword: String?,
        importedIdentityIni: String? = null,
    ) {
        connect(
            ConnectRequest(
                host = server.host,
                port = server.port,
                nickname = nickname,
                serverPassword = serverPassword,
                importedIdentityIni = importedIdentityIni,
            ),
            rememberServer = false,
            serverDisplayName = server.displayName,
        )
    }

    fun removeSavedServer(serverId: String) {
        viewModelScope.launch { settingsStore.removeSavedServer(serverId) }
    }

    fun disconnect() {
        viewModelScope.launch { controller.disconnect() }
    }

    fun refreshDirectory() {
        viewModelScope.launch { controller.refreshDirectory() }
    }

    fun joinChannel(channelId: Int, password: String?) {
        viewModelScope.launch { controller.joinChannel(channelId, password) }
    }

    fun setVoiceMode(mode: VoiceMode) = controller.setVoiceMode(mode)
    fun setPushToTalkPressed(pressed: Boolean) = controller.setPushToTalkPressed(pressed)
    fun setVadThresholdDbfs(threshold: Float) = controller.setVadThresholdDbfs(threshold)
}
