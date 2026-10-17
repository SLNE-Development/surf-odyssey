package dev.slne.surf.odyssey.runtime.paper.hub.common.editor

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPermissions
import dev.slne.surf.odyssey.runtime.paper.hub.common.HubPlugin
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.Region
import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.BlockPoint
import org.bukkit.entity.Player

/**
 * Registers `/hubregion`, the in-game region editor: `create <id>`, `prism <minY> <maxY>`,
 * `cuboid`, `exclude prism|cuboid`, `point`, `undo`, `confirm`, `cancel` and `wand`.
 * `point` adds the block the player stands on; `wand` hands out the [RegionWand]. `confirm` writes the shape block to
 * `regions/<id>.kt` in the plugin's data folder and refuses to overwrite an existing file; the session
 * stays open when saving does not succeed.
 */
fun regionEditorCommand() = commandTree("hubregion") {
    withPermission(HubPermissions.REGION_EDITOR)
    literalArgument("create") {
        stringArgument("id") {
            playerExecutor { player, args ->
                val id = args["id"] as String
                if (!Region.ID_PATTERN.matches(id)) return@playerExecutor fail(player, "Erlaubt sind Kleinbuchstaben, Ziffern und Unterstriche.")
                EditorSessions.open(player.uniqueId, id)
                player.sendText {
                    appendSuccessPrefix()
                    success("Region ")
                    variableValue(id)
                    success(" wird bearbeitet. Starte einen Teil mit /hubregion prism oder cuboid.")
                }
            }
        }
    }
    literalArgument("prism") {
        integerArgument("minY") {
            integerArgument("maxY") {
                playerExecutor { player, args ->
                    withSession(player) {
                        it.startPrism(args["minY"] as Int, args["maxY"] as Int, excluded = false)
                        ok(player, "Prisma begonnen.")
                    }
                }
            }
        }
    }
    literalArgument("cuboid") {
        playerExecutor { player, _ -> withSession(player) { it.startCuboid(excluded = false); ok(player, "Quader begonnen.") } }
    }
    literalArgument("exclude") {
        literalArgument("prism") {
            integerArgument("minY") {
                integerArgument("maxY") {
                    playerExecutor { player, args ->
                        withSession(player) {
                            it.startPrism(args["minY"] as Int, args["maxY"] as Int, excluded = true)
                            ok(player, "Aussparung (Prisma) begonnen.")
                        }
                    }
                }
            }
        }
        literalArgument("cuboid") {
            playerExecutor { player, _ -> withSession(player) { it.startCuboid(excluded = true); ok(player, "Aussparung (Quader) begonnen.") } }
        }
    }
    literalArgument("point") {
        playerExecutor { player, _ ->
            withSession(player) { session ->
                val block = player.location.block.getRelative(0, -1, 0)
                if (session.addPoint(BlockPoint(block.x, block.y, block.z))) ok(player, "Punkt ${block.x} ${block.y} ${block.z} gesetzt.")
                else fail(player, "Starte zuerst einen Teil. Ein Quader hat höchstens zwei Ecken.")
            }
        }
    }
    literalArgument("undo") {
        playerExecutor { player, _ -> withSession(player) { if (it.undo()) ok(player, "Rückgängig gemacht.") else fail(player, "Nichts zum Rückgängigmachen.") } }
    }
    literalArgument("wand") {
        playerExecutor { player, _ ->
            player.inventory.addItem(RegionWand.create()).values.forEach { player.world.dropItem(player.location, it) }
            ok(player, "Rechtsklick auf einen Block setzt einen Punkt, Linksklick entfernt den letzten.")
        }
    }
    literalArgument("cancel") {
        playerExecutor { player, _ -> EditorSessions.close(player.uniqueId); ok(player, "Bearbeitung verworfen.") }
    }
    literalArgument("confirm") {
        playerExecutor { player, _ ->
            withSession(player) { session ->
                val problems = session.problems()
                if (problems.isNotEmpty()) return@withSession fail(player, problems.joinToString(" "))
                when (val result = RegionFile.save(HubPlugin.instance.dataPath, session)) {
                    is RegionFile.Result.Saved -> {
                        EditorSessions.close(player.uniqueId)
                        ok(player, "Gespeichert in ${result.file.toAbsolutePath()}.")
                    }
                    is RegionFile.Result.Exists -> fail(
                        player,
                        "${result.file.toAbsolutePath()} existiert bereits und wird nicht überschrieben. Lösche oder verschiebe die Datei zuerst.",
                    )
                    is RegionFile.Result.Failed -> fail(
                        player,
                        "Speichern in ${result.file.toAbsolutePath()} fehlgeschlagen: ${result.error.message ?: result.error.javaClass.simpleName}",
                    )
                }
            }
        }
    }
}

/** Runs [block] with the open session of [player], or tells the player to start one first. */
internal inline fun withSession(player: Player, block: (EditorSession) -> Unit) {
    val session = EditorSessions[player.uniqueId] ?: return fail(player, "Starte zuerst mit /hubregion create <id>.")
    block(session)
}

/** Sends [text] to [player] as a success message. */
internal fun ok(player: Player, text: String) = player.sendText { appendSuccessPrefix(); success(text) }

/** Sends [text] to [player] as an error message. */
internal fun fail(player: Player, text: String) = player.sendText { appendErrorPrefix(); error(text) }
