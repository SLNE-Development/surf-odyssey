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
import org.bukkit.inventory.ItemStack
import java.util.*

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

/**
 * Adds usage to the tools players hold and applies level-ups.
 *
 * Usage and chronicle events are kept in a [ToolWriteBuffer] and written to the tool item, together with its bonuses
 * and lore, at most once per second, and at once when the tool reaches a new level or a flush is requested.
 */
object ToolUsage {
    /** The changes of every tool that are not written to its item yet. */
    private val buffer = ToolWriteBuffer()

    /**
     * Adds [amount] usage to the tool in the main hand of [player] and records [detail] in its chronicle.
     *
     * Nothing happens unless the engine runs in [ProgressionMode.FULL] and the main hand holds a tool of the installed
     * content whose type is [type]. The change is written to the item at once when the tool was not written within
     * the last second or reaches a new level; otherwise it stays buffered. A new level is announced to the player with
     * a message and a sound.
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

        val holder = player.uniqueId
        val before = state.usage + buffer.pendingUsage(holder, state.uuid)
        val leveledUp = ToolProgress.isLevelUp(tool, before, added)
        buffer.add(holder, state.uuid, player.inventory.heldItemSlot, added, detail)
        if (buffer.shouldWriteNow(holder, state.uuid, now(), leveledUp)) flushTool(player, state.uuid)

        if (leveledUp) announceLevel(player, tool, ToolProgress.levelFor(tool, before + added))
    }

    /**
     * Writes the changes [holder] buffered for the tool [item] into it, with bonuses and lore, and changes it in place.
     *
     * @param holder the player whose buffered changes are written
     * @param item the item to write into, may be `null`
     * @return `true` if [item] is a tool with buffered changes of [holder] and was changed
     */
    fun writeInto(holder: UUID, item: ItemStack?): Boolean {
        if (item == null) return false
        val state = ToolItems.read(item) ?: return false
        val tool = ToolItems.definitionOf(state) ?: return false
        val delta = buffer.drain(holder, state.uuid, now()) ?: return false
        val updated = state.copy(
            usage = state.usage + delta.usage,
            chronicle = delta.events.fold(state.chronicle) { chronicle, event -> chronicle.record(event) },
        )
        ToolItems.write(item, updated)
        ToolItems.refresh(item, tool, updated)
        return true
    }

    /**
     * Writes the changes [player] buffered for the tool item [tool] into the inventory slot the tool was in at the
     * newest change, if that slot still holds it, otherwise into the first slot that holds it. Nothing is written
     * while the player holds no item with that UUID; the changes stay buffered.
     *
     * Must run on the thread of [player].
     *
     * @param player the holder
     * @param tool the unique ID of the tool item
     */
    fun flushTool(player: Player, tool: UUID) {
        val holder = player.uniqueId
        val remembered = buffer.slotOf(holder, tool) ?: return
        val inventory = player.inventory
        val slot = remembered.takeIf { it in 0 until inventory.size && ToolItems.uuidOf(inventory.getItem(it)) == tool }
            ?: ToolItems.findSlot(player, tool)
            ?: return
        val item = inventory.getItem(slot) ?: return
        if (writeInto(holder, item)) inventory.setItem(slot, item)
    }

    /**
     * Writes every buffered change of [player] into the matching tool items of the inventory.
     *
     * Must run on the thread of [player].
     *
     * @param player the player whose tools are written
     */
    fun flushAll(player: Player) {
        buffer.pendingOf(player.uniqueId).forEach { flushTool(player, it) }
    }

    /**
     * Writes every buffered change of a leaving [player] into the inventory, then drops the changes whose tool is no
     * longer in the inventory and the write times of the player.
     *
     * Must run on the thread of [player].
     *
     * @param player the leaving player
     * @return the tools whose buffered changes were dropped
     */
    fun flushOnQuit(player: Player): List<UUID> {
        flushAll(player)
        return buffer.discard(player.uniqueId)
    }

    /**
     * Returns every buffered entry as holder and tool whose last write is at least a second ago, for writes after the
     * buffering interval.
     */
    fun due(): List<Pair<UUID, UUID>> = buffer.due(now())

    /** Returns the current time of the engine clock in epoch milliseconds. */
    private fun now(): Long = Progression.runtime.clock.millis()

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
