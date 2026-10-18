package dev.slne.surf.odyssey.runtime.paper.progression.vanilla

import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertSame

/** Tests which loot a dying entity may leave behind. */
class LootLockdownRulesTest {

    private val custom = MobLoot(emptyList(), 7)

    /** Supplies [custom] loot for zombies and no loot for every other type. */
    private val provider = object : MobLootProvider {
        /** Returns [custom] for zombies, otherwise `null`. */
        override fun lootFor(entityType: EntityType): MobLoot? =
            if (entityType == EntityType.ZOMBIE) custom else null
    }

    @Test
    fun `without a provider an entity leaves nothing`() {
        assertSame(MobLoot.NONE, LootLockdownRules.lootFor(EntityType.ZOMBIE, MobLootProvider.NONE))
    }

    @Test
    fun `a provider supplies the loot of a type it knows`() {
        assertSame(custom, LootLockdownRules.lootFor(EntityType.ZOMBIE, provider))
    }

    @Test
    fun `a type the provider does not know leaves nothing`() {
        assertSame(MobLoot.NONE, LootLockdownRules.lootFor(EntityType.COW, provider))
    }

    @Test
    fun `players are never affected`() {
        assertSame(null, LootLockdownRules.lootFor(EntityType.PLAYER, provider))
    }

    /** Returns a mocked entity of [type]. */
    private fun entity(type: EntityType): Entity = mockk { every { this@mockk.type } returns type }

    @Test
    fun `a provider without entity lookup uses the type of the entity`() {
        assertSame(custom, LootLockdownRules.lootFor(entity(EntityType.ZOMBIE), provider))
        assertSame(MobLoot.NONE, LootLockdownRules.lootFor(entity(EntityType.COW), provider))
    }

    @Test
    fun `a provider can decide by the dying entity`() {
        val zombie = entity(EntityType.ZOMBIE)
        val byEntity = object : MobLootProvider {
            /** Knows no type. */
            override fun lootFor(entityType: EntityType): MobLoot? = null

            /** Knows only [zombie]. */
            override fun lootFor(entity: Entity): MobLoot? = if (entity === zombie) custom else null
        }
        assertSame(custom, LootLockdownRules.lootFor(zombie, byEntity))
        assertSame(MobLoot.NONE, LootLockdownRules.lootFor(entity(EntityType.ZOMBIE), byEntity))
    }

    @Test
    fun `dying players are never affected`() {
        assertSame(null, LootLockdownRules.lootFor(entity(EntityType.PLAYER), provider))
    }
}
