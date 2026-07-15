package com.toosarax.ts3client.protocol.ts3j

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ts3IdentityStoreTest {
    @Test
    fun acceptsExportedIdentityToken() {
        assertTrue(
            Ts3IdentityStore.looksLikePrivateIdentityToken(
                "1408625VIGKvX+examplePrivateKeyMaterial==",
            ),
        )
    }

    @Test
    fun rejectsPublicUniqueIdField() {
        assertFalse(
            Ts3IdentityStore.looksLikePrivateIdentityToken(
                "VIGKvX+68E2HBBWA6HoBNK/C0MwPfUX",
            ),
        )
    }
}
