package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Binds the answer lines of the conversation screen each player currently sees to a random token. Thread-safe.
 *
 * Only the token of a player's current screen resolves. Showing a new screen or closing the conversation makes every
 * earlier token of that player stale. A current token resolves any number of times.
 *
 * @param T the type of an answer line
 */
class AnswerTokens<T> {
    /**
     * The current screen of a player.
     *
     * @property token the token of the screen
     * @property lines the answer lines of the screen
     */
    private class Screen<T>(val token: UUID, val lines: List<T>)

    /** The current screen of each player. */
    private val screens = ConcurrentHashMap<UUID, Screen<T>>()

    /**
     * Makes [lines] the current screen of [player] under a new token.
     *
     * @return the new token
     */
    fun show(player: UUID, lines: List<T>): UUID {
        val token = UUID.randomUUID()
        screens[player] = Screen(token, lines.toList())
        return token
    }

    /**
     * Returns line [index] of the current screen of [player].
     *
     * @return the line, or `null` if [token] is not the current token of the player or there is no such line
     */
    fun resolve(player: UUID, token: UUID, index: Int): T? {
        val screen = screens[player]?.takeIf { it.token == token } ?: return null
        return screen.lines.getOrNull(index)
    }

    /** Returns whether [token] is the token of the current screen of [player]. */
    fun isCurrent(player: UUID, token: UUID): Boolean = screens[player]?.token == token

    /** Ends the current screen of [player]; every token of the player is stale afterwards. */
    fun close(player: UUID) {
        screens.remove(player)
    }

    /**
     * Ends the current screen of [player] if its token is [token].
     *
     * @return `true` if the screen was ended, `false` if [token] was stale
     */
    fun closeIfCurrent(player: UUID, token: UUID): Boolean {
        var closed = false
        screens.computeIfPresent(player) { _, screen ->
            if (screen.token == token) {
                closed = true
                null
            } else {
                screen
            }
        }
        return closed
    }
}
