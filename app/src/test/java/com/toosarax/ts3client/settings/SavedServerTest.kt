package com.toosarax.ts3client.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedServerTest {
    @Test
    fun roundTripsThroughStorageEncoding() {
        val server = SavedServer(
            id = "abc",
            displayName = "Gaming Night",
            host = "192.168.0.5",
            port = 9987,
        )
        val restored = SavedServer.decode(server.encode())
        assertEquals(server, restored)
    }

    @Test
    fun decodeAllIgnoresInvalidLines() {
        val valid = SavedServer(displayName = "Home", host = "10.0.0.2", port = 9987)
        val servers = SavedServer.decodeAll("${valid.encode()}\ninvalid-line")
        assertEquals(1, servers.size)
        assertEquals("Home", servers.first().displayName)
    }
}
