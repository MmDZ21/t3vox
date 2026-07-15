package com.toosarax.ts3client

import android.content.Context
import com.toosarax.ts3client.client.DefaultClientRuntime
import com.toosarax.ts3client.protocol.DnsJavaTs3ServerResolver
import com.toosarax.ts3client.protocol.ts3j.Ts3IdentityStore
import com.toosarax.ts3client.protocol.ts3j.Ts3jProtocolFactory
import com.toosarax.ts3client.service.ForegroundServiceCoordinator
import com.toosarax.ts3client.settings.SettingsStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsStore = SettingsStore(appContext)
    val identityStore = Ts3IdentityStore(appContext)
    val foregroundCoordinator = ForegroundServiceCoordinator(appContext)
    val serverResolver = DnsJavaTs3ServerResolver()
    val protocolFactory = Ts3jProtocolFactory(identityStore, serverResolver)

    val clientRuntime by lazy {
        DefaultClientRuntime(
            context = appContext,
            settingsStore = settingsStore,
            identityStore = identityStore,
            serverResolver = serverResolver,
            protocolFactory = protocolFactory,
            foregroundCoordinator = foregroundCoordinator,
        )
    }
}
