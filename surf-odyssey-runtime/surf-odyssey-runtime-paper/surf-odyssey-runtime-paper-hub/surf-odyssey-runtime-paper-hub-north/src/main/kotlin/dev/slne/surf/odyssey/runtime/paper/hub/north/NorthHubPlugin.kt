package dev.slne.surf.odyssey.runtime.paper.hub.north

import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.RegionRegistry
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.shape
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.Portal
import dev.slne.surf.odyssey.runtime.paper.hub.common.travel.WorldPosition
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Plugin of the North hub world. */
class NorthHubPlugin : HubPlugin() {
    /** Shown to players as "Norden". */
    override val worldDisplayName: Component = Component.text("Norden")

    /** Declares North's regions. */
    override fun RegionRegistry.regions() = northRegions()

    /** Declares North's portals. */
    override fun portals() = northPortals()
}

/** Declares the regions of North: three forests, two mines, a lake, the ruins and a wolf area. Coordinates are placeholders until the map is in place. */
fun RegionRegistry.northRegions() {
    forest("north_forest_spruce") {
        displayName = Component.text("Fichtenhain")
        shape { prism(minY = 60, maxY = 110) { point(100, 100); point(160, 100); point(160, 160); point(100, 160) } }
        breakable(Material.SPRUCE_LOG, Material.SPRUCE_LEAVES)
        regenerateAfter = 3.minutes
    }
    forest("north_forest_birch") {
        displayName = Component.text("Birkenwald")
        shape { prism(minY = 60, maxY = 110) { point(200, 100); point(260, 100); point(260, 160); point(200, 160) } }
        breakable(Material.BIRCH_LOG, Material.BIRCH_LEAVES)
        regenerateAfter = 3.minutes
    }
    forest("north_forest_oak") {
        displayName = Component.text("Eichenwald")
        shape { prism(minY = 60, maxY = 110) { point(300, 100); point(360, 100); point(360, 160); point(300, 160) } }
        breakable(Material.OAK_LOG, Material.OAK_LEAVES)
        regenerateAfter = 3.minutes
    }
    mine("north_mine_ridge") {
        displayName = Component.text("Frostgrat")
        shape { cuboid(100, 20, 300, 140, 59, 340) }
        breakable(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.COAL_ORE)
        regenerateAfter = 5.minutes
    }
    mine("north_mine_deep") {
        displayName = Component.text("Tiefe Grube")
        shape { cuboid(200, -40, 300, 240, 10, 340) }
        breakable(Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE)
        placeholder = Material.DEEPSLATE
        regenerateAfter = 10.minutes
    }
    fishing("north_lake") {
        displayName = Component.text("Eissee")
        shape { prism(minY = 50, maxY = 64) { point(400, 100); point(460, 100); point(460, 160); point(400, 160) } }
        fish("frost_trout", weight = 30) { night(); item { ItemStack(Material.SALMON) } }
        fish("lake_perch", weight = 70) { item { ItemStack(Material.COD) } }
    }
    region(NorthRuinsRegion())
    mobSpawn("north_wolves") {
        displayName = Component.text("Wolfsgebiet")
        shape { cuboid(600, 60, 100, 640, 90, 140) }
        mobs(EntityType.WOLF)
        spawnPoint(610, 64, 110)
        spawnPoint(620, 64, 120)
        spawnPoint(630, 64, 130)
        maxAlive = 4
        respawnInterval = 30.seconds
    }
}

/** The portals of North: one back to the lobby. Coordinates are placeholders until the map is in place. */
fun northPortals() = listOf(
    Portal("to_lobby", shape { cuboid(-2, 64, -2, 2, 67, -2) }, "odyssey-hub-lobby", WorldPosition(0.5, 64.0, 0.5, 0f, 0f)),
)
