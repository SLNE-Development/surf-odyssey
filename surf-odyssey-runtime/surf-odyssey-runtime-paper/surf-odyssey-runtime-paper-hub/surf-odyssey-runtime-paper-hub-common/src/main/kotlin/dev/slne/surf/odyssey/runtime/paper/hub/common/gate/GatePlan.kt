package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RequirementContext
import org.bukkit.GameMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How a locked region hurts players who stay inside.
 *
 * @property grace time after entering before the first damage
 * @property interval time between two damages after the first
 * @property damage damage dealt each time, 2.0 being one heart
 */
data class GateDamage(val grace: Duration = 5.seconds, val interval: Duration = 3.seconds, val damage: Double = 2.0)

/** Decides when gates lock players out and when a locked region deals damage. */
object GatePlan {
    /** How long each application of the gate darkness lasts, in ticks; longer than the check interval of a second. */
    const val DARKNESS_TICKS = 60

    /**
     * Returns whether a darkness effect is the one gates apply: at most [DARKNESS_TICKS] left, without particles and
     * without icon. Infinite effects have a negative duration and never count.
     *
     * @param durationTicks the remaining duration of the effect
     * @param hasParticles whether the effect shows particles
     * @param hasIcon whether the effect shows an icon
     */
    fun isGateDarkness(durationTicks: Int, hasParticles: Boolean, hasIcon: Boolean): Boolean =
        durationTicks in 0..DARKNESS_TICKS && !hasParticles && !hasIcon

    /**
     * Returns whether a player inside a locked region takes damage now.
     *
     * The first damage is due [GateDamage.grace] after entering, every further one [GateDamage.interval] after the
     * previous one.
     *
     * @param enteredAt when the player entered the region, in epoch milliseconds
     * @param lastDamageAt when the player last took damage in it, or `null` if not yet
     * @param now the current time in epoch milliseconds
     * @param damage the damage schedule of the region
     */
    fun damageDue(enteredAt: Long, lastDamageAt: Long?, now: Long, damage: GateDamage): Boolean =
        if (lastDamageAt == null) now - enteredAt >= damage.grace.inWholeMilliseconds
        else now - lastDamageAt >= damage.interval.inWholeMilliseconds

    /**
     * Returns whether [gate] locks a player out.
     *
     * @param gate the requirement of the region, portal or world, or `null` if it has none
     * @param context the player's requirement context, or `null` while their progression is not loaded
     * @return `false` without gate; otherwise `true` unless the context exists and meets the gate
     */
    fun isLocked(gate: Requirement?, context: RequirementContext?): Boolean {
        if (gate == null) return false
        return context == null || !gate.isMet(context)
    }

    /**
     * Returns whether gates do not apply to a player.
     *
     * @param gameMode the player's game mode
     * @param hasBypass whether the player has the gate bypass permission
     * @return `true` in creative or spectator mode or with the bypass permission
     */
    fun isExempt(gameMode: GameMode, hasBypass: Boolean): Boolean =
        hasBypass || gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR
}
