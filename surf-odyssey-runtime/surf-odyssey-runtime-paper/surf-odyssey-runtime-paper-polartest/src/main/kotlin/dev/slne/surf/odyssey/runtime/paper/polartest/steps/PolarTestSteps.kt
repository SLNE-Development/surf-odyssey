package dev.slne.surf.odyssey.runtime.paper.polartest.steps

import dev.slne.surf.odyssey.runtime.paper.polartest.PolarTestPaperMain
import dev.slne.surf.odyssey.runtime.paper.polartest.check.CheckReport
import dev.slne.surf.odyssey.runtime.paper.polartest.data.ChunkRef
import dev.slne.surf.odyssey.runtime.paper.polartest.data.MarkedPositionsRegistry
import dev.slne.surf.odyssey.runtime.paper.polartest.layout.BlockPos
import dev.slne.surf.odyssey.runtime.paper.polartest.layout.TestLayout
import dev.slne.surf.odyssey.runtime.paper.polartest.session.FAIL_GAME_MODE
import dev.slne.surf.odyssey.runtime.paper.polartest.session.LIVE_MIGRATED_MARKER
import dev.slne.surf.odyssey.runtime.paper.polartest.session.MAIN_GAME_MODE
import dev.slne.surf.odyssey.runtime.paper.polartest.session.PolarTestSession
import dev.slne.surf.odyssey.runtime.paper.polartest.session.awaitTrue
import dev.slne.surf.odyssey.runtime.paper.polartest.session.failSettings
import dev.slne.surf.odyssey.runtime.paper.polartest.session.loadChunk
import dev.slne.surf.odyssey.runtime.paper.polartest.session.onMain
import dev.slne.surf.odyssey.runtime.paper.polartest.session.ticks
import dev.slne.surf.polarpaper.api.SurfPolarPaperApi
import dev.slne.surf.polarpaper.api.migration.RawWorld
import dev.slne.surf.polarpaper.api.migration.WorldMigration
import dev.slne.surf.polarpaper.api.world.WorldMigrationException
import dev.slne.surf.polarpaper.api.world.WorldVersionTooNewException
import kotlinx.coroutines.future.await
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/** The test steps. Each one returns a [CheckReport] with every expectation it checked. */
class PolarTestSteps(private val plugin: PolarTestPaperMain, private val session: PolarTestSession) {

    /** A raw migration to version 2 that changes a block and then throws, so a leaked partial result would show. */
    private object BrokenMigration : WorldMigration(2) {
        /** Puts a diamond block onto the floor of chunk 0, 0 and throws. */
        override fun migrateRaw(world: RawWorld) {
            world.chunk(0, 0)?.setBlock(0, TestLayout.FLOOR_Y, 0, "minecraft:diamond_block")
            throw IllegalStateException("polartest: the migration to version 2 is broken on purpose")
        }
    }

    /** Runs [step] and returns its report. */
    suspend fun run(step: PolarTestStep): CheckReport = when (step) {
        PolarTestStep.SETUP -> setup()
        PolarTestStep.HANDLER -> handler()
        PolarTestStep.MIGRATE_RAW -> migrateRaw()
        PolarTestStep.MIGRATE_LIVE -> migrateLive()
        PolarTestStep.FURNITURE -> furniture()
        PolarTestStep.FAIL_NEWER -> failNewer()
        PolarTestStep.FAIL_THROW -> failThrow()
        PolarTestStep.CLEANUP -> cleanup()
    }

    /** Teleports [player] to the spawn of the main test world when that world is loaded. */
    suspend fun showWorld(player: Player) {
        val world = session.loadedMainWorld ?: return
        plugin.onMain { player.teleportAsync(TestLayout.spawn(world.bukkitWorld)) }.await()
    }

    /** Replaces any previous test world with a new one at version 1 holding the full layout and the furniture. */
    private suspend fun setup(): CheckReport {
        val report = CheckReport(PolarTestStep.SETUP.id)
        try {
            session.deleteMainWorld()
        } catch (exception: Exception) {
            plugin.logger.warning("Could not delete the previous test world: $exception")
            session.worldUuid = null
        }

        val api = session.reopen(1)
        val uuid = UUID.randomUUID()
        val world = api.createWorld(uuid, MAIN_GAME_MODE)
        session.worldUuid = uuid
        val bukkit = world.bukkitWorld

        for ((cx, cz) in TestLayout.chunks) buildChunk(bukkit, cx, cz)
        val placed = plugin.onMain {
            for ((cx, cz) in TestLayout.chunks) {
                MarkedPositionsRegistry.set(ChunkRef(bukkit.uid, cx, cz), TestLayout.cobblestone(cx, cz))
            }
            FurnitureAccess.place(TestLayout.furniture(bukkit))
        }
        report.expectTrue("CraftEngine hat ${FurnitureAccess.FURNITURE_ID} platziert", placed)
        // Colliders are spawned a few ticks after the base entity
        ticks(10)

        api.saveWorld(world)
        report.expect("Version nach dem Anlegen", 1, world.version)
        checkBlocks(report, bukkit, "nach dem Anlegen", Material.COBBLESTONE, Material.MOSSY_COBBLESTONE)
        return report
    }

    /** Unloads and reloads one test chunk and checks that the handler released and restored its positions. */
    private suspend fun handler(): CheckReport {
        val report = CheckReport(PolarTestStep.HANDLER.id)
        val world = session.loadedMainWorld ?: session.openMainWorld()
        val bukkit = world.bukkitWorld
        session.evacuate(bukkit)
        checkMarkedPositions(report, bukkit, "nach dem Laden")

        val (cx, cz) = TestLayout.chunks.last()
        val ref = ChunkRef(bukkit.uid, cx, cz)
        plugin.onMain {
            bukkit.removePluginChunkTicket(cx, cz, plugin)
            bukkit.unloadChunk(cx, cz, true)
        }
        report.expectTrue("Chunk $cx, $cz wurde entladen", plugin.awaitTrue { !bukkit.isChunkLoaded(cx, cz) })
        report.expect("Positionen von Chunk $cx, $cz nach dem Entladen freigegeben", null, MarkedPositionsRegistry.get(ref))

        plugin.loadChunk(bukkit, cx, cz)
        val restored = plugin.awaitTrue { MarkedPositionsRegistry.get(ref) == TestLayout.cobblestone(cx, cz) }
        report.expectTrue(
            "Positionen von Chunk $cx, $cz nach dem Neuladen wiederhergestellt (gefunden: ${MarkedPositionsRegistry.get(ref)})",
            restored
        )
        return report
    }

    /** Opens the main world at version 2 twice and checks that the cobblestone is gone both times. */
    private suspend fun migrateRaw(): CheckReport {
        val report = CheckReport(PolarTestStep.MIGRATE_RAW.id)
        for (moment in listOf("direkt nach der Migration", "nach erneutem Öffnen")) {
            val world = session.openMainWorld(2)
            report.expect("Version $moment", 2, world.version)
            checkBlocks(report, world.bukkitWorld, moment, Material.AIR, Material.MOSSY_COBBLESTONE)
            checkMarkedPositions(report, world.bukkitWorld, moment)
        }
        return report
    }

    /** Opens the main world at version 3 twice and checks the stone and the live marker of every test chunk. */
    private suspend fun migrateLive(): CheckReport {
        val report = CheckReport(PolarTestStep.MIGRATE_LIVE.id)
        for (moment in listOf("direkt nach der Migration", "nach erneutem Öffnen")) {
            val world = session.openMainWorld(3)
            report.expect("Version $moment", 3, world.version)
            checkBlocks(report, world.bukkitWorld, moment, Material.AIR, Material.STONE)
            checkMarkedPositions(report, world.bukkitWorld, moment)
            val bukkit = world.bukkitWorld
            plugin.onMain {
                for ((cx, cz) in TestLayout.chunks) {
                    val marker = bukkit.getChunkAt(cx, cz).persistentDataContainer
                        .get(LIVE_MIGRATED_MARKER, PersistentDataType.BOOLEAN)
                    report.expect("Live-Marker in Chunk $cx, $cz $moment", true, marker)
                }
            }
        }
        return report
    }

    /** Reopens the main world and checks that the test furniture exists exactly once, live and at its spot. */
    private suspend fun furniture(): CheckReport {
        val report = CheckReport(PolarTestStep.FURNITURE.id)
        val bukkit = session.openMainWorld().bukkitWorld
        val chunk = plugin.loadChunk(bukkit, 0, 0)
        report.expectTrue(
            "Test-Möbel nach dem Neuladen wieder da",
            plugin.awaitTrue { FurnitureAccess.furnitureIn(chunk).isNotEmpty() }
        )
        // A duplicate would be placed a tick later, or by a second restore
        ticks(40)

        val spot = TestLayout.furniture(bukkit)
        val expected = listOf(FurnitureAccess.FoundFurniture(FurnitureAccess.FURNITURE_ID, spot.blockX, spot.blockY, spot.blockZ))
        report.expect("Test-Möbel in Chunk 0, 0", expected, plugin.onMain { FurnitureAccess.furnitureIn(chunk) })
        return report
    }

    /** Stores a failure world at version 5 and checks that version 4 refuses it and version 5 loads it unchanged. */
    private suspend fun failNewer(): CheckReport {
        val report = CheckReport(PolarTestStep.FAIL_NEWER.id)
        val uuid = createFailWorld(5)

        withFailApi(4) { older ->
            try {
                older.loadWorld(uuid, FAIL_GAME_MODE)
                report.fail("Laden mit Version 4 wurde nicht abgelehnt")
            } catch (exception: WorldVersionTooNewException) {
                report.expect("Gespeicherte Version in der Exception", 5, exception.storedVersion)
                report.expect("Aktuelle Version in der Exception", 4, exception.currentVersion)
            }
            report.expectTrue("Abgelehnte Welt ist nicht geladen", older.findWorld(uuid, FAIL_GAME_MODE) == null)
        }
        verifyFailWorldUnchanged(report, uuid, 5)
        return report
    }

    /** Stores a failure world at version 1 and checks that a throwing migration aborts and leaves it unchanged. */
    private suspend fun failThrow(): CheckReport {
        val report = CheckReport(PolarTestStep.FAIL_THROW.id)
        val uuid = createFailWorld(1)

        withFailApi(2, BrokenMigration) { updated ->
            try {
                updated.loadWorld(uuid, FAIL_GAME_MODE)
                report.fail("Eine werfende Migration hat das Laden nicht abgebrochen")
            } catch (exception: WorldMigrationException) {
                report.expect("Version in der Exception", 2, exception.toVersion)
            }
            report.expectTrue("Abgebrochene Welt ist nicht geladen", updated.findWorld(uuid, FAIL_GAME_MODE) == null)
        }
        verifyFailWorldUnchanged(report, uuid, 1)
        return report
    }

    /** Deletes the main test world and closes the instance. */
    private suspend fun cleanup(): CheckReport {
        val report = CheckReport(PolarTestStep.CLEANUP.id)
        session.deleteMainWorld()
        session.closeApi()
        report.expect("Testwelt vergessen", null, session.worldUuid)
        return report
    }

    /** Creates a failure world at [version] with the layout of chunk 0, 0, saves it and returns its uuid. */
    private suspend fun createFailWorld(version: Int): UUID {
        val uuid = UUID.randomUUID()
        withFailApi(version) { api ->
            val world = api.createWorld(uuid, FAIL_GAME_MODE)
            buildChunk(world.bukkitWorld, 0, 0)
            api.saveWorld(world)
        }
        return uuid
    }

    /** Loads the failure world at [version], checks version and blocks, then deletes it. */
    private suspend fun verifyFailWorldUnchanged(report: CheckReport, uuid: UUID, version: Int) {
        withFailApi(version) { api ->
            val world = api.loadWorld(uuid, FAIL_GAME_MODE)
            report.expect("Gespeicherte Version danach", version, world.version)
            val bukkit = world.bukkitWorld
            plugin.loadChunk(bukkit, 0, 0)
            plugin.onMain {
                TestLayout.cobblestone(0, 0).forEach {
                    report.expect("Cobblestone bei $it danach", Material.COBBLESTONE, bukkit.blockAt(it).type)
                }
                report.expect("Boden bei 0, ${TestLayout.FLOOR_Y}, 0 danach", Material.STONE, bukkit.getBlockAt(0, TestLayout.FLOOR_Y, 0).type)
            }
            world.delete()
        }
    }

    /** Runs [block] with a short-lived instance that owns [FAIL_GAME_MODE] at [version], and closes it afterwards. */
    private suspend fun withFailApi(version: Int, migration: WorldMigration? = null, block: suspend (SurfPolarPaperApi) -> Unit) {
        val api = SurfPolarPaperApi.create(plugin, failSettings(version, migration))
        try {
            block(api)
        } finally {
            for (world in api.loadedWorlds.toList()) session.evacuate(world.bukkitWorld)
            api.unloadAllWorlds()
            api.close()
        }
    }

    /** Loads the chunk at [chunkX], [chunkZ] and builds its stone floor, cobblestone and mossy cobblestone. */
    private suspend fun buildChunk(world: World, chunkX: Int, chunkZ: Int) {
        plugin.loadChunk(world, chunkX, chunkZ)
        plugin.onMain {
            for (x in 0..15) for (z in 0..15) {
                world.getBlockAt(chunkX * 16 + x, TestLayout.FLOOR_Y, chunkZ * 16 + z).setType(Material.STONE, false)
            }
            TestLayout.cobblestone(chunkX, chunkZ).forEach { world.blockAt(it).setType(Material.COBBLESTONE, false) }
            TestLayout.mossy(chunkX, chunkZ).forEach { world.blockAt(it).setType(Material.MOSSY_COBBLESTONE, false) }
        }
    }

    /** Checks the cobblestone spots for [cobblestoneSpot], the mossy spots for [mossySpot] and every floor. */
    private suspend fun checkBlocks(report: CheckReport, world: World, moment: String, cobblestoneSpot: Material, mossySpot: Material) {
        for ((cx, cz) in TestLayout.chunks) plugin.loadChunk(world, cx, cz)
        plugin.onMain {
            for ((cx, cz) in TestLayout.chunks) {
                TestLayout.cobblestone(cx, cz).forEach { report.expect("Cobblestone-Platz $it $moment", cobblestoneSpot, world.blockAt(it).type) }
                TestLayout.mossy(cx, cz).forEach { report.expect("Mossy-Platz $it $moment", mossySpot, world.blockAt(it).type) }
                report.expect("Boden von Chunk $cx, $cz $moment", Material.STONE, world.getBlockAt(cx * 16, TestLayout.FLOOR_Y, cz * 16).type)
            }
        }
    }

    /** Waits until the handler restored the marked positions of every test chunk and checks them. */
    private suspend fun checkMarkedPositions(report: CheckReport, world: World, moment: String) {
        for ((cx, cz) in TestLayout.chunks) {
            plugin.loadChunk(world, cx, cz)
            val ref = ChunkRef(world.uid, cx, cz)
            val restored = plugin.awaitTrue { MarkedPositionsRegistry.get(ref) == TestLayout.cobblestone(cx, cz) }
            report.expectTrue(
                "Markierte Positionen von Chunk $cx, $cz $moment (gefunden: ${MarkedPositionsRegistry.get(ref)})",
                restored
            )
        }
    }

    /** The block at [pos]. */
    private fun World.blockAt(pos: BlockPos): Block = getBlockAt(pos.x, pos.y, pos.z)
}
