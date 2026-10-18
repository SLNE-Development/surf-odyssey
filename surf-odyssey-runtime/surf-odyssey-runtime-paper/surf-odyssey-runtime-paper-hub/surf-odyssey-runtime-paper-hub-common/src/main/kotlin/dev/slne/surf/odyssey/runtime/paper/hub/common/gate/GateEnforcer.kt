package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.PlayerRegionTracker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.HubTravel
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Applies the gates of the hub world once per second on the main thread.
 *
 * A player inside a region whose gate locks them out is darkened and reminded in the action bar, and takes damage
 * after the region's grace period and then at its interval. The darkness ends at the first check after the player
 * leaves the region, meets its gate or becomes exempt.
 *
 * A player whose progression has loaded since joining is sent to the lobby once if the world gate of the hub locks
 * them out.
 */
object GateEnforcer {
    /** The server category of the lobby. */
    const val LOBBY_CATEGORY = "odyssey-hub-lobby"

    /** The locked region each player is in and whether this enforcer darkened them. */
    private val tracker = GateTracker()

    /** The players whose world gate was checked since they joined. */
    private val worldChecked = ConcurrentHashMap.newKeySet<UUID>()

    /** The running check task. */
    private var task: BukkitTask? = null

    /**
     * Starts checking every online player once per second. A check that fails for one player is logged as a warning
     * and does not stop the checks of the others.
     *
     * @param hub the hub plugin
     * @param clock the source of the current time
     */
    fun start(hub: HubPlugin, clock: Clock = Clock.systemUTC()) {
        task = hub.server.scheduler.runTaskTimer(hub, Runnable {
            val now = clock.millis()
            hub.server.onlinePlayers.forEach { player ->
                runCatching { check(hub, player, now) }
                    .onFailure { hub.logger.log(Level.WARNING, "Could not apply the gates to ${player.name}", it) }
            }
        }, 20L, 20L)
    }

    /** Stops checking, removes the darkness it applied from every online player and forgets every player. */
    fun stop() {
        task?.cancel()
        task = null
        tracker.clear().forEach { id -> Bukkit.getPlayer(id)?.let(::removeGateDarkness) }
        worldChecked.clear()
    }

    /**
     * Removes the darkness this enforcer applied to [player] and forgets everything about them.
     *
     * @param player the leaving player
     */
    fun forget(player: Player) {
        release(player)
        worldChecked.remove(player.uniqueId)
    }

    /** Applies the region gate and the world gate to [player] at [now]. */
    private fun check(hub: HubPlugin, player: Player, now: Long) {
        checkWorldGate(hub, player)
        val region = PlayerRegionTracker.regionOf(player)
        if (player.isDead || player.world != hub.hubWorld || region == null || !GateAccess.isLockedOut(player, region)) {
            release(player)
            return
        }
        punish(player, region, now)
    }

    /** Darkens [player] inside the locked [region], reminds them and deals damage when due. */
    private fun punish(player: Player, region: Region, now: Long) {
        val damageDue = tracker.inLockedRegion(player.uniqueId, region.id, region.gateDamage, now)
        darken(player)
        GateAccess.tellRegionLocked(player)
        if (damageDue) player.damage(region.gateDamage.damage)
    }

    /** Ends the effects of a locked region on [player]. */
    private fun release(player: Player) {
        if (tracker.release(player.uniqueId)) removeGateDarkness(player)
    }

    /** Applies the gate darkness to [player] unless darkness from another source is active. */
    private fun darken(player: Player) {
        val current = player.getPotionEffect(PotionEffectType.DARKNESS)
        if (current != null && !isGateDarkness(current)) return
        player.addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, GatePlan.DARKNESS_TICKS, 0, false, false, false))
    }

    /** Removes the darkness of [player] if it is the gate darkness. */
    private fun removeGateDarkness(player: Player) {
        val current = player.getPotionEffect(PotionEffectType.DARKNESS) ?: return
        if (isGateDarkness(current)) player.removePotionEffect(PotionEffectType.DARKNESS)
    }

    /** Returns whether [effect] is the darkness gates apply. */
    private fun isGateDarkness(effect: PotionEffect): Boolean =
        GatePlan.isGateDarkness(effect.duration, effect.hasParticles(), effect.hasIcon())

    /**
     * Checks the world gate of [hub] once per join, as soon as the progression of [player] is loaded, and sends them
     * to the lobby if it locks them out. Exempt players and worlds without gate pass at once.
     */
    private fun checkWorldGate(hub: HubPlugin, player: Player) {
        if (player.uniqueId in worldChecked) return
        val gate = hub.worldGate
        if (gate == null || GateAccess.isExempt(player)) {
            worldChecked += player.uniqueId
            return
        }
        if (Progression.runtimeOrNull?.store?.get(player.uniqueId) == null) return
        worldChecked += player.uniqueId
        if (GateAccess.isLockedOut(player, gate)) hub.launch { HubTravel.send(player, LOBBY_CATEGORY) }
    }
}
