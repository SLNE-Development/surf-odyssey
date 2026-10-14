@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.common.menu.home

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.builder.LoreBuilder
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.odyssey.core.common.home.Home
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.Registry
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import java.util.logging.Level

/** The icon of [home] as shown in the home menus, with a click hint when [clickHint] is set. */
internal fun createHomeItem(home: Home, clickHint: String? = null): ItemStack {
    val registryItemType = Registry.ITEM[home.itemType] ?: ItemType.NAME_TAG

    return buildItem(registryItemType) {
        displayName {
            primary(home.name)
        }

        buildLore {
            if (home.description.isNotEmpty()) {
                emptyLine()
                appendHomeDescription(description = home.description)
            }

            if (clickHint != null) {
                emptyLine()
                line {
                    spacer(clickHint)
                }
            }

            emptyLine()
        }
    }
}

private fun LoreBuilder.appendHomeDescription(maxWords: Int = 8, description: String) {
    val lines = description.lines()
        .flatMap { line -> line.split(" ").chunked(maxWords).map { it.joinToString(" ") } }

    lines.forEach { line ->
        line {
            spacer(line)
        }
    }
}

/** Whether the player has [permission]; tells them and plays the "no" sound when they do not. */
internal fun Player.requirePermission(permission: String): Boolean {
    if (hasPermission(permission)) return true

    playNoSound()
    sendText {
        appendErrorPrefix()
        error("Dazu hast du keine Berechtigung.")
    }

    return false
}

/**
 * Runs the suspending home action [block] for this player. A failure is logged and reported to the player with
 * [failureMessage] instead of disappearing in the coroutine.
 */
internal fun Player.launchHomeAction(failureMessage: String, block: suspend () -> Unit) {
    plugin.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Home action of $name failed", e)

            onOwnThread {
                playNoSound()
                sendText {
                    appendErrorPrefix()
                    error(failureMessage)
                }
            }
        }
    }
}

/** Runs [block] on the thread that owns this player, which menus, dialogs and teleports must be used from. */
internal suspend fun Player.onOwnThread(block: () -> Unit) {
    withContext(plugin.entityDispatcher(this)) { block() }
}

/** Teleports the player to [home] if its world is loaded on this server, otherwise tells them it is not. */
internal fun Player.teleportToHome(home: Home, server: Server = plugin.server) {
    val world = server.getWorld(home.worldUuid)

    if (world == null) {
        playNoSound()
        sendText {
            appendErrorPrefix()
            error("Die Welt dieses Homes ist auf diesem Server nicht verfügbar.")
        }
        return
    }

    val location = Location(world, home.x, home.y, home.z, home.yaw, home.pitch)

    teleportAsync(location, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept { success ->
        sendText {
            if (success) {
                appendSuccessPrefix()
                success("Du wurdest zu ")
                variableValue(home.name)
                success(" teleportiert.")
            } else {
                appendErrorPrefix()
                error("Du konntest nicht teleportiert werden.")
            }
        }
    }
}
