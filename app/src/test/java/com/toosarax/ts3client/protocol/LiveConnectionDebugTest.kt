package com.toosarax.ts3client.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveConnectionDebugTest {
    @Test
    fun connectsUsingExplicitEnvironmentConfiguration() {
        assertEquals(0, LiveConnectionDebug.run())
    }
}
