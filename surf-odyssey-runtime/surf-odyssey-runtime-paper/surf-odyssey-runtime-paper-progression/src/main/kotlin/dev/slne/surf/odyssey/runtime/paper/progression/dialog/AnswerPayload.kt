package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import net.kyori.adventure.key.Key
import java.util.*

/**
 * Encodes and decodes the SNBT payload of answer clicks in conversation dialogs: the token of the screen and the
 * index of the clicked line, or [CLOSE] for the button that ends the conversation.
 */
object AnswerPayload {
    /** The identifier of the custom click sent by answer lines and the end button. */
    val KEY: Key = Key.key("odyssey", "answer")

    /** The line index sent by the button that ends the conversation. */
    const val CLOSE: Int = -1

    /** Finds the token in a payload, with single or double quotes. */
    private val TOKEN = Regex("""token\s*:\s*["']([0-9a-fA-F-]{36})["']""")

    /** Finds the line index in a payload. */
    private val LINE = Regex("""line\s*:\s*(-?\d+)""")

    /**
     * Returns the payload of a click on line [index] of the screen with [token].
     *
     * @return an SNBT compound with `token` and `line`
     */
    fun encode(token: UUID, index: Int): String = "{token:\"$token\",line:$index}"

    /**
     * Reads token and line index from a payload sent back by the client.
     *
     * @param snbt the SNBT payload, or `null` if the click carried none
     * @return the token and the line index, or `null` if the payload is malformed
     */
    fun decode(snbt: String?): Pair<UUID, Int>? {
        if (snbt == null) return null
        val token = TOKEN.find(snbt)?.groupValues?.get(1) ?: return null
        val line = LINE.find(snbt)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val uuid = try {
            UUID.fromString(token)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return uuid to line
    }
}
