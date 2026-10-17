package dev.slne.surf.odyssey.runtime.paper.hub.skillworld

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.generator.ChunkGenerator
import java.util.Random

class VoidChunkGenerator : ChunkGenerator() {
    override fun getFixedSpawnLocation(world: World, random: Random): Location = Location(world, 0.5, 64.0, 0.5)
}
