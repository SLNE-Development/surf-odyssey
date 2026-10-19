package dev.slne.surf.odyssey.runtime.paper.common.scoreboard

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * The quest block of the scoreboard.
 *
 * @property name the name of the tracked quest
 * @property objective the current objective of the tracked quest, including its counts
 */
data class ScoreboardQuest(val name: Component, val objective: Component)

/** Supplies the scoreboard lines whose data comes from modules that paper-common does not depend on. */
interface ScoreboardProvider {
    /** Returns the title [player] shows, or `null` if there is none or the data is not available. */
    fun titleOf(player: Player): Component?

    /** Returns the quest [player] tracks, or `null` if there is none or the data is not available. */
    fun questOf(player: Player): ScoreboardQuest?
}

/** Holds the [ScoreboardProvider] that the scoreboard reads title and quest from. */
object ScoreboardProviders {
    @Volatile
    private var provider: ScoreboardProvider? = null

    /** The registered provider, or `null` while none is registered. */
    val current: ScoreboardProvider? get() = provider

    /** Makes [provider] the source of the title and quest lines. */
    fun register(provider: ScoreboardProvider) {
        this.provider = provider
    }

    /** Removes the registered provider. */
    fun unregister() {
        provider = null
    }
}
