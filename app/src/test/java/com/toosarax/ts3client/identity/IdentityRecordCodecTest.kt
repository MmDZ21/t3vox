package com.toosarax.ts3client.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IdentityRecordCodecTest {
    @Test
    fun roundTripsIdentityRecord() {
        val original = IdentityRecord(
            keyOffset = 8,
            privateKeyHex = "abc123",
            hwid = "dGVzdA==",
            importedFromDesktop = true,
        )
        val decoded = IdentityRecordCodec.decode(IdentityRecordCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun generatesUniqueHwids() {
        val first = IdentityRecordCodec.generateHwid()
        val second = IdentityRecordCodec.generateHwid()
        assertNotEquals(first, second)
    }
}
