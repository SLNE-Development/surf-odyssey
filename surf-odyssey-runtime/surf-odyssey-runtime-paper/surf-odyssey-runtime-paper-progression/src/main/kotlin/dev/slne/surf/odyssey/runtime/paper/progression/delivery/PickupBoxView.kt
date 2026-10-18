@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.delivery

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.scope
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

/**
 * The pickup box: every unclaimed delivery of the viewer, oldest first. Clicking a delivery claims it and puts its
 * item into the inventory, then the box is opened again with the remaining deliveries.
 */
val pickupBoxView: AbstractPaginatedSurfView = paginatedSurfView("Abholfach") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
    }

    layoutTarget('D')

    pagination<DeliveryDto> {
        lazyAsyncSource { context ->
            val player = context.player
            ProgressionModule.plugin.scope.future<Iterable<DeliveryDto>> { loadDeliveries(player) }
        }
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
 * Opens the pickup box for [player], or tells the player that it is not available while the progression service
 * or the player's state is not.
 */
fun openPickupBox(player: Player) {
    if (!Progression.isAvailable || !Progression.runtime.store.isAvailableFor(player.uniqueId)) {
        ProgressionMessages.unavailable(player)
        return
    }
    pickupBoxView.open(player)
}

/** Loads the unclaimed deliveries of [player], oldest first; returns none when the service fails. */
private suspend fun loadDeliveries(player: Player): List<DeliveryDto> = try {
    ProgressionBridge.loadState(player.uniqueId).deliveries.sortedBy { it.createdAt }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Progression.markUnavailable()
    ProgressionModule.plugin.logger.log(Level.WARNING, "Could not load the deliveries of ${player.uniqueId}", e)
    emptyList()
}

/** Returns the item of [delivery] with a lore line that explains how to pick it up. */
private fun displayItem(delivery: DeliveryDto): ItemStack {
    val item = ItemCodec.decode(delivery.itemBase64)
    item.editMeta { meta ->
        val lore = meta.lore().orEmpty() + Component.empty() + buildText { spacer("Klicke zum Abholen".toSmallCaps()) }
        meta.lore(lore)
    }
    return item
}

/** Claims [delivery] for [player] in the background, tells the player the result and opens the box again. */
private fun claimFromBox(player: Player, delivery: DeliveryDto) {
    val plugin = ProgressionModule.plugin
    plugin.launch {
        val result = Progression.runtime.operations.claim(player, delivery)
        withContext(plugin.entityDispatcher(player)) {
            when (result) {
                null -> {
                    player.playNoSound()
                    ProgressionMessages.inventoryFull(player)
                }
                ClaimResult.CLAIMED -> player.playYesSound()
                ClaimResult.GONE -> {
                    player.playNoSound()
                    ProgressionMessages.deliveryGone(player)
                }
                ClaimResult.FAILED -> {
                    player.playNoSound()
                    ProgressionMessages.unavailable(player)
                    player.closeInventory()
                    return@withContext
                }
            }
            if (player.isOnline) pickupBoxView.open(player)
        }
    }
}
