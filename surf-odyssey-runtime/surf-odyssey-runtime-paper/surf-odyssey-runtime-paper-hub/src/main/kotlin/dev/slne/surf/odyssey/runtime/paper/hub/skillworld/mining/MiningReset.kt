package dev.slne.surf.odyssey.runtime.paper.hub.skillworld.mining

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.core.common.skill.SkillType
import dev.slne.surf.odyssey.runtime.paper.hub.plugin
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillLocks
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorld
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.SkillWorldLifecycle
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.SkillWorldSettings
import dev.slne.surf.odyssey.runtime.paper.hub.skillworld.config.effectiveOres
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.future.await
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import java.lang.ref.WeakReference
import java.util.logging.Level
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object MiningReset : SkillWorldLifecycle {
    @Volatile
    private var snapshot: MiningSnapshot? = null

    @Volatile
    private var lastWorld: WeakReference<World>? = null

    @Volatile
    private var cycle: MiningResetCycle? = null

    @Volatile
    private var abortRequested = false

    @Volatile
    private var resetInProgress = false

    @Volatile
    private var tables: Map<Material, WeightedTable<Material>> = emptyMap()

    enum class TriggerResult {
        Started,

        AlreadyRunning,

        NoCycle,
    }

    override fun onLoaded(world: World) {
        if (!resetInProgress) abortRequested = false
        cycle?.stop()
        cycle = null

        val box = SkillWorld.regions.box(SkillType.MINING) ?: run {
            snapshot = null
            plugin.logger.warning("No mining region configured, the mining reset is disabled")
            return
        }

        val mining = SkillWorldSettings.config.mining
        tables = oreTables(mining.effectiveOres())

        val kept = snapshot?.takeIf { lastWorld?.get() === world && it.box == box }
        val captured = kept ?: MiningSnapshot.capture(world, box)
        snapshot = captured
        if (kept == null) distributeOres(world, captured)
        lastWorld = WeakReference(world)

        cycle = MiningResetCycle(
            scope = plugin.scope + plugin.globalRegionDispatcher,
            interval = mining.resetIntervalMinutes.coerceAtLeast(1).minutes,
            warnings = warningTimes(mining.warningSeconds),
            warn = ::warn,
            reset = ::reset,
            onFailure = { plugin.logger.log(Level.SEVERE, "The mining reset failed", it) },
        ).also { it.start() }
    }

    override fun onUnloading(world: World) {
        abortRequested = true
        cycle?.stop()
        cycle = null
        if (!resetInProgress) SkillLocks.unlock(SkillType.MINING)
    }

    fun triggerNow(): TriggerResult {
        if (resetInProgress) return TriggerResult.AlreadyRunning
        val running = cycle ?: return TriggerResult.NoCycle
        return if (running.triggerNow()) TriggerResult.Started else TriggerResult.AlreadyRunning
    }

    private suspend fun warn(remaining: Duration) {
        val seconds = remaining.inWholeSeconds.toInt()
        val counting = seconds <= 5
        playersInRegion().forEach { player ->
            if (counting) {
                player.sendText {
                    appendInfoPrefix()
                    info("Mining-Reset in ")
                    variableValue("$seconds Sekunden.")
                }
                player.playSound(true) {
                    type(Sound.BLOCK_NOTE_BLOCK_PLING)
                    volume(.7f)
                    pitch(countdownPitch(seconds))
                }
            } else {
                player.sendText {
                    appendInfoPrefix()
                    info("Die Mining-Höhle wird in ")
                    variableValue("$seconds Sekunden")
                    info(" zurückgesetzt.")
                }
                player.playSound(true) {
                    type(Sound.BLOCK_NOTE_BLOCK_HAT)
                    volume(.5f)
                    pitch(1.0f)
                }
            }
        }
    }

    private suspend fun reset() {
        if (resetInProgress) return
        val world = SkillWorld.world ?: return
        val captured = snapshot ?: return

        resetInProgress = true
        SkillLocks.lock(SkillType.MINING)
        try {
            abortRequested = false
            val shouldAbort = { abortRequested || SkillWorld.world !== world }
            withContext(NonCancellable) {
                val spawn = SkillWorld.hubSpawn()
                playersInRegion().forEach { it.teleportAsync(spawn).await() }
                val restored = !shouldAbort() &&
                    captured.restore(world, SkillWorldSettings.config.mining.blocksPerTick, shouldAbort)
                if (restored && !shouldAbort()) distributeOres(world, captured)
            }
        } finally {
            SkillLocks.unlock(SkillType.MINING)
            resetInProgress = false
        }
    }

    private fun distributeOres(world: World, captured: MiningSnapshot) {
        val mining = SkillWorldSettings.config.mining

        OreDistributor.distribute(captured.materials(), mining.oreChance, tables, Random.Default).forEach { (index, ore) ->
            val pos = captured.positionAt(index)
            world.getBlockAt(pos.x, pos.y, pos.z).setType(ore, false)
        }
    }

    private fun oreTables(config: Map<String, Map<String, Int>>): Map<Material, WeightedTable<Material>> =
        config.mapNotNull { (carrierName, weights) ->
            val carrier = Material.matchMaterial(carrierName) ?: run {
                plugin.logger.warning("Unknown carrier block $carrierName in skill-world.yml")
                return@mapNotNull null
            }
            val ores = weights.entries.mapNotNull { (oreName, weight) ->
                val ore = Material.matchMaterial(oreName)
                when {
                    ore == null -> { plugin.logger.warning("Unknown ore $oreName in skill-world.yml"); null }
                    weight <= 0 -> { plugin.logger.warning("Ore $oreName in skill-world.yml needs a positive weight"); null }
                    else -> ore to weight
                }
            }.toMap()

            if (ores.isEmpty()) null else carrier to WeightedTable(ores)
        }.toMap()

    private fun playersInRegion(): List<Player> {
        val world = SkillWorld.world ?: return emptyList()
        val box = snapshot?.box ?: return emptyList()
        return world.players.filter { box.contains(it.location.x, it.location.y, it.location.z) }
    }
}
