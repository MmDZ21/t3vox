package com.toosarax.ts3client.protocol.ts3j

import com.github.manevolent.ts3j.api.Channel
import com.github.manevolent.ts3j.api.Client
import com.github.manevolent.ts3j.api.Codec
import com.github.manevolent.ts3j.command.CommandException
import com.github.manevolent.ts3j.event.ChannelCreateEvent
import com.github.manevolent.ts3j.event.ChannelDeletedEvent
import com.github.manevolent.ts3j.event.ChannelEditedEvent
import com.github.manevolent.ts3j.event.ChannelListEvent
import com.github.manevolent.ts3j.event.ClientJoinEvent
import com.github.manevolent.ts3j.event.ClientLeaveEvent
import com.github.manevolent.ts3j.event.ClientMovedEvent
import com.github.manevolent.ts3j.event.TS3Listener
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket
import com.toosarax.ts3client.protocol.ChannelInfo
import com.toosarax.ts3client.protocol.ConnectRequest
import com.toosarax.ts3client.protocol.DirectorySnapshot
import com.toosarax.ts3client.protocol.IncomingVoiceSink
import com.toosarax.ts3client.protocol.OutgoingVoiceSource
import com.toosarax.ts3client.protocol.RemoteClientInfo
import com.toosarax.ts3client.protocol.Ts3Protocol
import com.toosarax.ts3client.protocol.Ts3ProtocolEvent
import com.toosarax.ts3client.protocol.Ts3ProtocolFactory
import com.toosarax.ts3client.protocol.Ts3ServerResolver
import com.toosarax.ts3client.protocol.Ts3DebugLog
import com.toosarax.ts3client.protocol.VoiceCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class Ts3jProtocolFactory(
    private val identityStore: Ts3IdentityStore,
    private val serverResolver: Ts3ServerResolver,
) : Ts3ProtocolFactory {
    override suspend fun create(
        incomingVoiceSink: IncomingVoiceSink,
        outgoingVoiceSource: OutgoingVoiceSource,
    ): Ts3Protocol {
        val bundle = identityStore.getOrCreate()
        return Ts3jProtocol(
            identityBundle = bundle,
            incomingVoiceSink = incomingVoiceSink,
            outgoingVoiceSource = outgoingVoiceSource,
            serverResolver = serverResolver,
        )
    }
}

class Ts3jProtocol(
    private val identityBundle: IdentityBundle,
    private val incomingVoiceSink: IncomingVoiceSink,
    private val outgoingVoiceSource: OutgoingVoiceSource,
    private val serverResolver: Ts3ServerResolver,
) : Ts3Protocol {
    private val ioDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val socket = LocalTeamspeakClientSocket()
    private val microphone = Ts3jMicrophone(outgoingVoiceSource)
    private val eventsFlow = MutableSharedFlow<Ts3ProtocolEvent>(extraBufferCapacity = 8)

    // Some servers (e.g. TeaSpeak) reject query-style commands such as "channellist" over the
    // full-client connection, so we also build the directory from the events the server pushes.
    private val channelCache = ConcurrentHashMap<Int, ChannelInfo>()
    private val clientCache = ConcurrentHashMap<Int, RemoteClientInfo>()

    override val events = eventsFlow.asSharedFlow()

    private val directoryListener = object : TS3Listener {
        override fun onChannelList(e: ChannelListEvent) {
            val info = Channel(e.map).toChannelInfo()
            channelCache[info.id] = info
        }

        override fun onChannelCreate(e: ChannelCreateEvent) {
            val info = Channel(e.map).toChannelInfo()
            channelCache[info.id] = info
        }

        override fun onChannelEdit(e: ChannelEditedEvent) {
            val existing = channelCache[e.channelId] ?: return
            val updated = Channel(e.map)
            val name = updated.name
            channelCache[e.channelId] = existing.copy(
                name = if (name.isNullOrEmpty()) existing.name else name,
            )
        }

        override fun onChannelDeleted(e: ChannelDeletedEvent) {
            channelCache.remove(e.channelId)
        }

        override fun onClientJoin(e: ClientJoinEvent) {
            clientCache[e.clientId] = RemoteClientInfo(
                id = e.clientId,
                channelId = e.clientTargetId,
                nickname = e.clientNickname.orEmpty(),
            )
        }

        override fun onClientMoved(e: ClientMovedEvent) {
            val existing = clientCache[e.clientId] ?: return
            clientCache[e.clientId] = existing.copy(channelId = e.targetChannelId)
        }

        override fun onClientLeave(e: ClientLeaveEvent) {
            clientCache.remove(e.clientId)
        }
    }

    init {
        socket.addListener(directoryListener)
        socket.setExceptionHandler { throwable ->
            eventsFlow.tryEmit(
                Ts3ProtocolEvent.Disconnected(
                    message = throwable.message,
                    cause = throwable,
                ),
            )
        }
        socket.setVoiceHandler { packet ->
            Ts3jVoiceMapper.map(packet, incomingVoiceSink)
        }
        socket.setMicrophone(microphone)
    }

    override suspend fun connect(request: ConnectRequest) = withContext(ioDispatcher) {
        Ts3DebugLog.d("connect requested host=${request.host} port=${request.port} nick=${request.nickname}")
        socket.setIdentity(identityBundle.identity)
        socket.setNickname(request.nickname)
        socket.setHWID(identityBundle.hwid)

        val endpoint = serverResolver.resolve(request.host, request.port)
        Ts3DebugLog.d(
            "resolved endpoint host=${endpoint.host} port=${endpoint.port} srv=${endpoint.srvResolved}",
        )
        val address = InetAddress.getByName(endpoint.host)
        Ts3DebugLog.d("dns ${endpoint.host} -> ${address.hostAddress}")
        val remote = InetSocketAddress(address, endpoint.port)
        Ts3DebugLog.d("connecting to $remote …")
        channelCache.clear()
        clientCache.clear()
        socket.connect(remote, request.serverPassword ?: "", 30_000L)
        Ts3DebugLog.d("connected clientId=${socket.clientId}")
        runCatching { socket.subscribeAll() }
            .onFailure { Ts3DebugLog.d("subscribeAll not supported: ${it.message}") }
        Unit
    }

    override suspend fun fetchDirectory(): DirectorySnapshot = withContext(ioDispatcher) {
        val commandBased = runCatching {
            val channels = socket.listChannels().map { it.toChannelInfo() }
            val clients = socket.listClients().map { it.toRemoteClientInfo() }
            val self = socket.getClientInfo(socket.clientId)
            DirectorySnapshot(
                channels = channels,
                clients = clients,
                ownClientId = socket.clientId,
                currentChannelId = self.channelId,
            )
        }
        commandBased.getOrElse { error ->
            val commandRejected = generateSequence(error as Throwable?) { it.cause }
                .any { it is CommandException }
            if (!commandRejected) throw error
            Ts3DebugLog.d("list commands unsupported (${error.message}); using pushed events")
            DirectorySnapshot(
                channels = channelCache.values.sortedBy { it.id },
                clients = clientCache.values.sortedBy { it.id },
                ownClientId = socket.clientId,
                currentChannelId = clientCache[socket.clientId]?.channelId ?: 0,
            )
        }
    }

    override suspend fun joinChannel(channelId: Int, password: String?) = withContext(ioDispatcher) {
        socket.joinChannel(channelId, password)
    }

    override suspend fun disconnect(reason: String?) = withContext(ioDispatcher) {
        runCatching {
            if (reason != null) {
                socket.disconnect(reason)
            } else {
                socket.disconnect("Disconnected")
            }
        }
        Unit
    }

    override fun close() {
        scope.cancel()
        runCatching { socket.close() }
    }

    private fun Channel.toChannelInfo(): ChannelInfo = ChannelInfo(
        id = id,
        parentId = parentChannelId,
        predecessorId = order,
        name = name,
        hasPassword = hasPassword(),
        codec = when (codec) {
            Codec.OPUS_VOICE -> VoiceCodec.OPUS_VOICE
            else -> VoiceCodec.UNSUPPORTED
        },
    )

    private fun Client.toRemoteClientInfo(): RemoteClientInfo = RemoteClientInfo(
        id = id,
        channelId = channelId,
        nickname = nickname,
    )
}
