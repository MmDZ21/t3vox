package com.toosarax.ts3client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.manevolent.ts3j.identity.LocalIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DependencySmokeTest {
    @Test
    fun generatesAndReloadsIdentity() {
        val identity = LocalIdentity.generateNew(8)
        val reloaded = LocalIdentity.load(identity.privateKey)
        reloaded.keyOffset = identity.keyOffset
        reloaded.lastCheckedKeyOffset = identity.keyOffset
        assertEquals(identity.privateKey, reloaded.privateKey)
        assertNotNull(identity.publicKeyString)
    }
}
