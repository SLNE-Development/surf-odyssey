package dev.slne.surf.odyssey.runtime.paper.hub.doublejump

import dev.slne.surf.odyssey.runtime.paper.common.utils.OdysseyBasePermissionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DoubleJumpListener : Listener {
    private const val DASH_SPEED = 5.0
    private const val DASH_LIFT = 0.15
    private const val TRAIL_TICKS = 6L
    private val DOUBLE_TAP_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(350)

    private val groundJumps = ConcurrentHashMap<UUID, Long>()
    private val jumpHeld: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    @EventHandler(ignoreCancelled = true)
    fun onInput(event: PlayerInputEvent) {
        val player = event.player
        val uuid = player.uniqueId

        if (!canUse(player)) {
            forget(uuid)
            return
        }

        if (!event.input.isJump) {
            jumpHeld.remove(uuid)
            return
        }
        if (!jumpHeld.add(uuid)) return

        @Suppress("DEPRECATION")
        if (player.isOnGround) {
            groundJumps[uuid] = System.nanoTime()
            return
        }

        val firstJump = groundJumps.remove(uuid) ?: return
        if (System.nanoTime() - firstJump > DOUBLE_TAP_WINDOW_NANOS) return

        dash(player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        forget(event.player.uniqueId)
    }

    private fun dash(player: Player) {
        val boost = player.eyeLocation.direction.multiply(DASH_SPEED)
        player.velocity = boost.setY(maxOf(boost.y, DASH_LIFT))

        val world = player.world
        world.spawnParticle(Particle.GUST_EMITTER_SMALL, player.location, 1)
        world.playSound(player.location, Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 1.1f)

        var ticks = 0L
        player.scheduler.runAtFixedRate(plugin, { task ->
            if (++ticks > TRAIL_TICKS) {
                task.cancel()
                return@runAtFixedRate
            }
            world.spawnParticle(Particle.SMALL_GUST, player.location.add(0.0, 0.5, 0.0), 2, 0.2, 0.3, 0.2, 0.0)
            world.spawnParticle(Particle.CLOUD, player.location, 3, 0.2, 0.05, 0.2, 0.02)
        }, null, 1L, 1L)
    }

    private fun forget(uuid: UUID) {
        groundJumps.remove(uuid)
        jumpHeld.remove(uuid)
    }

    private fun canUse(player: Player) =
        (player.gameMode == GameMode.SURVIVAL || player.gameMode == GameMode.ADVENTURE) &&
                player.hasPermission(OdysseyBasePermissionRegistry.HUB_DOUBLE_JUMP)
}
