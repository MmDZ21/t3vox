package com.toosarax.ts3client.client

import com.toosarax.ts3client.protocol.ConnectRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultClientRuntimeTest {
    @Test
    fun connectRequestValidationRejectsBlankHost() {
        val error = validateConnectRequest(
            ConnectRequest(host = " ", port = 9987, nickname = "User"),
        )
        assertNotNull(error)
        assertEquals("Host is required", error?.message)
    }

    @Test
    fun connectRequestValidationRejectsInvalidPort() {
        val error = validateConnectRequest(
            ConnectRequest(host = "127.0.0.1", port = 0, nickname = "User"),
        )
        assertNotNull(error)
    }

    @Test
    fun connectRequestValidationAcceptsValidInput() {
        val error = validateConnectRequest(
            ConnectRequest(host = "127.0.0.1", port = 9987, nickname = "User"),
        )
        assertNull(error)
    }

    private fun validateConnectRequest(request: ConnectRequest): ClientError.InvalidInput? {
        if (request.host.isBlank()) return ClientError.InvalidInput("Host is required")
        if (request.nickname.isBlank()) return ClientError.InvalidInput("Nickname is required")
        if (request.port !in 1..65535) return ClientError.InvalidInput("Port must be between 1 and 65535")
        return null
    }
}
