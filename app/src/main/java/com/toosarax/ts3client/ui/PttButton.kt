package com.toosarax.ts3client.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.toosarax.ts3client.R

@Composable
fun PttButton(
    pressed: Boolean,
    enabled: Boolean,
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = {},
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        onPressedChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onPressedChange(false)
                        }
                    },
                )
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        ),
    ) {
        Text(
            text = stringResource(if (pressed) R.string.ptt_active else R.string.ptt_hold),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
