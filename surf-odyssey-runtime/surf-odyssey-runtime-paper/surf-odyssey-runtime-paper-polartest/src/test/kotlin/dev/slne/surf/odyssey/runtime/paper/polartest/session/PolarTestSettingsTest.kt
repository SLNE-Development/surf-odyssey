package dev.slne.surf.odyssey.runtime.paper.polartest.session

import dev.slne.surf.odyssey.runtime.paper.polartest.data.MarkedPositionsHandler
import dev.slne.surf.polarpaper.api.migration.WorldMigration
import dev.slne.surf.polarpaper.api.settings.BuiltInHandler
import dev.slne.surf.polarpaper.api.settings.polarPaperSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks that the settings of the test game modes declare the right versions, migrations and handlers. */
class PolarTestSettingsTest {
    @Test
    fun `the main game mode carries the migrations up to its version`() {
        assertEquals(emptyList(), polarPaperSettings(mainSettings(1)).gameModes.getValue(MAIN_GAME_MODE).migrations.map { it.toVersion })
        assertEquals(listOf(2), polarPaperSettings(mainSettings(2)).gameModes.getValue(MAIN_GAME_MODE).migrations.map { it.toVersion })
        assertEquals(listOf(2, 3), polarPaperSettings(mainSettings(3)).gameModes.getValue(MAIN_GAME_MODE).migrations.map { it.toVersion })
    }

    @Test
    fun `the main game mode stores entities, chunk data, furniture and marked positions`() {
        val settings = polarPaperSettings(mainSettings(3))
        assertEquals(setOf(BuiltInHandler.ENTITIES, BuiltInHandler.CRAFT_ENGINE), settings.builtInHandlers)
        assertEquals(listOf(MarkedPositionsHandler), settings.handlers)
    }

    @Test
    fun `the fail game mode has no handlers and only the migration it is given`() {
        val broken = object : WorldMigration(2) {}
        val settings = polarPaperSettings(failSettings(2, broken))
        assertEquals(listOf(2), settings.gameModes.getValue(FAIL_GAME_MODE).migrations.map { it.toVersion })
        assertTrue(settings.handlers.isEmpty())
        assertTrue(settings.builtInHandlers.isEmpty())
    }
}
