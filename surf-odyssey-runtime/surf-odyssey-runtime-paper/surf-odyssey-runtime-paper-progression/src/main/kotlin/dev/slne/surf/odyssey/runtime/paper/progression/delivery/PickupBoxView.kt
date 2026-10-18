@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.delivery

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.odyssey.core.common.progression.DeliveryDto
import dev.slne.surf.odyssey.core.common.progression.ProgressionBridge
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.closeItem
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playGeneralClickSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.progression.ItemCodec
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.StackCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.logging.Level

/**
 * The deliveries a pickup box is opened with.
 *
 * @property deliveries the unclaimed deliveries, oldest first
 */
private class PickupBoxContent(val deliveries: List<DeliveryDto>)

/**
 * The pickup box: the unclaimed deliveries of the viewer, oldest first, as loaded by [openPickupBox]. Clicking a
 * delivery claims it and puts its item into the inventory, then the box is opened again with the remaining
 * deliveries.
 */
val pickupBoxView: AbstractPaginatedSurfView = paginatedSurfView("Abholfach") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('D')

    pagination<DeliveryDto> {
        lazySource { context -> (context.initialData as? PickupBoxContent)?.deliveries.orEmpty() }
        itemFactory { delivery ->
            withItem(displayItem(delivery))

            onItemClick {
                playGeneralClickSound()
                claimFromBox(player, delivery)
            }
        }
    }

    onFirstRender {
        slot(4, 1) {
            withItem(closeItem)
            onItemClick {
                playGeneralClickSound()
                closeForPlayer()
            }
        }
    }
}

/**
 * Loads the deliveries of [player] in the background and opens the pickup box with them. While the progression
 * service or the player's state is not available, or when loading fails, the player is told that it is not
 * available and an open inventory is closed.
 */
fun openPickupBox(player: Player) {
    if (!Progression.isAvailable || !Progression.runtime.store.isAvailableFor(player.uniqueId)) {
        ProgressionMessages.unavailable(player)
        return
    }
    val plugin = ProgressionModule.plugin
    plugin.launch {
        val deliveries = loadDeliveries(player)
        withContext(plugin.entityDispatcher(player)) {
            if (!player.isOnline) return@withContext
            if (deliveries == null) {
                player.closeInventory()
                ProgressionMessages.unavailable(player)
                return@withContext
            }
            pickupBoxView.open(player, PickupBoxContent(deliveries))
        }
    }
}

/** Loads the unclaimed deliveries of [player], oldest first; returns `null` when the service fails. */
private suspend fun loadDeliveries(player: Player): List<DeliveryDto>? = try {
    ProgressionBridge.loadState(player.uniqueId).deliveries.sortedBy { it.createdAt }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Progression.reportFailure(ProgressionModule.plugin.logger, "load the deliveries of ${player.uniqueId}", e)
    null
}

/** Decodes the items of the deliveries the pickup box shows. */
internal object DeliveryDisplay {
    /**
     * Returns the item of [delivery] decoded with [codec], or `null` if it cannot be decoded, in which case
     * [onFailure] is called with the ID of the delivery and the failure.
     *
     * @param codec decodes the item
     * @param delivery the delivery to show
     * @param onFailure called when the item cannot be decoded
     */
    fun decodeOrNull(codec: StackCodec, delivery: DeliveryDto, onFailure: (UUID, Exception) -> Unit): ItemStack? =
        try {
            codec.decode(delivery.itemBase64)
        } catch (e: Exception) {
            onFailure(delivery.id, e)
            null
        }
}

/**
 * Returns the item of [delivery] with a lore line that explains how to pick it up, or a barrier named "Beschädigte
 * Lieferung" if the item cannot be decoded, which is logged with the ID of the delivery.
 */
private fun displayItem(delivery: DeliveryDto): ItemStack {
    val item = DeliveryDisplay.decodeOrNull(ItemCodec, delivery) { id, e ->
        ProgressionModule.plugin.logger.log(Level.WARNING, "Could not decode the item of delivery $id", e)
    } ?: return brokenDeliveryItem()
    item.editMeta { meta ->
        val lore = meta.lore().orEmpty() + Component.empty() + buildText { spacer("Klicke zum Abholen".toSmallCaps()) }
        meta.lore(lore)
    }
    return item
}

/** Claims [delivery] for [player] in the background, tells the player the result and loads and opens the box again. */
private fun claimFromBox(player: Player, delivery: DeliveryDto) {
    val plugin = ProgressionModule.plugin
    plugin.launch {
        val result = Progression.runtime.operations.claim(player, delivery)
        withContext(plugin.entityDispatcher(player)) {
            when (result) {
                ClaimResult.NO_ROOM -> {
                    player.playNoSound()
                    ProgressionMessages.inventoryFull(player)
                }
                ClaimResult.BUSY -> return@withContext
                ClaimResult.CLAIMED -> player.playYesSound()
                ClaimResult.GONE -> {
                    player.playNoSound()
                    ProgressionMessages.deliveryGone(player)
                }
                ClaimResult.BROKEN -> {
                    player.playNoSound()
                    ProgressionMessages.deliveryBroken(player)
                }
                ClaimResult.FAILED -> {
                    player.playNoSound()
                    ProgressionMessages.unavailable(player)
                    player.closeInventory()
                    return@withContext
                }
            }
            if (player.isOnline) openPickupBox(player)
        }
    }
}

/** Returns the placeholder shown for a delivery whose item cannot be decoded. */
private fun brokenDeliveryItem(): ItemStack = ItemStack(Material.BARRIER).apply {
    editMeta { it.displayName(Component.text("Beschädigte Lieferung").decoration(TextDecoration.ITALIC, false)) }
}
