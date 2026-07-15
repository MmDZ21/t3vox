package com.toosarax.ts3client.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.toosarax.ts3client.protocol.VoiceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ts3_settings")

class SettingsStore(private val context: Context) {
    private val voiceModeKey = stringPreferencesKey("voice_mode")
    private val vadThresholdKey = floatPreferencesKey("vad_threshold_dbfs")
    private val lastHostKey = stringPreferencesKey("last_host")
    private val lastPortKey = stringPreferencesKey("last_port")
    private val lastNicknameKey = stringPreferencesKey("last_nickname")
    private val savedServersKey = stringPreferencesKey("saved_servers")

    val voiceMode: Flow<VoiceMode> = context.dataStore.data.map { prefs ->
        when (prefs[voiceModeKey]) {
            VoiceMode.VOICE_ACTIVATION.name -> VoiceMode.VOICE_ACTIVATION
            else -> VoiceMode.PUSH_TO_TALK
        }
    }

    val vadThresholdDbfs: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[vadThresholdKey] ?: -42f
    }

    val lastHost: Flow<String?> = context.dataStore.data.map { it[lastHostKey] }
    val lastPort: Flow<Int?> = context.dataStore.data.map { it[lastPortKey]?.toIntOrNull() }
    val lastNickname: Flow<String?> = context.dataStore.data.map { it[lastNicknameKey] }
    val savedServers: Flow<List<SavedServer>> = context.dataStore.data.map { prefs ->
        SavedServer.decodeAll(prefs[savedServersKey])
    }

    suspend fun setVoiceMode(mode: VoiceMode) {
        context.dataStore.edit { it[voiceModeKey] = mode.name }
    }

    suspend fun setVadThresholdDbfs(threshold: Float) {
        context.dataStore.edit { it[vadThresholdKey] = threshold }
    }

    suspend fun saveConnectionFields(host: String, port: Int, nickname: String) {
        context.dataStore.edit {
            it[lastHostKey] = host
            it[lastPortKey] = port.toString()
            it[lastNicknameKey] = nickname
        }
    }

    suspend fun upsertSavedServer(server: SavedServer) {
        context.dataStore.edit { prefs ->
            val current = SavedServer.decodeAll(prefs[savedServersKey])
            val updated = current
                .filterNot { it.host.equals(server.host, ignoreCase = true) && it.port == server.port }
                .plus(server)
            prefs[savedServersKey] = SavedServer.encodeAll(updated)
        }
    }

    suspend fun removeSavedServer(serverId: String) {
        context.dataStore.edit { prefs ->
            val updated = SavedServer.decodeAll(prefs[savedServersKey]).filterNot { it.id == serverId }
            prefs[savedServersKey] = SavedServer.encodeAll(updated)
        }
    }
}
