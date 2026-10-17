package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The open editor session of each admin. */
object EditorSessions {
    private val sessions = ConcurrentHashMap<UUID, EditorSession>()

    /** Returns the session of [player], or `null`. */
    operator fun get(player: UUID): EditorSession? = sessions[player]

    /** Opens a new session for [player], replacing an old one. */
    fun open(player: UUID, regionId: String): EditorSession = EditorSession(regionId).also { sessions[player] = it }

    /** Closes the session of [player]. */
    fun close(player: UUID) {
        sessions.remove(player)
    }

    /** All open sessions with their owners. */
    fun all(): Map<UUID, EditorSession> = sessions
}
