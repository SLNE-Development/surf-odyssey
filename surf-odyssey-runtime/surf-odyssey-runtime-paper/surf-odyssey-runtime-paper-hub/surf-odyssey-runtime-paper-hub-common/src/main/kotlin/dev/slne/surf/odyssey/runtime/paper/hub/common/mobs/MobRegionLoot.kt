package dev.slne.surf.odyssey.runtime.paper.hub.common.mobs

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.gate.GateAccess
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.progression.vanilla.MobLoot
import dev.slne.surf.odyssey.runtime.paper.progression.vanilla.MobLootProvider
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import kotlin.random.Random

/**
 * Supplies the code loot of mob spawn regions: a mob killed by a player drops one roll of the loot table of the region
 * that spawned it, or, if no region spawned it, of the mob spawn region it dies in. Nothing drops when no player
 * killed the mob or when the region's gate locks the killer out. Every other entity drops nothing.
 */
object MobRegionLoot : MobLootProvider {
    /** Knows no loot by type alone; loot depends on the dying entity. */
    override fun lootFor(entityType: EntityType): MobLoot? = null

    /**
     * Returns one roll of the loot table of the mob region of [entity] without experience, or `null` if it has
     * none.
     */
    override fun lootFor(entity: Entity): MobLoot? {
        val hub = HubPlugin.instance
        val location = entity.location
        if (location.world != hub.hubWorld) return null
        val killer = (entity as? LivingEntity)?.killer
        val deathRegion = hub.regionIndex.regionAt(location.blockX, location.blockY, location.blockZ)
        val region = lootRegion(
            hub.regionIndex.regionsOfType<MobSpawnRegion>(),
            { it.hasSpawned(entity.uniqueId) },
            deathRegion,
            admits = killer?.let { player -> { region -> !GateAccess.isLockedOut(player, region) } },
        ) ?: return null
        return MobLoot(region.loot.drops(Random.Default), 0)
    }

    /**
     * Returns the mob spawn region whose loot a dying mob drops for its killer.
     *
     * @param regions the mob spawn regions of the world
     * @param spawnedBy returns whether a region spawned the dying mob
     * @param deathRegion the region the mob dies in, or `null` outside every region
     * @param admits returns whether a region's gate lets the killing player act in it, or `null` if no player
     *        killed the mob
     * @return the region found by [regionOf] if a player killed the mob and the region admits them, otherwise `null`
     */
    fun lootRegion(
        regions: List<Region>,
        spawnedBy: (MobSpawnRegion) -> Boolean,
        deathRegion: Region?,
        admits: ((MobSpawnRegion) -> Boolean)?,
    ): MobSpawnRegion? {
        if (admits == null) return null
        return regionOf(regions, spawnedBy, deathRegion)?.takeIf(admits)
    }

    /**
     * Returns the mob spawn region a dying mob belongs to.
     *
     * @param regions the mob spawn regions of the world
     * @param spawnedBy returns whether a region spawned the dying mob
     * @param deathRegion the region the mob dies in, or `null` outside every region
     * @return the first region that spawned the mob, otherwise [deathRegion] if it is a mob spawn region, otherwise
     *         `null`
     */
    fun regionOf(regions: List<Region>, spawnedBy: (MobSpawnRegion) -> Boolean, deathRegion: Region?): MobSpawnRegion? =
        regions.filterIsInstance<MobSpawnRegion>().firstOrNull(spawnedBy) ?: deathRegion as? MobSpawnRegion
}
