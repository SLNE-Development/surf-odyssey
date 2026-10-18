package dev.slne.surf.odyssey.runtime.paper.progression.tool

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconColor
import dev.slne.surf.api.paper.inventory.framework.view.icon.ViewIconType
import dev.slne.surf.api.paper.inventory.framework.view.icon.viewIcon
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.closeItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessorySlot
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationPlan
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

/** The source stored with the operations of the accessory view. */
private const val ACCESSORY_SOURCE = "tool_accessory"

/**
 * The tool an accessory view is opened for.
 *
 * @property toolUuid the unique ID of the tool item
 */
private class AccessoryViewContent(val toolUuid: UUID)

/**
 * A tool item found in the inventory of a player.
 *
 * @property slot the inventory slot of the item
 * @property item the item
 * @property state the state of the item
 * @property tool the definition of the tool
 */
private class HeldTool(val slot: Int, val item: ItemStack, val state: ToolState, val tool: ToolDefinition)

/**
 * The accessory view of a tool: one slot per open accessory slot of the tool. Clicking an empty slot inserts the
 * first fitting accessory from the inventory of the viewer, clicking a filled slot takes its accessory out. Both run
 * as an operation with the accessory held in escrow, then the view is opened again.
 */
val accessoryView: AbstractSurfView = surfView("Zubehör") {
    settings {
        rows(ViewRows.THREE)
    }

    onFirstRender {
        val toolUuid = (initialData as? AccessoryViewContent)?.toolUuid
        val held = toolUuid?.let { findTool(player, it) }
        val slots = held?.let { ToolSlots.unlocked(it.tool, ToolProgress.levelFor(it.tool, it.state.usage)) }.orEmpty()

        if (held == null || slots.isEmpty()) {
            slot(2, 5) { withItem(noSlotsItem) }
        } else {
            slotColumns(slots.size).zip(slots).forEach { (column, accessorySlot) ->
                val accessory = held.state.accessories[accessorySlot.id]?.let(Progression.runtime.registry::accessory)
                slot(2, column) {
                    if (accessory != null) {
                        withItem(filledSlotItem(accessory))
                        onItemClick {
                            playGeneralClickSound()
                            closeForPlayer()
                            removeAccessory(player, held.state.uuid, accessorySlot)
                        }
                    } else {
                        withItem(emptySlotItem(accessorySlot))
                        onItemClick {
                            playGeneralClickSound()
                            val fitting = fittingAccessory(player, accessorySlot)
                            if (fitting == null) {
                                playNoSound()
                                ToolMessages.noFittingAccessory(player)
                                return@onItemClick
                            }
                            closeForPlayer()
                            insertAccessory(player, held.state.uuid, accessorySlot, fitting)
                        }
                    }
                }
            }
        }

        slot(3, 5) {
            withItem(closeItem)
            onItemClick {
                playGeneralClickSound()
                closeForPlayer()
            }
        }
    }
}

/**
 * Opens the accessory view for the tool item in the main hand of [player]. Does nothing when the main hand holds no
 * tool of the installed content.
 */
fun openAccessoryView(player: Player) {
    val uuid = ToolItems.read(player.inventory.itemInMainHand)?.takeIf { ToolItems.definitionOf(it) != null }?.uuid
        ?: return
    accessoryView.open(player, AccessoryViewContent(uuid))
}

/** Returns the columns of row 2 for [count] slots, centred: two columns apart for up to four slots, else adjacent. */
private fun slotColumns(count: Int): List<Int> {
    val shown = count.coerceAtMost(9)
    return if (shown <= 4) {
        (0 until shown).map { 5 - (shown - 1) + 2 * it }
    } else {
        val start = 5 - (shown - 1) / 2
        (0 until shown).map { (start + it).coerceAtMost(9) }
    }
}

/** Returns the tool item with [uuid] in the inventory of [player] with its state and definition, or `null`. */
private fun findTool(player: Player, uuid: UUID): HeldTool? {
    val slot = ToolItems.findSlot(player, uuid) ?: return null
    val item = player.inventory.getItem(slot) ?: return null
    val state = ToolItems.read(item) ?: return null
    val tool = ToolItems.definitionOf(state) ?: return null
    return HeldTool(slot, item, state, tool)
}

/** Returns the first accessory of the installed content that fits [slot] and that [player] carries, or `null`. */
private fun fittingAccessory(player: Player, slot: AccessorySlot): AccessoryDefinition? {
    val candidates = Progression.runtime.registry.allAccessories.filter { it.slot.id == slot.id }
    val contents = player.inventory.storageContents
    return candidates.firstOrNull { accessory -> contents.any(accessory.item::matches) }
}

/**
 * Stores [state] on the tool item [held] and puts it back into its inventory slot of [player], with bonuses and lore
 * applied.
 */
private fun store(player: Player, held: HeldTool, state: ToolState) {
    ToolItems.write(held.item, state)
    ToolItems.refresh(held.item, held.tool, state)
    player.inventory.setItem(held.slot, held.item)
}

/**
 * Takes one [accessory] from the inventory of [player] into escrow, commits the operation and attaches the accessory
 * to the slot [slot] of the tool item [toolUuid]. If the player left or the tool cannot take it after the commit, the
 * accessory is given back with [returnAccessory]. The view is opened again afterwards.
 */
private fun insertAccessory(player: Player, toolUuid: UUID, slot: AccessorySlot, accessory: AccessoryDefinition) {
    val plugin = ProgressionModule.plugin
    val runtime = Progression.runtime
    plugin.launch {
        val plan = OperationPlan(listOf(Ingredient(accessory.item.ref, 1)), emptyList(), emptyList(), ACCESSORY_SOURCE)
        val outcome = runtime.operations.run(player, plan)
        if (outcome == OperationOutcome.Success && !player.isOnline) {
            returnAccessory(player, accessory)
            return@launch
        }
        withContext(plugin.entityDispatcher(player)) {
            if (outcome != OperationOutcome.Success) {
                runtime.operations.report(player, outcome)
                if (outcome != OperationOutcome.Busy) player.playNoSound()
                return@withContext
            }
            val held = findTool(player, toolUuid)
            val attached = held?.let { AccessoryRules.attach(it.tool, it.state, slot.id, accessory) }
            if (held == null || attached == null) {
                returnAccessory(player, accessory)
                return@withContext
            }
            store(player, held, attached)
            player.playYesSound()
            reopen(player, toolUuid)
        }
    }
}

/**
 * Detaches the accessory in the slot [slot] of the tool item [toolUuid] of [player] and gives it to the player through
 * an operation, so that it lands in the inventory or the pickup box. When the operation definitely was not stored, the
 * accessory is attached again, or given back as an item when the tool is gone. When the service could not be reached,
 * the slot stays empty, the case is logged and the player is told that the accessory will be delivered. The view is
 * opened again after a successful removal.
 *
 * Must run on the thread of [player].
 */
private fun removeAccessory(player: Player, toolUuid: UUID, slot: AccessorySlot) {
    val held = findTool(player, toolUuid) ?: return
    val (detached, accessoryId) = AccessoryRules.detach(held.state, slot.id) ?: return
    val runtime = Progression.runtime
    val accessory = runtime.registry.accessory(accessoryId) ?: run {
        ProgressionModule.plugin.logger.warning("Tool $toolUuid holds the unknown accessory $accessoryId")
        return
    }
    store(player, held, detached)

    val plugin = ProgressionModule.plugin
    plugin.launch {
        val plan = OperationPlan(emptyList(), emptyList(), listOf(ItemReward(accessory.item.ref, 1)), ACCESSORY_SOURCE)
        val outcome = runtime.operations.run(player, plan)
        val result = AccessoryRules.afterRemoval(outcome)
        if (result == RemovalResult.PENDING) {
            plugin.logger.warning(
                "Removing accessory ${accessory.id} from tool $toolUuid of ${player.uniqueId} was not confirmed: " +
                    "the slot stays empty"
            )
        }
        withContext(plugin.entityDispatcher(player)) {
            when (result) {
                RemovalResult.DELIVERED -> {
                    player.playYesSound()
                    reopen(player, toolUuid)
                }
                RemovalResult.PENDING -> {
                    player.playNoSound()
                    ToolMessages.accessoryPending(player)
                }
                RemovalResult.REATTACH -> {
                    runtime.operations.report(player, outcome)
                    player.playNoSound()
                    val current = findTool(player, toolUuid)
                    val reattached = current?.let { AccessoryRules.attach(it.tool, it.state, slot.id, accessory) }
                    if (current != null && reattached != null) {
                        store(player, current, reattached)
                    } else {
                        giveBack(player, accessory)
                    }
                }
            }
        }
    }
}

/**
 * Gives one [accessory] back to [player] in the background through an operation that delivers it into the inventory
 * or the pickup box. If that operation is not stored, for example because the player left, the loss is logged.
 */
private fun returnAccessory(player: Player, accessory: AccessoryDefinition) {
    val plugin = ProgressionModule.plugin
    val runtime = Progression.runtime
    plugin.launch {
        val plan = OperationPlan(emptyList(), emptyList(), listOf(ItemReward(accessory.item.ref, 1)), ACCESSORY_SOURCE)
        val outcome = runtime.operations.run(player, plan)
        if (outcome != OperationOutcome.Success) {
            plugin.logger.severe("Could not return accessory ${accessory.id} to ${player.uniqueId}: $outcome")
        }
    }
}

/** Puts one [accessory] into the inventory of [player], dropping what does not fit at the feet of the player. */
private fun giveBack(player: Player, accessory: AccessoryDefinition) {
    val plugin = ProgressionModule.plugin
    if (!player.isOnline) {
        plugin.logger.severe("Lost accessory ${accessory.id} of ${player.uniqueId}: the player left")
        return
    }
    player.inventory.addItem(accessory.item.create(1)).values.forEach {
        player.world.dropItemNaturally(player.location, it)
    }
}

/** Opens the accessory view of the tool item [toolUuid] for [player] again while the player is online. */
private fun reopen(player: Player, toolUuid: UUID) {
    if (player.isOnline) accessoryView.open(player, AccessoryViewContent(toolUuid))
}

/** Returns the item of a filled slot: the accessory with a lore line that explains how to take it out. */
private fun filledSlotItem(accessory: AccessoryDefinition): ItemStack {
    val item = accessory.item.create(1)
    item.editMeta { meta ->
        val lore = meta.lore().orEmpty() + Component.empty() + buildText { spacer("Klicke zum Entnehmen".toSmallCaps()) }
        meta.lore(lore)
    }
    return item
}

/** Returns the item of an empty slot named after [slot]. */
private fun emptySlotItem(slot: AccessorySlot): ItemStack = viewIcon(ViewIconType.PLUS, ViewIconColor.WHITE) {
    displayName {
        primary("Freier Slot: ", TextDecoration.BOLD)
        append(slot.name)
    }
    buildLore {
        emptyLine()
        line { spacer("Klicke, um passendes Zubehör einzusetzen".toSmallCaps()) }
        emptyLine()
    }
}

/** The item shown when the tool has no open accessory slot. */
private val noSlotsItem: ItemStack = viewIcon(ViewIconType.EXCLAMATION_MARK, ViewIconColor.YELLOW) {
    displayName {
        primary("Keine Zubehör-Slots", TextDecoration.BOLD)
    }
    buildLore {
        emptyLine()
        line { spacer("Dein Werkzeug schaltet Slots mit höherem Level frei".toSmallCaps()) }
        emptyLine()
    }
}

/** The chat messages of the tool features. */
object ToolMessages {
    /** Tells [player] that the inventory holds no accessory that fits the chosen slot. */
    fun noFittingAccessory(player: Player) = player.sendText {
        appendErrorPrefix()
        error("Du hast kein passendes Zubehör dabei.")
    }

    /** Tells [player] that a removed accessory is delivered once the progression service can be reached again. */
    fun accessoryPending(player: Player) = player.sendText {
        appendInfoPrefix()
        info("Das Zubehör wird dir zugestellt, sobald der Dienst wieder erreichbar ist. Prüfe dein Abholfach.")
    }
}
