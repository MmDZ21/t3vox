package com.toosarax.ts3client.client

import com.toosarax.ts3client.protocol.ChannelInfo

object ChannelTreeBuilder {
    fun build(flatChannels: List<ChannelInfo>): List<ChannelTreeNode> {
        if (flatChannels.isEmpty()) return emptyList()

        val byId = flatChannels.associateBy { it.id }
        val childrenByParent = flatChannels.groupBy { it.parentId }

        fun buildLevel(parentId: Int): List<ChannelTreeNode> {
            val siblings = childrenByParent[parentId].orEmpty()
            val ordered = orderSiblings(siblings, byId)
            return ordered.map { channel ->
                ChannelTreeNode(
                    channel = channel,
                    children = buildLevel(channel.id),
                )
            }
        }

        val rootParentIds = flatChannels.map { it.parentId }.toSet() - flatChannels.map { it.id }.toSet()
        return rootParentIds.flatMap { parentId -> buildLevel(parentId) }
    }

    internal fun orderSiblings(
        siblings: List<ChannelInfo>,
        byId: Map<Int, ChannelInfo>,
    ): List<ChannelInfo> {
        if (siblings.size <= 1) return siblings

        val byPredecessor = siblings.associateBy { it.predecessorId }
        val heads = siblings.filter { predecessor ->
            predecessor.predecessorId == 0 || !byId.containsKey(predecessor.predecessorId)
        }

        val start = heads.minByOrNull { it.id } ?: siblings.minBy { it.id }
        val ordered = mutableListOf<ChannelInfo>()
        val visited = mutableSetOf<Int>()
        var current: ChannelInfo? = start

        while (current != null && current.id !in visited) {
            ordered += current
            visited += current.id
            current = siblings.firstOrNull { it.predecessorId == current.id }
        }

        siblings.filter { it.id !in visited }
            .sortedBy { it.id }
            .forEach { ordered += it }

        return ordered
    }
}
