package dev.slne.surf.odyssey.runtime.paper.progression.npc

import dev.slne.surf.api.core.util.mutableObjectSetOf
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.npc.api.SurfNpcApi
import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.npc.api.npc.rotation.NpcRotationType
import dev.slne.surf.npc.api.npc.skin.NpcSkin
import dev.slne.surf.npc.api.npc.skin.NpcSkinPart
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * An [NpcBackend] that spawns mannequins through the surf-npc plugin. Each NPC starts with an empty viewer set, so it
 * is seen only by the players added with [setVisible].
 *
 * Must be registered as a listener; interactions with its NPCs are passed to [onInteract] on the player's thread.
 *
 * @param onInteract called when a player interacts with an NPC of this backend
 */
class SurfNpcBackend(private val onInteract: (Player, SpawnedNpc) -> Unit) : NpcBackend, Listener {
    /** The spawned NPCs by their unique surf-npc name. */
    private val spawned = ConcurrentHashMap<String, SurfSpawnedNpc>()

    /** Numbers the spawned NPCs, so that the same definition can be placed more than once. */
    private val counter = AtomicInteger()

    /**
     * An NPC spawned through surf-npc.
     *
     * @property definition the definition the NPC was spawned from
     * @property uniqueName the unique surf-npc name of the NPC
     */
    private class SurfSpawnedNpc(override val definition: NpcDefinition, val uniqueName: String) : SpawnedNpc

    /** Spawns a non-persistent mannequin with the definition's name and skin that looks at each viewer. */
    override fun spawn(definition: NpcDefinition, location: Location): SpawnedNpc {
        val uniqueName = "$NAME_PREFIX${definition.id}_${counter.incrementAndGet()}"
        SurfNpcApi.getNpc(uniqueName)?.let(SurfNpcApi::deleteNpc)
        SurfNpcApi.createNpc(
            displayName = definition.name,
            uniqueName = uniqueName,
            type = EntityType.MANNEQUIN,
            location = location,
            viewers = mutableObjectSetOf<UUID>(),
            rotationType = NpcRotationType.PER_PLAYER,
            persistent = false,
            skin = NpcSkin(definition.id, definition.skin.value, definition.skin.signature, NpcSkinPart.entries.toObjectSet()),
        )
        return SurfSpawnedNpc(definition, uniqueName).also { spawned[uniqueName] = it }
    }

    /** Adds [player] to or removes them from the viewers of [npc] when that changes anything. */
    override fun setVisible(npc: SpawnedNpc, player: Player, visible: Boolean) {
        val handle = SurfNpcApi.getNpc((npc as SurfSpawnedNpc).uniqueName) ?: return
        val seen = handle.hasViewer(player.uniqueId)
        if (visible && !seen) handle.addViewer(player.uniqueId)
        if (!visible && seen) handle.removeViewer(player.uniqueId)
    }

    /** Deletes every NPC spawned by this backend. */
    override fun despawnAll() {
        spawned.keys.toList().forEach { name ->
            spawned.remove(name)
            SurfNpcApi.getNpc(name)?.let(SurfNpcApi::deleteNpc)
        }
    }

    /** Passes interactions with NPCs of this backend to [onInteract]. */
    @EventHandler
    fun onNpcInteract(event: NpcInteractEvent) {
        val npc = spawned[event.npc.uniqueName] ?: return
        onInteract(event.player, npc)
    }

    /** Holds the naming of the spawned NPCs. */
    private companion object {
        /** Prefix of the unique surf-npc names of the NPCs of this backend. */
        const val NAME_PREFIX = "odyssey_"
    }
}
