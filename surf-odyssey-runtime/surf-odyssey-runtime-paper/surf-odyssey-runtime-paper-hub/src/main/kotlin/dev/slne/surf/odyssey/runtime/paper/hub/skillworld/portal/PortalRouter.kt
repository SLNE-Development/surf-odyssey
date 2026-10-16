package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.PortalKind

sealed interface PortalTarget {
    data object HubSpawn : PortalTarget

    data class Arrival(val skill: SkillType) : PortalTarget
}

sealed interface PortalOutcome {
    data class Teleport(val target: PortalTarget) : PortalOutcome

    data object WorldUnavailable : PortalOutcome

    data object SkillLocked : PortalOutcome

    data object NoArrival : PortalOutcome
}

object PortalRouter {
    fun route(
        kind: PortalKind,
        skill: SkillType?,
        worldLoaded: Boolean,
        lockedSkills: Set<SkillType>,
        arrivals: Set<SkillType>,
    ): PortalOutcome = when (kind) {
        PortalKind.EXIT -> PortalOutcome.Teleport(PortalTarget.HubSpawn)
        PortalKind.ENTRY -> when {
            skill == null -> PortalOutcome.NoArrival
            !worldLoaded -> PortalOutcome.WorldUnavailable
            skill in lockedSkills -> PortalOutcome.SkillLocked
            skill !in arrivals -> PortalOutcome.NoArrival
            else -> PortalOutcome.Teleport(PortalTarget.Arrival(skill))
        }
    }

    fun expectedWorldIsHub(kind: PortalKind): Boolean = kind == PortalKind.ENTRY
}
