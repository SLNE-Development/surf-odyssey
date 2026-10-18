package dev.slne.surf.odyssey.runtime.paper.progression

import org.bukkit.inventory.ItemStack
import java.util.*

/** Turns item stacks into text for transfer and storage, and back. */
interface StackCodec {
    /**
     * Encodes [stack] as text.
     *
     * @param stack the stack to encode
     * @return the encoded stack
     */
    fun encode(stack: ItemStack): String

    /**
     * Decodes a stack encoded by [encode].
     *
     * @param encoded the encoded stack
     * @return the decoded stack
     */
    fun decode(encoded: String): ItemStack
}

/** Encodes item stacks as the Base64 text of [ItemStack.serializeAsBytes]. */
object ItemCodec : StackCodec {
    /** Serializes [stack] with [ItemStack.serializeAsBytes] and encodes the bytes as Base64. */
    override fun encode(stack: ItemStack): String = Base64.getEncoder().encodeToString(stack.serializeAsBytes())

    /** Decodes the Base64 text and deserializes the bytes with [ItemStack.deserializeBytes]. */
    override fun decode(encoded: String): ItemStack = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded))
}
