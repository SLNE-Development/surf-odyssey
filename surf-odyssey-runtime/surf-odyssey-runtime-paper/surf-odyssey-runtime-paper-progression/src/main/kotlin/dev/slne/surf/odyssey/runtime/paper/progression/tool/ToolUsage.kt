package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curves
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** Computes levels of tools from their usage. */
object ToolProgress {
    /**
     * Returns the level of [tool] at [usage], between 1 and the maximum level of the tool.
     *
     * @param tool the tool definition
     * @param usage the total usage of the tool
     */
    fun levelFor(tool: ToolDefinition, usage: Long): Int = Curves.levelFor(tool.curve, usage, tool.maxLevel)

    /**
     * Returns whether adding [added] usage to [before] raises the level of [tool].
     *
     * @param tool the tool definition
     * @param before the total usage before
     * @param added the added usage
     */
    fun isLevelUp(tool: ToolDefinition, before: Long, added: Long): Boolean =
        levelFor(tool, before + added) > levelFor(tool, before)

    /**
     * Returns the total usage [tool] needs for the level after [level], or `null` if [level] is the maximum level.
     *
     * @param tool the tool definition
     * @param level the current level
     */
    fun usageForNextLevel(tool: ToolDefinition, level: Int): Long? =
        if (level >= tool.maxLevel) null else tool.curve.usageFor(level + 1)
}

/** Adds usage to the tools players hold and applies level-ups. */
object ToolUsage {
    /** How long, in milliseconds, the lore of one tool item is not rendered again after it was rendered. */
    private const val LORE_INTERVAL_MILLIS = 1_000L

    /** The time the lore of each tool item was last rendered, by the unique ID of the item. */
    private val loreRenderedAt = ConcurrentHashMap<UUID, Long>()

    /**
     * Adds [amount] usage to the tool in the main hand of [player] and records [detail] in its chronicle.
     *
     * Nothing happens unless the engine runs in [ProgressionMode.FULL] and the main hand holds a tool of the installed
     * content whose type is [type]. The bonuses of the tool are applied again after every change, and its lore is
     * rendered at most once per second, but always when the tool reaches a new level. A new level is announced to
     * the player with a message and a sound.
     *
     * Must run on the thread of [player].
     *
     * @param player the player who used the tool
     * @param type the kind of tool the action needs
     * @param amount the usage the action is worth; negative amounts count as 0
     * @param detail what the chronicle records about the action, or `null`
     */
    fun record(player: Player, type: ToolType, amount: Int, detail: ChronicleEvent? = null) {
        if (!Progression.isInstalled || Progression.runtime.mode != ProgressionMode.FULL) return
        val item = player.inventory.itemInMainHand
        val state = ToolItems.read(item) ?: return
        val tool = ToolItems.definitionOf(state) ?: return
        if (tool.type != type) return
        val added = amount.coerceAtLeast(0).toLong()
        if (added == 0L && detail == null) return

        val leveledUp = ToolProgress.isLevelUp(tool, state.usage, added)
        val updated = state.copy(
            usage = state.usage + added,
            chronicle = detail?.let(state.chronicle::record) ?: state.chronicle,
        )
        ToolItems.write(item, updated)
        ToolItems.refresh(item, tool, updated, lore = loreDue(updated.uuid, forced = leveledUp))
        player.inventory.setItemInMainHand(item)

        if (leveledUp) announceLevel(player, tool, ToolProgress.levelFor(tool, updated.usage))
    }

    /**
     * Returns whether the lore of the tool item [uuid] is rendered now: always when [forced], otherwise when it was
     * not rendered within the last second. Remembers the time of every render.
     */
    private fun loreDue(uuid: UUID, forced: Boolean): Boolean {
        val now = Progression.runtime.clock.millis()
        var due = false
        loreRenderedAt.compute(uuid) { _, last ->
            due = forced || last == null || now - last >= LORE_INTERVAL_MILLIS
            if (due) now else last
        }
        return due
    }

    /** Tells [player] that [tool] reached [level] and plays the level-up sound. */
    private fun announceLevel(player: Player, tool: ToolDefinition, level: Int) {
        player.sendText {
            appendSuccessPrefix()
            success("Deine ")
            append(tool.name)
            success(" ist jetzt ")
            variableValue("Level $level")
            success(".")
        }
        player.playSound(true) {
            type(Sound.ENTITY_PLAYER_LEVELUP)
        }
    }
}
