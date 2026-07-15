package com.toosarax.ts3client.protocol

import com.github.manevolent.ts3j.identity.LocalIdentity
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket
import com.toosarax.ts3client.identity.IdentityRecordCodec
import com.toosarax.ts3client.protocol.ts3j.Ts3IdentityStore
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.InetSocketAddress

fun main() {
    val exitCode = LiveConnectionDebug.run()
    kotlin.system.exitProcess(exitCode)
}

object LiveConnectionDebug {
    fun run(): Int {
        val host = System.getenv("TS3_HOST")?.trim()?.takeIf { it.isNotEmpty() } ?: run {
            println("TS3_HOST is required. Run scripts/debug-connect.ps1 -ServerHost \"voice.example.com\".")
            return 2
        }
        val port = System.getenv("TS3_PORT")?.toIntOrNull() ?: DEFAULT_TS3_PORT
        val nickname = System.getenv("TS3_NICK")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Ts3MobileDebug"
        val password = System.getenv("TS3_PASSWORD") ?: ""

        println("=== TS3 live connection debug ===")
        println("Input: $host:$port as '$nickname'")

        val endpoint = runBlocking { DnsJavaTs3ServerResolver().resolve(host, port) }
        println("Resolve: srv=${endpoint.srvResolved} -> ${endpoint.host}:${endpoint.port}")

        val address = InetAddress.getByName(endpoint.host)
        println("DNS: ${endpoint.host} -> ${address.hostAddress}")

        val importedToken = System.getenv("TS3_IDENTITY")?.trim()?.trim('"')
        val identity = if (!importedToken.isNullOrBlank()) {
            println("Using imported identity token (${importedToken.length} chars)")
            val ini = "[Identity]\r\nidentity=\"$importedToken\""
            ini.byteInputStream(Charsets.UTF_8).use { LocalIdentity.read(it) }
        } else {
            val securityLevel = System.getenv("TS3_SECURITY")?.toIntOrNull()
                ?: Ts3IdentityStore.MIN_SECURITY_LEVEL
            println("Generating new identity (security $securityLevel) …")
            LocalIdentity.generateNew(securityLevel)
        }
        println("Identity security level: ${identity.securityLevel}, keyOffset=${identity.keyOffset}")
        println("Identity UID: ${identity.uid.toBase64()}")
        val socket = LocalTeamspeakClientSocket()
        socket.setIdentity(identity)
        socket.setNickname(nickname)
        socket.setHWID(IdentityRecordCodec.generateHwid())

        return try {
            println("Connecting to ${address.hostAddress}:${endpoint.port} …")
            val channelEvents = java.util.concurrent.ConcurrentHashMap<Int, String>()
            val clientEvents = java.util.concurrent.ConcurrentHashMap<Int, String>()
            socket.addListener(object : com.github.manevolent.ts3j.event.TS3Listener {
                override fun onChannelList(e: com.github.manevolent.ts3j.event.ChannelListEvent) {
                    channelEvents[e.channelId] = e.map["channel_name"] ?: "?"
                }
                override fun onClientJoin(e: com.github.manevolent.ts3j.event.ClientJoinEvent) {
                    clientEvents[e.clientId] = e.clientNickname
                }
            })
            socket.connect(InetSocketAddress(address, endpoint.port), password, 15_000L)
            println("SUCCESS: connected, clientId=${socket.clientId}")
            try {
                val channels = socket.listChannels().toList()
                val clients = socket.listClients().toList()
                println("Channels via command (${channels.size}):")
                channels.take(10).forEach { ch -> println("  - ${ch.name} (id=${ch.id})") }
                println("Clients via command (${clients.size}):")
                clients.take(10).forEach { cl -> println("  - ${cl.nickname} (id=${cl.id})") }
            } catch (e: Exception) {
                println("List commands rejected (${e.message}); falling back to pushed events")
                Thread.sleep(2000L)
                println("Channels via events (${channelEvents.size}):")
                channelEvents.entries.take(10).forEach { (id, name) -> println("  - $name (id=$id)") }
                println("Clients via events (${clientEvents.size}):")
                clientEvents.entries.take(10).forEach { (id, nick) -> println("  - $nick (id=$id)") }
            }
            socket.disconnect("debug done")
            println("Disconnected cleanly.")
            0
        } catch (e: Exception) {
            println("FAILED: ${e.javaClass.simpleName}: ${e.message}")
            if (e is ArrayIndexOutOfBoundsException || e.cause is ArrayIndexOutOfBoundsException) {
                println()
                println("Hint: the server likely rejected this identity (ts3j issue #42).")
                println("Try a higher security level, e.g.:")
                println("  Set TS3_SECURITY=40 and run scripts/debug-connect.ps1")
                println("Or clear the app data on your phone so it generates a stronger identity.")
            }
            e.printStackTrace()
            1
        }
    }
}
