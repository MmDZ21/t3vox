package com.toosarax.ts3client.protocol

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsJavaTs3ServerResolverTest {
    @Test
    fun normalizeHostStripsTs3UrlPrefix() {
        assertEquals(
            "voice.example.com",
            DnsJavaTs3ServerResolver.normalizeHost("ts3server://voice.example.com"),
        )
    }

    @Test
    fun normalizeHostStripsPathAndPort() {
        assertEquals(
            "voice.example.com",
            DnsJavaTs3ServerResolver.normalizeHost("voice.example.com:9037/path"),
        )
    }

    @Test
    fun recognizesIpv4Literals() {
        assertTrue(DnsJavaTs3ServerResolver.isIpv4Literal("192.168.0.1"))
        assertFalse(DnsJavaTs3ServerResolver.isIpv4Literal("voice.example.com"))
    }

    @Test
    fun customPortSkipsSrvLookup() = runBlocking {
        var lookupCalled = false
        val resolver = DnsJavaTs3ServerResolver(
            srvLookup = SrvRecordLookup {
                lookupCalled = true
                "should-not-be-used.example.com" to 9988
            },
        )
        val endpoint = resolver.resolve("voice.example.com", 9037)
        assertEquals("voice.example.com", endpoint.host)
        assertEquals(9037, endpoint.port)
        assertFalse(endpoint.srvResolved)
        assertFalse(lookupCalled)
    }

    @Test
    fun defaultPortUsesInjectedSrvResult() = runBlocking {
        var queriedHost: String? = null
        val resolver = DnsJavaTs3ServerResolver(
            srvLookup = SrvRecordLookup { host ->
                queriedHost = host
                "srv.example.com" to 9037
            },
        )
        val endpoint = resolver.resolve("ts3server://voice.example.com", DEFAULT_TS3_PORT)
        assertEquals("voice.example.com", queriedHost)
        assertEquals("srv.example.com", endpoint.host)
        assertEquals(9037, endpoint.port)
        assertTrue(endpoint.srvResolved)
    }

    @Test
    fun missingSrvResultFallsBackToRequestedHost() = runBlocking {
        val resolver = DnsJavaTs3ServerResolver(
            srvLookup = SrvRecordLookup { null },
        )
        val endpoint = resolver.resolve("voice.example.com", DEFAULT_TS3_PORT)
        assertEquals("voice.example.com", endpoint.host)
        assertEquals(DEFAULT_TS3_PORT, endpoint.port)
        assertFalse(endpoint.srvResolved)
    }
}
