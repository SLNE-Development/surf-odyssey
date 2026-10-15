package dev.slne.surf.odyssey.runtime.paper.hub.skill.mining

import dev.slne.surf.api.paper.pdc.block.pdc
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.skill.awardSkillXp
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.persistence.PersistentDataType

object MiningListener : Listener {
    private val placedKey = NamespacedKey("odyssey", "player_placed")

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        if (MiningXp.forBlock(block.type) == null) return

        block.pdc().set(placedKey, PersistentDataType.BOOLEAN, true)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.wasPlacedByPlayer()) {
            block.pdc().remove(placedKey)
            return
        }

        val xp = MiningXp.forBlock(block.type) ?: return
        event.player.awardSkillXp(SkillType.MINING, xp)
    }

    private fun Block.wasPlacedByPlayer() = pdc().has(placedKey)
}