package com.toosarax.ts3client.identity

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64

data class IdentityRecord(
    val keyOffset: Long,
    val privateKeyHex: String,
    val hwid: String,
    val importedFromDesktop: Boolean = false,
)

object IdentityRecordCodec {
    private const val FORMAT_VERSION = 2
    private const val FORMAT_VERSION_LEGACY = 1

    fun encode(record: IdentityRecord): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            stream.writeInt(FORMAT_VERSION)
            stream.writeLong(record.keyOffset)
            stream.writeUTF(record.privateKeyHex)
            stream.writeUTF(record.hwid)
            stream.writeBoolean(record.importedFromDesktop)
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): IdentityRecord {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        return when (val formatVersion = input.readInt()) {
            FORMAT_VERSION_LEGACY -> IdentityRecord(
                keyOffset = input.readLong(),
                privateKeyHex = input.readUTF(),
                hwid = input.readUTF(),
            )
            FORMAT_VERSION -> IdentityRecord(
                keyOffset = input.readLong(),
                privateKeyHex = input.readUTF(),
                hwid = input.readUTF(),
                importedFromDesktop = input.readBoolean(),
            )
            else -> error("Unsupported identity format version: $formatVersion")
        }
    }

    fun generateHwid(): String {
        val random = ByteArray(16)
        java.security.SecureRandom().nextBytes(random)
        return Base64.getEncoder().encodeToString(random)
    }
}
