package com.toosarax.ts3client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.toosarax.ts3client.BuildConfig
import com.toosarax.ts3client.R
import com.toosarax.ts3client.client.ClientState
import com.toosarax.ts3client.client.ConnectionStatus
import com.toosarax.ts3client.protocol.ConnectRequest
import com.toosarax.ts3client.settings.SavedServer

@Composable
fun ConnectScreen(
    state: ClientState,
    identityPreparing: Boolean,
    initialHost: String?,
    initialPort: Int?,
    initialNickname: String?,
    savedServers: List<SavedServer>,
    onConnect: (ConnectRequest, Boolean, String) -> Unit,
    onConnectSaved: (SavedServer, String, String?, String?) -> Unit,
    onRemoveSavedServer: (String) -> Unit,
) {
    var host by rememberSaveable { mutableStateOf(initialHost.orEmpty()) }
    var port by rememberSaveable { mutableStateOf((initialPort ?: 9987).toString()) }
    var nickname by rememberSaveable { mutableStateOf(initialNickname.orEmpty()) }
    var serverPassword by rememberSaveable { mutableStateOf("") }
    var serverLabel by rememberSaveable { mutableStateOf(initialHost.orEmpty()) }
    var rememberServer by rememberSaveable { mutableStateOf(true) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var importedIdentity by rememberSaveable { mutableStateOf("") }
    var serverToDelete by rememberSaveable { mutableStateOf<SavedServer?>(null) }
    val connecting = state.status == ConnectionStatus.CONNECTING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.section_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.section_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (identityPreparing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.status_identity_preparing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (savedServers.isNotEmpty()) {
            Text(
                text = stringResource(R.string.section_saved_servers),
                style = MaterialTheme.typography.titleMedium,
            )
            savedServers.forEach { server ->
                SavedServerCard(
                    server = server,
                    enabled = !connecting,
                    onConnect = {
                        if (nickname.isBlank()) {
                            nickname = initialNickname.orEmpty()
                        }
                        onConnectSaved(server, nickname.trim(), serverPassword.ifBlank { null }, importedIdentity.ifBlank { null })
                    },
                    onDelete = { serverToDelete = server },
                )
            }
        }

        OutlinedTextField(
            value = host,
            onValueChange = {
                host = it
                if (serverLabel.isBlank() || serverLabel == host.dropLast(1)) {
                    serverLabel = it.trim()
                }
            },
            label = { Text(stringResource(R.string.label_host)) },
            placeholder = { Text(stringResource(R.string.label_host_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !connecting,
        )
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text(stringResource(R.string.label_nickname)) },
            placeholder = { Text(stringResource(R.string.label_nickname_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !connecting,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.section_identity_help),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.section_identity_steps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                OutlinedTextField(
                    value = importedIdentity,
                    onValueChange = { importedIdentity = it },
                    label = { Text(stringResource(R.string.label_import_identity)) },
                    placeholder = { Text(stringResource(R.string.label_import_identity_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = !connecting,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = rememberServer,
                onCheckedChange = { rememberServer = it },
                enabled = !connecting,
            )
            Text(
                text = stringResource(R.string.label_remember_server),
                modifier = Modifier.weight(1f),
            )
        }

        if (rememberServer) {
            OutlinedTextField(
                value = serverLabel,
                onValueChange = { serverLabel = it },
                label = { Text(stringResource(R.string.label_server_name)) },
                placeholder = { Text(stringResource(R.string.label_server_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !connecting,
            )
        }

        OutlinedButton(
            onClick = { showAdvanced = !showAdvanced },
            modifier = Modifier.fillMaxWidth(),
            enabled = !connecting,
        ) {
            Icon(
                imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
            Text(
                text = stringResource(
                    if (showAdvanced) R.string.action_hide_advanced else R.string.action_show_advanced,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (showAdvanced) {
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text(stringResource(R.string.label_port)) },
                placeholder = { Text(stringResource(R.string.label_port_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !connecting,
            )
            OutlinedTextField(
                value = serverPassword,
                onValueChange = { serverPassword = it },
                label = { Text(stringResource(R.string.label_server_password)) },
                placeholder = { Text(stringResource(R.string.label_server_password_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !connecting,
            )
        }

        state.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = error.userMessage(),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        if (BuildConfig.DEBUG && state.debugLines.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Debug log", style = MaterialTheme.typography.labelLarge)
                    state.debugLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                onConnect(
                    ConnectRequest(
                        host = host.trim(),
                        port = port.toIntOrNull() ?: 9987,
                        nickname = nickname.trim(),
                        serverPassword = serverPassword.ifBlank { null },
                        importedIdentityIni = importedIdentity.ifBlank { null },
                    ),
                    rememberServer,
                    serverLabel,
                )
            },
            enabled = !connecting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(
                if (connecting) {
                    stringResource(R.string.action_connecting)
                } else {
                    stringResource(R.string.action_connect)
                },
            )
        }
    }

    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text(stringResource(R.string.delete_server_title)) },
            text = { Text(stringResource(R.string.delete_server_body, server.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveSavedServer(server.id)
                        serverToDelete = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SavedServerCard(
    server: SavedServer,
    enabled: Boolean,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.History, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = server.displayName, fontWeight = FontWeight.Medium)
                Text(
                    text = "${server.host}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onConnect, enabled = enabled) {
                Text(stringResource(R.string.action_connect))
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}
