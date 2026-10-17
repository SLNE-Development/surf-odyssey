package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.listener.MiningLockListener
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.listener.SkillWorldPlayerListener
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.listener.SkillWorldProtectionListener
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining.MiningReset
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mobhunting.MobHuntingSpawner
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.portal.Portals
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.selection.SelectionWand
import java.util.logging.Level

object SkillWorldBootstrap {
    @Volatile
    private var done = false

    fun setup(): Boolean {
        if (done) return true

        if (!SkillWorldSettings.isLoaded) {
            try {
                SkillWorldSettings.load()
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to read skill-world.yml, the skill world stays disabled until /skillworld reload succeeds", e)
                return false
            }
        }

        SkillWorld.refreshRegions()
        Portals.refresh()
        SkillWorld.register(MiningReset)
        SkillWorld.register(MobHuntingSpawner)
        MiningLockListener.register()
        SkillWorldProtectionListener.register()
        SkillWorldPlayerListener.register()
        SelectionWand.register()
        SelectionWand.startOutline()

        done = true
        return true
    }
}
