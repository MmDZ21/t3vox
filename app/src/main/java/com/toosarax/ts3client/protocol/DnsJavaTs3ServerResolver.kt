package com.toosarax.ts3client.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xbill.DNS.Lookup
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.Type

internal fun interface SrvRecordLookup {
    fun lookup(host: String): Pair<String, Int>?
}

internal object DnsJavaSrvRecordLookup : SrvRecordLookup {
    override fun lookup(host: String): Pair<String, Int>? {
        if (DnsJavaTs3ServerResolver.isIpv4Literal(host)) return null

        return runCatching {
            val lookup = Lookup("_ts3._udp.$host", Type.SRV)
            val answers = lookup.run()
            if (lookup.result != Lookup.SUCCESSFUL || answers.isNullOrEmpty()) {
                Ts3DebugLog.d(
                    "SRV lookup failed for _ts3._udp." + host +
                        " (result=" + lookup.result +
                        ", answers=" + (answers?.size ?: 0) + ")",
                )
                return null
            }
            val record = answers.firstOrNull { it is SRVRecord } as? SRVRecord
            if (record == null) {
                Ts3DebugLog.d("SRV lookup for _ts3._udp.$host returned no SRV records")
                return null
            }
            val target = record.target.toString(true).removeSuffix(".")
            if (target.isBlank() || record.port !in 1..65535) return null
            Ts3DebugLog.d(
                "SRV lookup _ts3._udp." + host + " -> " + target + ":" + record.port,
            )
            target to record.port
        }.onFailure { error ->
            Ts3DebugLog.d("SRV lookup error for _ts3._udp." + host + ": " + error.message)
        }.getOrNull()
    }
}

class DnsJavaTs3ServerResolver internal constructor(
    private val srvLookup: SrvRecordLookup,
) : Ts3ServerResolver {
    constructor() : this(DnsJavaSrvRecordLookup)

    override suspend fun resolve(host: String, requestedPort: Int): ResolvedServerEndpoint =
        withContext(Dispatchers.IO) {
            val normalizedHost = normalizeHost(host)
            if (requestedPort != DEFAULT_TS3_PORT) {
                return@withContext ResolvedServerEndpoint(
                    host = normalizedHost,
                    port = requestedPort,
                )
            }

            val srv = srvLookup.lookup(normalizedHost)
            if (srv != null) {
                ResolvedServerEndpoint(
                    host = srv.first,
                    port = srv.second,
                    srvResolved = true,
                )
            } else {
                ResolvedServerEndpoint(
                    host = normalizedHost,
                    port = requestedPort,
                )
            }
        }

    companion object {
        fun normalizeHost(raw: String): String {
            var host = raw.trim()
            host = host.removePrefix("ts3server://")
            host = host.removePrefix("ts3://")
            if (host.contains("/")) {
                host = host.substringBefore("/")
            }
            if (host.contains(":") && !isIpv4Literal(host)) {
                host = host.substringBefore(":")
            }
            return host.trim().removeSuffix(".")
        }

        fun isIpv4Literal(host: String): Boolean {
            val parts = host.split(".")
            if (parts.size != 4) return false
            return parts.all { part ->
                part.toIntOrNull()?.let { value -> value in 0..255 } == true
            }
        }
    }
}
