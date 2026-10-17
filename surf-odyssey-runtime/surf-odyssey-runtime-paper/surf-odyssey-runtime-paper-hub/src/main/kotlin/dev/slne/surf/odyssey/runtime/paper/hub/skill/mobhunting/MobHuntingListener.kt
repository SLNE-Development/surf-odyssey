package dev.slne.surf.odyssey.runtime.paper.hub.skill.mobhunting

import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.common.skill.awardSkillXp
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.event.entity.EntityDeathEvent

object MobHuntingListener : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity is Player) return

        val killer = entity.killer ?: return
        val reason = entity.entitySpawnReason
        if (reason == SpawnReason.SPAWNER || reason == SpawnReason.SPAWNER_EGG) return

        if (!SkillWorld.allowsXp(SkillType.MOB_HUNTING, entity.location)) return

        val xp = MobHuntingXp.forEntity(entity.type) ?: return
        killer.awardSkillXp(SkillType.MOB_HUNTING, xp)
    }
}
