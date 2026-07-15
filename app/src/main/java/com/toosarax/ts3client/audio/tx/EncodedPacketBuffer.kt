package com.toosarax.ts3client.audio.tx

import com.toosarax.ts3client.audio.AudioConstants
import java.util.ArrayDeque

class EncodedPacketBuffer(private val capacity: Int = AudioConstants.ENCODED_PACKET_CAPACITY) {
    private val queue = ArrayDeque<ByteArray>(capacity)

    @Synchronized
    fun offer(packet: ByteArray): Boolean {
        if (queue.size >= capacity) {
            queue.removeFirst()
        }
        queue.addLast(packet)
        return true
    }

    @Synchronized
    fun poll(): ByteArray? = queue.pollFirst()

    @Synchronized
    fun clear() {
        queue.clear()
    }

    @Synchronized
    fun size(): Int = queue.size
}
