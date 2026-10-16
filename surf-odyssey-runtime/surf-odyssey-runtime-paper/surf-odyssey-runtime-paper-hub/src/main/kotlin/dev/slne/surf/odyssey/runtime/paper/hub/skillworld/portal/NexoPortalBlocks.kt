package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import com.nexomc.nexo.api.NexoBlocks
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import org.bukkit.block.Block

object NexoPortalBlocks {
    fun blockId(block: Block): String? = NexoBlocks.customBlockMechanic(block)?.itemID

    fun isPortalBlock(block: Block): Boolean = blockId(block) == SkillWorldSettings.config.portalBlock.nexoBlockId
}
