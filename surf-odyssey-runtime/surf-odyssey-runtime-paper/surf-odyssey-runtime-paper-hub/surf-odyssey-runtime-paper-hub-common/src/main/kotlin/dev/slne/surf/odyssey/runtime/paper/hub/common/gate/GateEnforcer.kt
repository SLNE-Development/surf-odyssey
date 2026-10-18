package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.PlayerRegionTracker
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.HubTravel
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

    /** How long each refresh of the darkness lasts, in ticks; longer than the check interval. */
    private const val DARKNESS_TICKS = 60

    /**
     * The locked region a player is in.
     *
     * @property regionId the region
     * @property enteredAt when the player entered it, in epoch milliseconds
     * @property lastDamageAt when the player last took damage in it, or `null` if not yet
     */
    private class Inside(val regionId: String, val enteredAt: Long, var lastDamageAt: Long? = null)

    /** The locked region each player is in. */
    private val inside = ConcurrentHashMap<UUID, Inside>()

    /** The players this enforcer darkened. */
    private val darkened = ConcurrentHashMap.newKeySet<UUID>()

    /** The players whose world gate was checked since they joined. */
    private val worldChecked = ConcurrentHashMap.newKeySet<UUID>()

    /** The running check task. */
    private var task: BukkitTask? = null

    /**
     * Starts checking every online player once per second.
     *
     * @param hub the hub plugin
     * @param clock the source of the current time
     */
    fun start(hub: HubPlugin, clock: Clock = Clock.systemUTC()) {
        task = hub.server.scheduler.runTaskTimer(hub, Runnable {
            val now = clock.millis()
            hub.server.onlinePlayers.forEach { check(hub, it, now) }
        }, 20L, 20L)
    }

    /** Stops checking. */
    fun stop() {
        task?.cancel()
        task = null
    }

    /**
     * Forgets everything about [player].
     *
     * @param player the leaving player
     */
    fun forget(player: Player) {
        inside.remove(player.uniqueId)
        darkened.remove(player.uniqueId)
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
        val state = inside[player.uniqueId]?.takeIf { it.regionId == region.id }
            ?: Inside(region.id, now).also { inside[player.uniqueId] = it }
        player.addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, DARKNESS_TICKS, 0, false, false, false))
        darkened += player.uniqueId
        GateAccess.tellRegionLocked(player)
        if (GatePlan.damageDue(state.enteredAt, state.lastDamageAt, now, region.gateDamage)) {
            player.damage(region.gateDamage.damage)
            state.lastDamageAt = now
        }
    }

    /** Ends the effects of a locked region on [player]. */
    private fun release(player: Player) {
        inside.remove(player.uniqueId)
        if (darkened.remove(player.uniqueId)) player.removePotionEffect(PotionEffectType.DARKNESS)
    }

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
