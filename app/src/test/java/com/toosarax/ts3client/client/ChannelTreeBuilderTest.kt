package com.toosarax.ts3client.client

import com.toosarax.ts3client.protocol.ChannelInfo
import com.toosarax.ts3client.protocol.VoiceCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelTreeBuilderTest {
    @Test
    fun ordersSiblingsByPredecessorChain() {
        val channels = listOf(
            channel(10, 0, 0, "Root A"),
            channel(11, 0, 10, "Root B"),
            channel(12, 0, 11, "Root C"),
        )
        val ordered = ChannelTreeBuilder.orderSiblings(channels, channels.associateBy { it.id })
        assertEquals(listOf(10, 11, 12), ordered.map { it.id })
    }

    @Test
    fun buildsNestedTreeWithMissingPredecessorFallback() {
        val channels = listOf(
            channel(1, 0, 0, "Root"),
            channel(2, 1, 99, "Child"),
        )
        val tree = ChannelTreeBuilder.build(channels)
        assertEquals(1, tree.size)
        assertEquals(1, tree.first().channel.id)
        assertEquals(2, tree.first().children.single().channel.id)
    }

    private fun channel(id: Int, parentId: Int, predecessorId: Int, name: String) = ChannelInfo(
        id = id,
        parentId = parentId,
        predecessorId = predecessorId,
        name = name,
        hasPassword = false,
        codec = VoiceCodec.OPUS_VOICE,
    )
}
