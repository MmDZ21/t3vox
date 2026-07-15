package com.toosarax.ts3client.protocol

data class ResolvedServerEndpoint(
    val host: String,
    val port: Int,
    val srvResolved: Boolean = false,
)

interface Ts3ServerResolver {
    suspend fun resolve(host: String, requestedPort: Int): ResolvedServerEndpoint
}

const val DEFAULT_TS3_PORT = 9987
