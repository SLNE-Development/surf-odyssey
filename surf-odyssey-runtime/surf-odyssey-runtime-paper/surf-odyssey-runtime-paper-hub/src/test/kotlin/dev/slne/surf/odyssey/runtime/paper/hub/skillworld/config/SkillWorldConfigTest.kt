package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config

import dev.slne.surf.api.core.config.serializer.collection.map.ThrowExceptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkillWorldConfigTest {
    @Test
    fun `defaults match the documented skill world settings`() {
        val config = SkillWorldConfig()

        assertEquals("skillworld", config.gameModeIdentifier)
        assertNull(config.worldUuid)
        assertEquals(60L, config.mining.resetIntervalMinutes)
        assertEquals(0.05, config.mining.oreChance)
        assertEquals(listOf(60L, 30L, 10L, 5L), config.mining.warningSeconds)
        assertEquals(listOf("BARRIER", "BEDROCK"), config.mining.unbreakableBlocks)
        assertEquals(setOf("STONE", "DEEPSLATE"), config.mining.ores.keys)
        assertEquals("skill_portal", config.portalBlock.nexoBlockId)
    }

    @Test
    fun `an empty ore map falls back to the default ores`() {
        val empty = MiningConfig(ores = emptyMap())
        assertEquals(DEFAULT_ORES, empty.effectiveOres())

        val custom = MiningConfig(ores = mapOf("STONE" to mapOf("COAL_ORE" to 1)))
        assertEquals(custom.ores, custom.effectiveOres())
    }

    @Test
    fun `the portal map is written with the serializer that removes deleted keys`() {
        val type = SkillWorldConfig::class.java.getDeclaredField("portals").annotatedType

        assertTrue(type.isAnnotationPresent(ThrowExceptions::class.java))
    }
}
