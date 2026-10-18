package dev.slne.surf.odyssey.runtime.paper.hub.common.gate

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.ProgressionView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RequirementContext
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolType
import org.bukkit.GameMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Tests for the damage schedule inside locked regions. */
class GatePlanTest {
    private val d = GateDamage()

    /** A context of a player without any progression. */
    private val context = object : RequirementContext {
        /** A progression without quests, recipes or titles. */
        override val progression = object : ProgressionView {
            /** Returns that no quest was ever accepted. */
            override fun questState(questId: String): QuestStateView? = null

            /** Returns that no recipe is unlocked. */
            override fun isRecipeUnlocked(recipeId: String) = false

            /** Returns that no title is owned. */
            override fun hasTitle(titleId: String) = false
        }

        /** Returns that no items are carried. */
        override fun countItems(ref: ItemRef) = 0

        /** Returns that no tool is held. */
        override fun toolLevelInHand(type: ToolType): Int? = null
    }

    @Test fun `no damage during the grace period`() = assertFalse(GatePlan.damageDue(0, null, 4_999, d))
    @Test fun `first damage right after the grace period`() = assertTrue(GatePlan.damageDue(0, null, 5_000, d))
    @Test fun `then every three seconds`() {
        assertFalse(GatePlan.damageDue(0, 5_000, 7_999, d)); assertTrue(GatePlan.damageDue(0, 5_000, 8_000, d))
    }

    @Test
    fun `regions can change the schedule`() {
        val quick = GateDamage(grace = 1.seconds, interval = 1.seconds)
        assertTrue(GatePlan.damageDue(0, null, 1_000, quick))
        assertTrue(GatePlan.damageDue(0, 1_000, 2_000, quick))
    }

    @Test
    fun `an area without gate is never locked`() = assertFalse(GatePlan.isLocked(null, null))

    @Test
    fun `a gate is locked while the progression is not loaded`() =
        assertTrue(GatePlan.isLocked(Requirement { true }, null))

    @Test
    fun `a gate is open once its requirement holds`() {
        assertFalse(GatePlan.isLocked(Requirement { true }, context))
        assertTrue(GatePlan.isLocked(Requirement { false }, context))
    }

    @Test
    fun `creative and spectator players and bypass holders are exempt`() {
        assertTrue(GatePlan.isExempt(GameMode.CREATIVE, hasBypass = false))
        assertTrue(GatePlan.isExempt(GameMode.SPECTATOR, hasBypass = false))
        assertTrue(GatePlan.isExempt(GameMode.SURVIVAL, hasBypass = true))
        assertFalse(GatePlan.isExempt(GameMode.SURVIVAL, hasBypass = false))
        assertFalse(GatePlan.isExempt(GameMode.ADVENTURE, hasBypass = false))
    }

    @Test
    fun `the darkness of a gate is short and hidden`() {
        assertTrue(GatePlan.isGateDarkness(GatePlan.DARKNESS_TICKS, hasParticles = false, hasIcon = false))
        assertTrue(GatePlan.isGateDarkness(1, hasParticles = false, hasIcon = false))
    }

    @Test
    fun `darkness from other sources is not the gate's`() {
        assertFalse(GatePlan.isGateDarkness(GatePlan.DARKNESS_TICKS + 1, hasParticles = false, hasIcon = false))
        assertFalse(GatePlan.isGateDarkness(-1, hasParticles = false, hasIcon = false))
        assertFalse(GatePlan.isGateDarkness(40, hasParticles = true, hasIcon = false))
        assertFalse(GatePlan.isGateDarkness(40, hasParticles = false, hasIcon = true))
    }
}
