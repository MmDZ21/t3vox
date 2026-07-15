package com.toosarax.ts3client.protocol

import android.content.Context
import org.xbill.DNS.ResolverConfig
import org.xbill.DNS.config.AndroidResolverConfigProvider

object AndroidDnsBootstrap {
    fun initialize(context: Context) {
        val appContext = context.applicationContext
        AndroidResolverConfigProvider.setContext(appContext)
        ResolverConfig.refresh()
    }
}
