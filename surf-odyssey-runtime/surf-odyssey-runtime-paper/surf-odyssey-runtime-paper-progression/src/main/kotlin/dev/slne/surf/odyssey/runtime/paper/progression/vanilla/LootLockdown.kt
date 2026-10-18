package dev.slne.surf.odyssey.runtime.paper.progression.vanilla

import org.bukkit.entity.AbstractVillager
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PiglinBarterEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantInventory

/**
 * The loot a dying entity leaves behind.
 *
 * @property items the items dropped on death
 * @property exp the experience dropped on death
 */
class MobLoot(val items: List<ItemStack>, val exp: Int) {
    companion object {
        /** Loot that drops nothing. */
        val NONE = MobLoot(emptyList(), 0)
    }
}

/** Supplies the loot of entities that are allowed to drop something. */
fun interface MobLootProvider {
    /**
     * Returns the loot of a dying entity of the given type.
     *
     * @param entityType the type of the entity that died
     * @return the loot, or `null` if this type drops nothing
     */
    fun lootFor(entityType: EntityType): MobLoot?

    companion object {
        /** A provider that knows no loot, so every entity drops nothing. */
        val NONE = MobLootProvider { null }
    }
}

/** Decides which loot a dying entity leaves behind. */
object LootLockdownRules {
    /**
     * Decides the loot of a dying entity.
     *
     * @param entityType the type of the entity that died
     * @param provider the source of allowed loot
     * @return the loot to drop, [MobLoot.NONE] if the type has none, or `null` for players, whose
     *         drops are left untouched
     */
    fun lootFor(entityType: EntityType, provider: MobLootProvider): MobLoot? {
        if (entityType == EntityType.PLAYER) return null
        return provider.lootFor(entityType) ?: MobLoot.NONE
    }
}

/**
 * Removes vanilla loot: mob drops and experience, generated loot containers, piglin bartering and
 * villager trading.
 *
 * @property mobLoot the source of loot that mobs may still drop
 */
internal class LootLockdown(private val mobLoot: MobLootProvider) : Listener {

    /** Replaces the drops and experience of a dying non-player entity with the allowed loot. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityDeath(event: EntityDeathEvent) {
        val loot = LootLockdownRules.lootFor(event.entityType, mobLoot) ?: return
        event.drops.clear()
        event.drops.addAll(loot.items)
        event.droppedExp = loot.exp
    }

    /** Cancels the generation of loot in chests and other containers. */
    @EventHandler(ignoreCancelled = true)
    fun onLootGenerate(event: LootGenerateEvent) {
        event.isCancelled = true
    }

    /** Cancels piglin bartering. */
    @EventHandler(ignoreCancelled = true)
    fun onBarter(event: PiglinBarterEvent) {
        event.isCancelled = true
    }

    /** Cancels opening a trading screen. */
    @EventHandler(ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventory is MerchantInventory) event.isCancelled = true
    }

    /** Cancels interacting with villagers and wandering traders. */
    @EventHandler(ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.rightClicked is AbstractVillager) event.isCancelled = true
    }
}
