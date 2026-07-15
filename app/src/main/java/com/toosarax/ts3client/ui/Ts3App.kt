package com.toosarax.ts3client.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toosarax.ts3client.client.ConnectionStatus

@Composable
fun Ts3App(
    viewModel: ClientViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lastHost by viewModel.lastHost.collectAsState(initial = null)
    val lastPort by viewModel.lastPort.collectAsState(initial = null)
    val lastNickname by viewModel.lastNickname.collectAsState(initial = null)
    val savedServers by viewModel.savedServers.collectAsState()
    val identityPreparing by viewModel.identityPreparing.collectAsState()

    when (state.status) {
        ConnectionStatus.CONNECTED,
        ConnectionStatus.DISCONNECTING,
        -> {
            ChannelScreen(
                state = state,
                onRefresh = viewModel::refreshDirectory,
                onDisconnect = viewModel::disconnect,
                onJoinChannel = viewModel::joinChannel,
                onVoiceModeChanged = viewModel::setVoiceMode,
                onVadThresholdChanged = viewModel::setVadThresholdDbfs,
                onPushToTalkChanged = viewModel::setPushToTalkPressed,
            )
        }
        else -> {
            ConnectScreen(
                state = state,
                identityPreparing = identityPreparing,
                initialHost = lastHost,
                initialPort = lastPort,
                initialNickname = lastNickname,
                savedServers = savedServers,
                onConnect = viewModel::connect,
                onConnectSaved = viewModel::connectSaved,
                onRemoveSavedServer = viewModel::removeSavedServer,
            )
        }
    }
}
