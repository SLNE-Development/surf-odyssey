package dev.slne.surf.odyssey.runtime.paper.progression.admin

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Tests the placement statements and the placement files the admin command writes. */
class PlacementWriterTest {
    @Test
    fun `npc statement rounds coordinates to one decimal and angles to whole degrees`() {
        val text = PlacementWriter.npc("old_smith", 12.3456, 64.0, -7.25, 91.6f, -12.4f)
        assertEquals("npc(Npcs.OLD_SMITH, at = SpawnPoint(12.3, 64.0, -7.3, 92f, -12f))\n", text)
    }

    @Test
    fun `npc statement never prints a negative zero`() {
        val text = PlacementWriter.npc("a", -0.04, 0.0, -0.0, -0.4f, 0f)
        assertEquals("npc(Npcs.A, at = SpawnPoint(0.0, 0.0, 0.0, 0f, 0f))\n", text)
    }

    @Test
    fun `station statement uses block coordinates`() {
        val text = PlacementWriter.station("forge2", 10, 70, -3)
        assertEquals("station(Stations.FORGE2, at = BlockPos(10, 70, -3))\n", text)
    }

    @Test
    fun `constant names are the upper snake case of the id`() {
        assertEquals("OLD_SMITH_2", PlacementWriter.constantName("old_smith_2"))
    }

    @Test
    fun `npc and station placements of one id use different files`() {
        assertEquals("npc_smith.kt", PlacementWriter.npcFileName("smith"))
        assertEquals("station_smith.kt", PlacementWriter.stationFileName("smith"))
    }

    @Test
    fun `an id outside the allowed characters is rejected`() {
        assertEquals(false, PlacementWriter.isValidId("Old-Smith"))
        assertEquals(true, PlacementWriter.isValidId("old_smith"))
    }

    @Test
    fun `saving writes the file and refuses to overwrite it`() {
        val folder = Files.createTempDirectory("placements")
        val first = PlacementFile.save(folder, PlacementWriter.npcFileName("smith"), "npc(Npcs.SMITH, at = SpawnPoint(1.0, 2.0, 3.0, 4f, 5f))\n")
        val saved = assertIs<PlacementFile.Result.Saved>(first)
        assertEquals("npc(Npcs.SMITH, at = SpawnPoint(1.0, 2.0, 3.0, 4f, 5f))\n", saved.file.readText())
        assertEquals(folder.resolve("placements").resolve("npc_smith.kt"), saved.file)
        assertIs<PlacementFile.Result.Exists>(PlacementFile.save(folder, PlacementWriter.npcFileName("smith"), "other"))
        assertEquals("npc(Npcs.SMITH, at = SpawnPoint(1.0, 2.0, 3.0, 4f, 5f))\n", saved.file.readText())
    }
}
