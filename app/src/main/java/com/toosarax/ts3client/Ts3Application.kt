package com.toosarax.ts3client

import android.app.Application
import com.toosarax.ts3client.protocol.AndroidDnsBootstrap

class Ts3Application : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AndroidDnsBootstrap.initialize(this)
        container = AppContainer(this)
        if (!BuildConfig.DEBUG) {
            container.identityStore.warmUpInBackground()
        }
    }
}
