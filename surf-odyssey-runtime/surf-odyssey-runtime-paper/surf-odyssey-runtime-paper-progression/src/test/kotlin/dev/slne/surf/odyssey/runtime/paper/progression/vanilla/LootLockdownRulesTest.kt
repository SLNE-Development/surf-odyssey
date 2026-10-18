package dev.slne.surf.odyssey.runtime.paper.progression.vanilla

import org.bukkit.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertSame

/** Tests which loot a dying entity may leave behind. */
class LootLockdownRulesTest {

    private val custom = MobLoot(emptyList(), 7)

    private val provider = object : MobLootProvider {
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
}
