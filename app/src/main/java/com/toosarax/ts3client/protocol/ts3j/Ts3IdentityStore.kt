package com.toosarax.ts3client.protocol.ts3j

import android.content.Context
import com.github.manevolent.ts3j.identity.LocalIdentity
import com.toosarax.ts3client.BuildConfig
import com.toosarax.ts3client.identity.IdentityRecord
import com.toosarax.ts3client.identity.IdentityRecordCodec
import com.toosarax.ts3client.protocol.Ts3DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.math.BigInteger

class Ts3IdentityStore(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val identityFile: File
        get() = File(appContext.noBackupFilesDir, "identity.dat")

    private val preparingFlow = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = preparingFlow.asStateFlow()

    private var cachedBundle: IdentityBundle? = null

    fun warmUpInBackground() {
        scope.launch {
            runCatching { getOrCreate() }
                .onFailure { Ts3DebugLog.e("Identity warm-up failed", it) }
        }
    }

    suspend fun getOrCreate(): IdentityBundle = mutex.withLock {
        cachedBundle?.let { return it }

        preparingFlow.value = true
        try {
            val bundle = loadOrCreateLocked()
            cachedBundle = bundle
            Ts3DebugLog.d(
                "Identity ready: security=${bundle.identity.securityLevel} offset=${bundle.identity.keyOffset}",
            )
            bundle
        } finally {
            preparingFlow.value = false
        }
    }

    suspend fun importFromDesktopIni(iniContent: String): IdentityBundle = mutex.withLock {
        preparingFlow.value = true
        try {
            val bundle = withContext(Dispatchers.Default) {
                val normalized = normalizeIni(iniContent)
                val token = extractIdentityToken(normalized)
                require(Companion.looksLikePrivateIdentityToken(token)) {
                    "public_unique_id"
                }
                val identity = ByteArrayInputStream(normalized.toByteArray(Charsets.UTF_8)).use { input ->
                    LocalIdentity.read(input)
                }
                val hwid = IdentityRecordCodec.generateHwid()
                val record = IdentityRecord(
                    keyOffset = identity.keyOffset,
                    privateKeyHex = identity.privateKey.toString(16),
                    hwid = hwid,
                    importedFromDesktop = true,
                )
                persistRecord(record)
                IdentityBundle(identity = identity, hwid = hwid)
            }
            cachedBundle = bundle
            Ts3DebugLog.d(
                "Imported desktop identity: security=${bundle.identity.securityLevel} offset=${bundle.identity.keyOffset}",
            )
            bundle
        } finally {
            preparingFlow.value = false
        }
    }

    private suspend fun loadOrCreateLocked(): IdentityBundle {
        val file = identityFile
        if (file.exists()) {
            return runCatching {
                val record = IdentityRecordCodec.decode(file.readBytes())
                val identity = LocalIdentity.load(BigInteger(record.privateKeyHex, 16))
                identity.setKeyOffset(record.keyOffset)
                identity.setLastCheckedKeyOffset(record.keyOffset)
                upgradeIfNeeded(identity, record)
            }.getOrElse {
                file.delete()
                createAndPersist()
            }
        }
        return createAndPersist()
    }

    private fun upgradeIfNeeded(identity: LocalIdentity, record: IdentityRecord): IdentityBundle {
        if (record.importedFromDesktop) {
            Ts3DebugLog.d(
                "Using imported desktop identity as-is: security=${identity.securityLevel} offset=${identity.keyOffset}",
            )
            return IdentityBundle(identity = identity, hwid = record.hwid)
        }
        val target = targetSecurityLevel()
        if (identity.securityLevel < target) {
            Ts3DebugLog.d(
                "Upgrading identity ${identity.securityLevel} → $target (offset=${identity.keyOffset})",
            )
            identity.improveSecurity(target)
            val updated = record.copy(keyOffset = identity.keyOffset)
            persistRecord(updated)
        }
        return IdentityBundle(identity = identity, hwid = record.hwid)
    }

    private suspend fun createAndPersist(): IdentityBundle = withContext(Dispatchers.Default) {
        val target = targetSecurityLevel()
        Ts3DebugLog.d("Generating new identity (target security $target) …")
        val started = System.currentTimeMillis()
        val identity = LocalIdentity.generateNew(target)
        Ts3DebugLog.d(
            "Identity generated in ${(System.currentTimeMillis() - started) / 1000}s: " +
                "security=${identity.securityLevel} offset=${identity.keyOffset}",
        )
        val hwid = IdentityRecordCodec.generateHwid()
        val record = IdentityRecord(
            keyOffset = identity.keyOffset,
            privateKeyHex = identity.privateKey.toString(16),
            hwid = hwid,
        )
        identityFile.parentFile?.mkdirs()
        persistRecord(record)
        IdentityBundle(identity = identity, hwid = hwid)
    }

    private fun persistRecord(record: IdentityRecord) {
        val temp = File(identityFile.parentFile, "identity.dat.tmp")
        temp.writeBytes(IdentityRecordCodec.encode(record))
        temp.renameTo(identityFile)
    }

    private fun normalizeIni(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) {
            return collapseIdentityIni(trimmed)
        }

        val identityLine = trimmed.lines().firstOrNull { line ->
            line.trimStart().startsWith("identity=", ignoreCase = true)
        }
        val token = if (identityLine != null) {
            collapseToken(extractIdentityToken("[Identity]\r\n${identityLine.trim()}"))
        } else {
            collapseToken(trimmed)
        }
        return "[Identity]\r\nidentity=\"$token\""
    }

    private fun collapseIdentityIni(ini: String): String {
        val tokenLine = ini.lines().firstOrNull { line ->
            line.trimStart().startsWith("identity=", ignoreCase = true)
        } ?: return ini
        val token = collapseToken(extractIdentityToken("[Identity]\r\n${tokenLine.trim()}"))
        val header = ini.lines().firstOrNull { it.trimStart().startsWith("[") } ?: "[Identity]"
        return "$header\r\nidentity=\"$token\""
    }

    private fun collapseToken(raw: String): String = raw.trim().trim('"').replace("\\s+".toRegex(), "")

    private fun extractIdentityToken(normalizedIni: String): String {
        val line = normalizedIni.lines().firstOrNull { it.trimStart().startsWith("identity=", ignoreCase = true) }
            ?: return ""
        val value = line.substringAfter('=').trim()
        return value.trim('"')
    }

    companion object {
        /** Public servers often reject identities below ~22 (ts3j #53) or ~35 (ts3j #42). */
        fun targetSecurityLevel(): Int = if (BuildConfig.DEBUG) 22 else 35

        const val MIN_SECURITY_LEVEL = 35

        internal fun looksLikePrivateIdentityToken(token: String): Boolean {
            val trimmed = token.trim().trim('"')
            // Exported TS3 identities start with the key offset counter, then "V", then the encoded key.
            return trimmed.matches(Regex("^\\d+V\\S+$"))
        }
    }
}

data class IdentityBundle(
    val identity: LocalIdentity,
    val hwid: String,
)
