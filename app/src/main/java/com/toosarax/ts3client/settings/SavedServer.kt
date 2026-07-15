package com.toosarax.ts3client.settings

import java.util.UUID

data class SavedServer(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val host: String,
    val port: Int = 9987,
) {
    fun encode(): String = listOf(id, displayName, host, port.toString()).joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = "\u001F"

        fun decode(line: String): SavedServer? {
            val parts = line.split(SEPARATOR)
            if (parts.size != 4) return null
            val port = parts[3].toIntOrNull() ?: return null
            if (parts[1].isBlank() || parts[2].isBlank() || port !in 1..65535) return null
            return SavedServer(
                id = parts[0].ifBlank { UUID.randomUUID().toString() },
                displayName = parts[1],
                host = parts[2],
                port = port,
            )
        }

        fun encodeAll(servers: List<SavedServer>): String =
            servers.joinToString("\n") { it.encode() }

        fun decodeAll(raw: String?): List<SavedServer> =
            raw.orEmpty()
                .lineSequence()
                .mapNotNull { decode(it) }
                .toList()
    }
}
