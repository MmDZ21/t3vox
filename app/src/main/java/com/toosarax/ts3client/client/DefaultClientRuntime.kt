package com.toosarax.ts3client.client

import android.content.Context
import android.os.PowerManager
import com.toosarax.ts3client.audio.AndroidAudioSession
import com.toosarax.ts3client.audio.tx.AndroidPcmInput
import com.toosarax.ts3client.audio.rx.AndroidPcmOutput
import com.toosarax.ts3client.audio.rx.IncomingVoiceEngine
import com.toosarax.ts3client.audio.tx.TransmitEngine
import com.toosarax.ts3client.protocol.ConnectRequest
import com.toosarax.ts3client.protocol.DirectorySnapshot
import com.toosarax.ts3client.protocol.Ts3DebugLog
import com.toosarax.ts3client.protocol.Ts3ServerResolver
import com.toosarax.ts3client.protocol.Ts3Protocol
import com.toosarax.ts3client.protocol.Ts3ProtocolEvent
import com.toosarax.ts3client.protocol.Ts3ProtocolFactory
import com.toosarax.ts3client.protocol.VoiceCodec
import com.toosarax.ts3client.protocol.VoiceMode
import com.toosarax.ts3client.service.ForegroundServiceCoordinator
import com.toosarax.ts3client.protocol.ts3j.Ts3IdentityStore
import com.toosarax.ts3client.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultClientRuntime(
    context: Context,
    private val settingsStore: SettingsStore,
    private val identityStore: Ts3IdentityStore,
    private val serverResolver: Ts3ServerResolver,
    private val protocolFactory: Ts3ProtocolFactory,
    private val foregroundCoordinator: ForegroundServiceCoordinator,
) : ClientController {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commandMutex = Mutex()
    private val audioSession = AndroidAudioSession(appContext)
    private val incomingEngine = IncomingVoiceEngine(AndroidPcmOutput(), scope = scope)
    private val transmitEngine = TransmitEngine(AndroidPcmInput(), scope = scope)
    private val stateFlow = MutableStateFlow(ClientState())
    private var protocol: Ts3Protocol? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var eventJob: kotlinx.coroutines.Job? = null

    override val state: StateFlow<ClientState> = stateFlow.asStateFlow()

    init {
        scope.launch {
            settingsStore.voiceMode.collect { mode ->
                stateFlow.update { it.copy(voiceMode = mode) }
                transmitEngine.setVoiceMode(mode)
            }
        }
        scope.launch {
            settingsStore.vadThresholdDbfs.collect { threshold ->
                stateFlow.update { it.copy(vadThresholdDbfs = threshold) }
                transmitEngine.setVadThresholdDbfs(threshold)
            }
        }
        incomingEngine.onTalkingChanged = { talking ->
            stateFlow.update { it.copy(talkingClientIds = talking) }
        }
        transmitEngine.onVadOpenChanged = { open ->
            stateFlow.update { it.copy(vadOpen = open) }
        }
        audioSession.onFocusLoss = {
            scope.launch { disconnectInternal(preserveError = false) }
        }
    }

    override suspend fun connect(request: ConnectRequest): Result<Unit> = commandMutex.withLock {
        if (stateFlow.value.status == ConnectionStatus.CONNECTED ||
            stateFlow.value.status == ConnectionStatus.CONNECTING
        ) {
            return Result.failure(IllegalStateException("Already connected or connecting"))
        }
        val validation = validateRequest(request)
        if (validation != null) {
            publishError(validation)
            return Result.failure(IllegalStateException(validation.message))
        }

        stateFlow.update {
            it.copy(
                status = ConnectionStatus.CONNECTING,
                error = null,
                unsupportedCodec = false,
                debugLines = listOf("Starting connection to ${request.host}:${request.port} …"),
            )
        }

        return runCatching {
            val endpoint = serverResolver.resolve(request.host, request.port)
            appendDebug(
                if (endpoint.srvResolved) {
                    "SRV found → ${endpoint.host}:${endpoint.port}"
                } else {
                    "No SRV record, using ${endpoint.host}:${endpoint.port}"
                },
            )
            appendDebug("Starting voice service …")
            foregroundCoordinator.awaitReady(timeoutMillis = 30_000L)
            appendDebug("Voice service ready")
            audioSession.acquire()
            appendDebug("Audio session acquired")
            acquireWakeLock()
            if (!request.importedIdentityIni.isNullOrBlank()) {
                appendDebug("Importing desktop identity …")
                val bundle = runCatching { identityStore.importFromDesktopIni(request.importedIdentityIni) }
                    .getOrElse { error ->
                        throw if (error.message == "public_unique_id") {
                            ClientError.IdentityFailure("public_unique_id", error)
                        } else {
                            ClientError.IdentityFailure("Could not read identity", error)
                        }
                    }
                appendDebug(
                    "Desktop identity imported (security=${bundle.identity.securityLevel}, offset=${bundle.identity.keyOffset})",
                )
            }
            appendDebug("Preparing identity …")
            val createdProtocol = protocolFactory.create(
                incomingVoiceSink = incomingEngine,
                outgoingVoiceSource = transmitEngine,
            )
            protocol = createdProtocol
            observeProtocolEvents(createdProtocol)
            appendDebug("Identity ready — connecting to TeamSpeak …")
            createdProtocol.connect(request)
            appendDebug("Connected, loading channels …")
            val snapshot = createdProtocol.fetchDirectory()
            applySnapshot(snapshot)
            appendDebug("Loaded ${snapshot.channels.size} rooms, ${snapshot.clients.size} users")
            settingsStore.saveConnectionFields(request.host, request.port, request.nickname)
            incomingEngine.start()
            transmitEngine.start()
            stateFlow.update {
                it.copy(
                    status = ConnectionStatus.CONNECTED,
                    error = null,
                    connectedHost = request.host,
                    connectedPort = request.port,
                )
            }
        }.onFailure { throwable ->
            val error = when {
                !request.importedIdentityIni.isNullOrBlank() &&
                    ClientError.looksLikeIdentityRejection(throwable) ->
                    ClientError.IdentityFailure(ClientError.IDENTITY_REJECTED_IMPORTED, throwable)
                request.importedIdentityIni.isNullOrBlank() &&
                    ClientError.looksLikeIdentityRejection(throwable) ->
                    ClientError.IdentityFailure(ClientError.IDENTITY_REJECTED, throwable)
                throwable is ClientError -> throwable
                else -> ClientError.fromThrowable(throwable)
            }
            appendDebug("Failed: ${error.message ?: throwable.javaClass.simpleName}")
            publishError(error)
            teardownAudioAndProtocol()
            foregroundCoordinator.stop()
        }
    }

    override suspend fun disconnect(): Result<Unit> = commandMutex.withLock {
        disconnectInternal(preserveError = false)
        Result.success(Unit)
    }

    override suspend fun refreshDirectory(): Result<Unit> = commandMutex.withLock {
        val activeProtocol = protocol ?: return Result.failure(IllegalStateException("Not connected"))
        return runCatching {
            val snapshot = activeProtocol.fetchDirectory()
            applySnapshot(snapshot)
        }.onFailure { throwable ->
            publishError(ClientError.fromThrowable(throwable))
        }
    }

    override suspend fun joinChannel(channelId: Int, password: String?): Result<Unit> = commandMutex.withLock {
        val activeProtocol = protocol ?: return Result.failure(IllegalStateException("Not connected"))
        return runCatching {
            activeProtocol.joinChannel(channelId, password)
            incomingEngine.reset()
            val snapshot = activeProtocol.fetchDirectory()
            applySnapshot(snapshot)
        }.onFailure { throwable ->
            publishError(ClientError.fromThrowable(throwable))
        }
    }

    override fun setVoiceMode(mode: VoiceMode) {
        scope.launch {
            settingsStore.setVoiceMode(mode)
            transmitEngine.setVoiceMode(mode)
            stateFlow.update { it.copy(voiceMode = mode, pushToTalkPressed = false) }
        }
    }

    override fun setPushToTalkPressed(pressed: Boolean) {
        transmitEngine.setPushToTalkPressed(pressed)
        stateFlow.update { it.copy(pushToTalkPressed = pressed) }
    }

    override fun setVadThresholdDbfs(threshold: Float) {
        scope.launch {
            settingsStore.setVadThresholdDbfs(threshold)
            transmitEngine.setVadThresholdDbfs(threshold)
            stateFlow.update { it.copy(vadThresholdDbfs = threshold) }
        }
    }

    fun requestDisconnectFromService() {
        scope.launch {
            commandMutex.withLock {
                disconnectInternal(preserveError = false)
            }
        }
    }

    private suspend fun disconnectInternal(preserveError: Boolean) {
        if (stateFlow.value.status == ConnectionStatus.DISCONNECTED ||
            stateFlow.value.status == ConnectionStatus.DISCONNECTING
        ) {
            return
        }
        stateFlow.update { it.copy(status = ConnectionStatus.DISCONNECTING) }
        teardownAudioAndProtocol()
        foregroundCoordinator.stop()
        stateFlow.update {
            it.copy(
                status = ConnectionStatus.DISCONNECTED,
                talkingClientIds = emptySet(),
                pushToTalkPressed = false,
                vadOpen = false,
                connectedHost = "",
                connectedPort = 9987,
                currentChannelName = "",
                channels = emptyList(),
                clients = emptyList(),
                debugLines = emptyList(),
                error = if (preserveError) it.error else null,
            )
        }
    }

    private suspend fun teardownAudioAndProtocol() {
        transmitEngine.closeTransmitGate()
        transmitEngine.stop()
        incomingEngine.stop()
        audioSession.release()
        releaseWakeLock()
        eventJob?.cancel()
        eventJob = null
        protocol?.let { activeProtocol ->
            runCatching { activeProtocol.disconnect() }
            runCatching { activeProtocol.close() }
        }
        protocol = null
    }

    private fun applySnapshot(snapshot: DirectorySnapshot) {
        val tree = ChannelTreeBuilder.build(snapshot.channels)
        val currentChannel = snapshot.channels.firstOrNull { it.id == snapshot.currentChannelId }
        val unsupported = currentChannel?.codec != VoiceCodec.OPUS_VOICE
        incomingEngine.updateDirectoryContext(
            ownClientId = snapshot.ownClientId,
            currentChannelId = snapshot.currentChannelId,
            clientChannels = snapshot.clients.associate { it.id to it.channelId },
        )
        stateFlow.update {
            it.copy(
                channels = tree,
                clients = snapshot.clients,
                ownClientId = snapshot.ownClientId,
                currentChannelId = snapshot.currentChannelId,
                currentChannelName = currentChannel?.name.orEmpty(),
                unsupportedCodec = unsupported,
            )
        }
        if (unsupported) {
            transmitEngine.closeTransmitGate()
            incomingEngine.reset()
        }
    }

    private fun observeProtocolEvents(activeProtocol: Ts3Protocol) {
        eventJob?.cancel()
        eventJob = scope.launch {
            activeProtocol.events.collect { event ->
                if (event is Ts3ProtocolEvent.Disconnected) {
                    commandMutex.withLock {
                        publishError(ClientError.ConnectionLoss(event.message ?: "Disconnected", event.cause))
                        disconnectInternal(preserveError = true)
                    }
                }
            }
        }
    }

    private fun validateRequest(request: ConnectRequest): ClientError.InvalidInput? {
        if (request.host.isBlank()) return ClientError.InvalidInput("Host is required")
        if (request.nickname.isBlank()) return ClientError.InvalidInput("Nickname is required")
        if (request.port !in 1..65535) return ClientError.InvalidInput("Port must be between 1 and 65535")
        return null
    }

    private fun publishError(error: ClientError) {
        stateFlow.update { it.copy(error = error, status = ConnectionStatus.DISCONNECTED) }
    }

    private fun appendDebug(line: String) {
        Ts3DebugLog.d(line)
        stateFlow.update { it.copy(debugLines = it.debugLines + line) }
    }

    private fun acquireWakeLock() {
        val manager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ts3client:voice").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        wakeLock = null
    }
}
