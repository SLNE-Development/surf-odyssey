package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.PortalKind
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.geometry.BlockBox
import org.bukkit.World

data class Portal(val id: String, val kind: PortalKind, val skill: SkillType?, val box: BlockBox) {
    fun world(): World? = if (PortalRouter.expectedWorldIsHub(kind)) SkillWorld.hubWorld() else SkillWorld.world
}
