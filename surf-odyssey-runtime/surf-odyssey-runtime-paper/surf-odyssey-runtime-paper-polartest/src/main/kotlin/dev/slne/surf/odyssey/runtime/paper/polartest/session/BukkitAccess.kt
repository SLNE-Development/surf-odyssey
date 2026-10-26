package dev.slne.surf.odyssey.runtime.paper.polartest.session

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import dev.slne.surf.odyssey.runtime.paper.polartest.PolarTestPaperMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World

/** Runs [block] on the main thread and returns its result. */
suspend fun <T> PolarTestPaperMain.onMain(block: () -> T): T =
    if (Bukkit.isPrimaryThread()) block() else withContext(globalRegionDispatcher) { block() }

/** Suspends until [ticks] server ticks have passed. */
suspend fun ticks(ticks: Int) {
    val target = Bukkit.getCurrentTick() + ticks
    while (Bukkit.getCurrentTick() < target) delay(5)
}

/**
 * Loads the chunk at [x], [z] of [world] and holds it with a plugin ticket. Without a ticket Paper unloads it a few
 * ticks later, and a chunk that unloaded is not part of the next save of a Polar world.
 */
suspend fun PolarTestPaperMain.loadChunk(world: World, x: Int, z: Int): Chunk {
    val chunk = world.getChunkAtAsync(x, z).await()
    onMain { world.addPluginChunkTicket(x, z, this) }
    return chunk
}

/** Checks [condition] on the main thread once per tick and returns whether it held within [timeoutTicks]. */
suspend fun PolarTestPaperMain.awaitTrue(timeoutTicks: Int = 100, condition: () -> Boolean): Boolean {
    repeat(timeoutTicks) {
        if (onMain(condition)) return true
        ticks(1)
    }
    return onMain(condition)
}
