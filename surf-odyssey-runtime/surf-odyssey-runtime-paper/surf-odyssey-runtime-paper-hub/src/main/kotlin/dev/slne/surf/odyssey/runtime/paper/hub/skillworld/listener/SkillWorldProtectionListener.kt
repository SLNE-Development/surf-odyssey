package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.listener

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorldRules
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent

object SkillWorldProtectionListener : Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        if (SkillWorld.isSkillWorld(event.block.world)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (!SkillWorld.isSkillWorld(block.world)) return

        val inMining = SkillWorld.regions.allows(SkillType.MINING, block.x, block.y, block.z)

        if (!SkillWorldRules.canBreak(inMining, block.type in SkillWorld.unbreakableBlocks)) {
            event.isCancelled = true
            return
        }

        if (block.type in SkillWorld.carrierBlocks) {
            event.isDropItems = false
            event.expToDrop = 0
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (SkillWorldRules.mustCancelBucket(SkillWorld.isSkillWorld(event.block.world))) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketFill(event: PlayerBucketFillEvent) {
        if (SkillWorldRules.mustCancelBucket(SkillWorld.isSkillWorld(event.block.world))) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val custom = event.spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM
        if (SkillWorldRules.mustCancelCreatureSpawn(SkillWorld.isSkillWorld(event.location.world), custom)) {
            event.isCancelled = true
        }
    }
}
