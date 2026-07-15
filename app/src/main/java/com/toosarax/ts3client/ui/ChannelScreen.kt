package com.toosarax.ts3client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.toosarax.ts3client.R
import com.toosarax.ts3client.client.ChannelTreeNode
import com.toosarax.ts3client.client.ClientState
import com.toosarax.ts3client.protocol.RemoteClientInfo
import com.toosarax.ts3client.protocol.VoiceMode

private enum class InCallTab { Talk, Rooms, People }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    state: ClientState,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onJoinChannel: (Int, String?) -> Unit,
    onVoiceModeChanged: (VoiceMode) -> Unit,
    onVadThresholdChanged: (Float) -> Unit,
    onPushToTalkChanged: (Boolean) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(InCallTab.Talk.ordinal) }
    var passwordChannelId by remember { mutableStateOf<Int?>(null) }
    var channelPassword by remember { mutableStateOf("") }
    val tabs = InCallTab.entries
    val peopleInRoom = state.clients.count { it.channelId == state.currentChannelId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.connectedHost.ifBlank { stringResource(R.string.status_connected) },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.currentChannelName.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.status_in_channel, state.currentChannelName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.action_disconnect),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.unsupportedCodec) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = stringResource(R.string.unsupported_codec),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = error.userMessage(),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                when (tab) {
                                    InCallTab.Talk -> stringResource(R.string.tab_talk)
                                    InCallTab.Rooms -> stringResource(R.string.tab_rooms)
                                    InCallTab.People -> stringResource(R.string.tab_people)
                                },
                            )
                        },
                    )
                }
            }

            when (tabs[selectedTab]) {
                InCallTab.Talk -> TalkTab(
                    state = state,
                    onVoiceModeChanged = onVoiceModeChanged,
                    onVadThresholdChanged = onVadThresholdChanged,
                    onPushToTalkChanged = onPushToTalkChanged,
                )
                InCallTab.Rooms -> RoomsTab(
                    channels = state.channels,
                    currentChannelId = state.currentChannelId,
                    onJoin = { channel ->
                        if (channel.hasPassword) {
                            passwordChannelId = channel.id
                            channelPassword = ""
                        } else {
                            onJoinChannel(channel.id, null)
                        }
                    },
                )
                InCallTab.People -> PeopleTab(
                    clients = state.clients.filter { it.channelId == state.currentChannelId },
                    talkingClientIds = state.talkingClientIds,
                    peopleCountLabel = stringResource(R.string.people_count, peopleInRoom),
                )
            }
        }
    }

    if (passwordChannelId != null) {
        AlertDialog(
            onDismissRequest = { passwordChannelId = null },
            title = { Text(stringResource(R.string.label_channel_password)) },
            text = {
                OutlinedTextField(
                    value = channelPassword,
                    onValueChange = { channelPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinChannel(passwordChannelId!!, channelPassword.ifBlank { null })
                        passwordChannelId = null
                    },
                ) { Text(stringResource(R.string.action_join)) }
            },
            dismissButton = {
                Button(onClick = { passwordChannelId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TalkTab(
    state: ClientState,
    onVoiceModeChanged: (VoiceMode) -> Unit,
    onVadThresholdChanged: (Float) -> Unit,
    onPushToTalkChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.label_voice_mode),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.voiceMode == VoiceMode.PUSH_TO_TALK,
                onClick = { onVoiceModeChanged(VoiceMode.PUSH_TO_TALK) },
                label = { Text(stringResource(R.string.mode_ptt)) },
            )
            FilterChip(
                selected = state.voiceMode == VoiceMode.VOICE_ACTIVATION,
                onClick = { onVoiceModeChanged(VoiceMode.VOICE_ACTIVATION) },
                label = { Text(stringResource(R.string.mode_vad)) },
            )
        }
        Text(
            text = stringResource(
                if (state.voiceMode == VoiceMode.PUSH_TO_TALK) {
                    R.string.mode_ptt_desc
                } else {
                    R.string.mode_vad_desc
                },
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.voiceMode == VoiceMode.PUSH_TO_TALK) {
            PttButton(
                pressed = state.pushToTalkPressed,
                enabled = !state.unsupportedCodec,
                onPressedChange = onPushToTalkChanged,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = stringResource(R.string.label_vad_threshold),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VadSensitivityChip(
                    label = stringResource(R.string.sensitivity_low),
                    selected = state.vadThresholdDbfs <= -48f,
                    onClick = { onVadThresholdChanged(-50f) },
                )
                VadSensitivityChip(
                    label = stringResource(R.string.sensitivity_normal),
                    selected = state.vadThresholdDbfs > -48f && state.vadThresholdDbfs <= -36f,
                    onClick = { onVadThresholdChanged(-42f) },
                )
                VadSensitivityChip(
                    label = stringResource(R.string.sensitivity_high),
                    selected = state.vadThresholdDbfs > -36f,
                    onClick = { onVadThresholdChanged(-32f) },
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = stringResource(
                        if (state.vadOpen) R.string.status_vad_open else R.string.status_vad_closed,
                    ),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun VadSensitivityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun RoomsTab(
    channels: List<ChannelTreeNode>,
    currentChannelId: Int,
    onJoin: (com.toosarax.ts3client.protocol.ChannelInfo) -> Unit,
) {
    if (channels.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.rooms_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        channels.forEach { node ->
            channelItems(node, depth = 0, currentChannelId, onJoin)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.channelItems(
    node: ChannelTreeNode,
    depth: Int,
    currentChannelId: Int,
    onJoin: (com.toosarax.ts3client.protocol.ChannelInfo) -> Unit,
) {
    val channel = node.channel
    val isCurrent = channel.id == currentChannelId
    item(key = "channel-${channel.id}") {
        ChannelRow(
            name = channel.name,
            depth = depth,
            isCurrent = isCurrent,
            locked = channel.hasPassword,
            onClick = { onJoin(channel) },
        )
    }
    node.children.forEach { child ->
        channelItems(child, depth + 1, currentChannelId, onJoin)
    }
}

@Composable
private fun ChannelRow(
    name: String,
    depth: Int,
    isCurrent: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(start = (12 + depth * 16).dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Folder, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal)
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.channel_current),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (locked) {
            Icon(
                Icons.Default.Lock,
                contentDescription = stringResource(R.string.channel_locked),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PeopleTab(
    clients: List<RemoteClientInfo>,
    talkingClientIds: Set<Int>,
    peopleCountLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(peopleCountLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (clients.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.people_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clients, key = { it.id }) { client ->
                    PersonRow(
                        client = client,
                        talking = client.id in talkingClientIds,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonRow(client: RemoteClientInfo, talking: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Person, contentDescription = null)
        Text(client.nickname, modifier = Modifier.weight(1f))
        if (talking) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = stringResource(R.string.user_talking),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
