package dev.slne.surf.odyssey.runtime.paper.progression.menu

import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestProgressView
import dev.slne.surf.odyssey.runtime.paper.progression.quest.TrackerText
import io.github.miniplaceholders.api.Expansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * The MiniPlaceholders tags of the progression engine in the `odyssey` expansion: `<odyssey_title>` is the title
 * the player shows, `<odyssey_quest_tracker>` the name and current objective of the tracked quest. Both are empty
 * when there is none or the player's state is not loaded.
 */
object ProgressionPlaceholders {
    /** The expansion registered by [registerStandalone], or `null` while none is registered. */
    private var standalone: Expansion? = null

    /**
     * Adds the tags `title` and `quest_tracker` to [builder].
     *
     * @param builder the builder of the `odyssey` expansion
     * @return [builder]
     */
    fun tags(builder: Expansion.Builder): Expansion.Builder = builder
        .audiencePlaceholder(Player::class.java, "title") { player, _, _ ->
            Tag.selfClosingInserting(titleOf(player))
        }
        .audiencePlaceholder(Player::class.java, "quest_tracker") { player, _, _ ->
            Tag.selfClosingInserting(trackerOf(player))
        }

    /**
     * Registers an own `odyssey` expansion with the tags of [tags], for servers without another `odyssey` expansion.
     * Does nothing when MiniPlaceholders is not enabled.
     */
    fun registerStandalone() {
        if (standalone != null || !Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")) return
        standalone = tags(Expansion.builder("odyssey")).build().also { it.register() }
    }

    /** Removes the expansion registered by [registerStandalone]. */
    fun unregisterStandalone() {
        standalone?.unregister()
        standalone = null
    }

    /** Returns the display of the title [player] shows, or an empty component. */
    private fun titleOf(player: Player): Component {
        val runtime = Progression.runtimeOrNull ?: return Component.empty()
        val titleId = runtime.store[player.uniqueId]?.state?.activeTitle ?: return Component.empty()
        return runtime.registry.title(titleId)?.display ?: Component.empty()
    }

    /** Returns the tracker line of the quest [player] tracks, or an empty component. */
    private fun trackerOf(player: Player): Component {
        val runtime = Progression.runtimeOrNull ?: return Component.empty()
        val state = runtime.store[player.uniqueId]?.state ?: return Component.empty()
        val quest = state.trackedQuest?.let(runtime.registry::quest) ?: return Component.empty()
        val stored = state.quests.find { it.questId == quest.id }
        val period = QuestProgressView.periodOf(quest, runtime.clock.instant())
        return TrackerText.render(quest, QuestProgressView.compute(quest, stored, state.counters, period))
    }
}
