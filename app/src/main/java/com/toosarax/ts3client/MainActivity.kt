package com.toosarax.ts3client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toosarax.ts3client.ui.ClientViewModel
import com.toosarax.ts3client.ui.Ts3App
import com.toosarax.ts3client.ui.theme.Ts3Theme

class MainActivity : ComponentActivity() {
    private var audioPermissionGranted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        audioPermissionGranted = results[Manifest.permission.RECORD_AUDIO] == true
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissionsIfNeeded()
        render()
    }

    private fun requestPermissionsIfNeeded() {
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isEmpty()) {
            audioPermissionGranted = true
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun render() {
        setContent {
            Ts3Theme {
                if (!audioPermissionGranted) {
                    Scaffold { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.permission_title),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = stringResource(R.string.permission_body),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = { requestPermissionsIfNeeded() }) {
                                Text(stringResource(R.string.action_grant_permission))
                            }
                        }
                    }
                } else {
                    val container = (application as Ts3Application).container
                    val factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ClientViewModel(container.clientRuntime, container.settingsStore, container.identityStore) as T
                        }
                    }
                    Ts3App(viewModel = viewModel(factory = factory))
                }
            }
        }
    }
}
