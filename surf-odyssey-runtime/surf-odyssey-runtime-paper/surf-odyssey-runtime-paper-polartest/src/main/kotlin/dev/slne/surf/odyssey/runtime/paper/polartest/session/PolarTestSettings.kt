package dev.slne.surf.odyssey.runtime.paper.polartest.session

import dev.slne.surf.odyssey.runtime.paper.polartest.data.MarkedPositionsHandler
import dev.slne.surf.odyssey.runtime.paper.polartest.migration.RemoveCobblestone
import dev.slne.surf.odyssey.runtime.paper.polartest.migration.ReplaceMossyCobblestone
import dev.slne.surf.polarpaper.api.migration.WorldMigration
import dev.slne.surf.polarpaper.api.settings.PolarPaperSettingsBuilder
import org.bukkit.NamespacedKey

/** The game mode of the main test world. */
const val MAIN_GAME_MODE = "polartest"

/** The game mode of the short-lived worlds of the failure steps. */
const val FAIL_GAME_MODE = "polartest-fail"

/** Set in the persistent data of every chunk the live migration visited. */
val LIVE_MIGRATED_MARKER = NamespacedKey("polartest", "live-migrated")

/**
 * Settings of the main game mode at [version]: the migrations up to that version, entities and chunk data, CraftEngine
 * furniture and the marked positions.
 */
fun mainSettings(version: Int): PolarPaperSettingsBuilder.() -> Unit = {
    gameMode(MAIN_GAME_MODE) {
        this.version = version
        if (version >= 2) migration(RemoveCobblestone)
        if (version >= 3) migration(ReplaceMossyCobblestone)
    }
    handlers {
        entities()
        craftEngine()
        add(MarkedPositionsHandler)
    }
}

/** Settings of the failure game mode at [version] with no handlers and, if given, [extraMigration]. */
fun failSettings(version: Int, extraMigration: WorldMigration? = null): PolarPaperSettingsBuilder.() -> Unit = {
    gameMode(FAIL_GAME_MODE) {
        this.version = version
        extraMigration?.let { migration(it) }
    }
}
