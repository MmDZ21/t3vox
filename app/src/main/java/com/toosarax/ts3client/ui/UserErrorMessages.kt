package com.toosarax.ts3client.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.toosarax.ts3client.R
import com.toosarax.ts3client.client.ClientError

@Composable
fun ClientError.userMessage(): String = when (this) {
    is ClientError.InvalidInput -> when (message) {
        "Host is required" -> stringResource(R.string.error_server_required)
        "Nickname is required" -> stringResource(R.string.error_name_required)
        "Port must be between 1 and 65535" -> stringResource(R.string.error_port_invalid)
        else -> message.orEmpty().ifBlank { stringResource(R.string.error_unexpected) }
    }
    is ClientError.PermissionDenied -> stringResource(R.string.error_permission_denied)
    is ClientError.IdentityFailure -> when (message) {
        ClientError.IDENTITY_REJECTED -> stringResource(R.string.error_identity_rejected)
        ClientError.IDENTITY_REJECTED_IMPORTED -> stringResource(R.string.error_identity_rejected_imported)
        "public_unique_id" -> stringResource(R.string.error_identity_public_id)
        "Could not read identity" -> stringResource(R.string.error_identity_invalid)
        else -> stringResource(R.string.error_identity)
    }
    is ClientError.DnsFailure -> stringResource(R.string.error_dns)
    is ClientError.Timeout -> stringResource(R.string.error_timeout)
    is ClientError.ServerRejection -> stringResource(R.string.error_server_rejected)
    is ClientError.UnsupportedCodec -> stringResource(R.string.unsupported_codec)
    is ClientError.AudioInitialization -> stringResource(R.string.error_audio)
    is ClientError.ConnectionLoss -> stringResource(R.string.error_disconnected)
    is ClientError.Unexpected -> stringResource(R.string.error_unexpected)
}
