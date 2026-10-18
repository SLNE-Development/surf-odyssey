package dev.slne.surf.odyssey.runtime.paper.progression.admin

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.uuidArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.odyssey.core.common.progression.ProgressionBridge
import dev.slne.surf.odyssey.core.common.progression.QuestStatus
import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionPermissions
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.TitleReward
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationPlan
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestProgressView
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolItems
import dev.slne.surf.odyssey.runtime.paper.progression.tool.ToolUsage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.Instant
import java.util.*

/** The source name stored with the deliveries of operations that admin commands run. */
private const val ADMIN_SOURCE = "admin"

/** The furthest distance in blocks at which `place station` finds the block the admin looks at. */
private const val TARGET_RANGE = 6

/**
 * Registers `/odysseyadmin`, which administers the progression of players.
 *
 * - `quest info <player> <quest>` shows the stored state and the counters of a quest.
 * - `quest give|complete|reset <player> <quest>` accepts a quest without checking its requirement, completes it, or
 *   deletes its state and counters. Completing gives the rewards of the current step and the final rewards of the
 *   quest, even when the current step is not the last one; the rewards of the steps in between are not given.
 * - `recipe give|take <player> <recipe>` and `title give|take <player> <title>` unlock or revoke them.
 * - `tool give <player> <tool> [level]` gives a new tool, `tool setlevel <level>` sets the level of the tool in the
 *   main hand and `tool lookup <uuid>` shows the registration of a tool.
 * - `place npc <npc>` and `place station <station>` write the statement that places the definition at the position
 *   of the admin, or at the block the admin looks at, to `placements/npc_<id>.kt` or `placements/station_<id>.kt` in the data folder of the plugin.
 *
 * Only `quest info` exists in [ProgressionMode.VIEW_ONLY]. Every subcommand needs [ProgressionPermissions.ADMIN].
 *
 * @param mode which parts of the engine run on this server
 */
fun progressionAdminCommand(mode: ProgressionMode) = commandTree("odysseyadmin") {
    withPermission(ProgressionPermissions.ADMIN)
    literalArgument("quest") {
        literalArgument("info") {
            entitySelectorArgumentOnePlayer("player") {
                stringArgument("quest") {
                    suggestIds { Progression.runtime.registry.allQuests.map { it.id } }
                    anyExecutor { sender, args -> questInfo(sender, args) }
                }
            }
        }
        if (mode == ProgressionMode.FULL) {
            literalArgument("give") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("quest") {
                        suggestIds { Progression.runtime.registry.allQuests.map { it.id } }
                        anyExecutor { sender, args -> questGive(sender, args) }
                    }
                }
            }
            literalArgument("complete") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("quest") {
                        suggestIds { Progression.runtime.registry.allQuests.map { it.id } }
                        anyExecutor { sender, args -> questComplete(sender, args) }
                    }
                }
            }
            literalArgument("reset") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("quest") {
                        suggestIds { Progression.runtime.registry.allQuests.map { it.id } }
                        anyExecutor { sender, args -> questReset(sender, args) }
                    }
                }
            }
        }
    }
    if (mode == ProgressionMode.FULL) {
        literalArgument("recipe") {
            literalArgument("give") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("recipe") {
                        suggestIds { Progression.runtime.registry.allRecipes.map { it.id } }
                        anyExecutor { sender, args -> recipeGive(sender, args) }
                    }
                }
            }
            literalArgument("take") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("recipe") {
                        suggestIds { Progression.runtime.registry.allRecipes.map { it.id } }
                        anyExecutor { sender, args -> recipeTake(sender, args) }
                    }
                }
            }
        }
        literalArgument("title") {
            literalArgument("give") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("title") {
                        suggestIds { Progression.runtime.registry.allTitles.map { it.id } }
                        anyExecutor { sender, args -> titleGive(sender, args) }
                    }
                }
            }
            literalArgument("take") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("title") {
                        suggestIds { Progression.runtime.registry.allTitles.map { it.id } }
                        anyExecutor { sender, args -> titleTake(sender, args) }
                    }
                }
            }
        }
        literalArgument("tool") {
            literalArgument("give") {
                entitySelectorArgumentOnePlayer("player") {
                    stringArgument("tool") {
                        suggestIds { Progression.runtime.registry.allTools.map { it.id } }
                        anyExecutor { sender, args -> toolGive(sender, args, level = 1) }
                        integerArgument("level", 1) {
                            anyExecutor { sender, args -> toolGive(sender, args, level = args["level"] as Int) }
                        }
                    }
                }
            }
            literalArgument("setlevel") {
                integerArgument("level", 1) {
                    playerExecutor { player, args -> toolSetLevel(player, args["level"] as Int) }
                }
            }
            literalArgument("lookup") {
                uuidArgument("tool") {
                    anyExecutor { sender, args -> toolLookup(sender, args["tool"] as UUID) }
                }
            }
        }
        literalArgument("place") {
            literalArgument("npc") {
                stringArgument("npc") {
                    suggestIds { Progression.runtime.registry.allNpcs.map { it.id } }
                    playerExecutor { player, args -> placeNpc(player, args["npc"] as String) }
                }
            }
            literalArgument("station") {
                stringArgument("station") {
                    suggestIds { Progression.runtime.registry.allStations.map { it.id } }
                    playerExecutor { player, args -> placeStation(player, args["station"] as String) }
                }
            }
        }
    }
}

/** Suggests the IDs [ids] returns, read when the suggestions are requested. */
private fun Argument<*>.suggestIds(ids: () -> List<String>) {
    replaceSuggestions(ArgumentSuggestions.strings<CommandSender> { ids().toTypedArray() })
}

/** Sends [text] to [sender] as a success message. */
private fun reply(sender: CommandSender, text: String) = sender.sendText {
    appendSuccessPrefix()
    success(text)
}

/** Sends [text] to [sender] as an error message. */
private fun refuse(sender: CommandSender, text: String) = sender.sendText {
    appendErrorPrefix()
    error(text)
}

/** Returns the quest named by the argument `quest`, or tells [sender] that it is unknown and returns `null`. */
private fun questOf(sender: CommandSender, args: CommandArguments): QuestDefinition? {
    val id = args["quest"] as String
    return Progression.runtime.registry.quest(id) ?: run {
        refuse(sender, AdminMessages.unknownQuest(id))
        null
    }
}

/** Runs [plan] for [target] in the background and tells [sender] how it ended; [done] is the text for success. */
private fun runPlan(sender: CommandSender, target: Player, plan: OperationPlan, done: String) {
    ProgressionModule.plugin.launch {
        when (val outcome = Progression.runtime.operations.run(target, plan)) {
            OperationOutcome.Success -> reply(sender, done)
            OperationOutcome.Busy -> refuse(sender, AdminMessages.busy(target.name))
            OperationOutcome.MissingItems -> refuse(sender, AdminMessages.missingItems(target.name))
            OperationOutcome.Unavailable, OperationOutcome.NotStarted ->
                refuse(sender, AdminMessages.serviceUnavailable())
            is OperationOutcome.Rejected -> refuse(sender, AdminMessages.rejected(outcome.reason))
        }
    }
}

/** Runs [call] against the progression service for [target] in the background and reloads the state of [target]. */
private fun runChange(
    sender: CommandSender,
    target: Player,
    discardedQuest: String?,
    done: String,
    call: suspend (ProgressionBridge) -> Unit,
) {
    ProgressionModule.plugin.launch {
        if (Progression.runtime.store.change(target.uniqueId, discardedQuest, call)) reply(sender, done)
        else refuse(sender, AdminMessages.serviceUnavailable())
    }
}

/** Shows the stored state and counters of the quest named in [args] for the player named in [args]. */
private fun questInfo(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val quest = questOf(sender, args) ?: return
    val runtime = Progression.runtime
    val state = runtime.store[target.uniqueId]?.state
        ?: return refuse(sender, AdminMessages.stateNotLoaded(target.name))
    val stored = state.quests.find { it.questId == quest.id }
        ?: return reply(sender, AdminMessages.questNotAccepted(target.name, quest.id))
    val period = QuestProgressView.periodOf(quest, runtime.clock.instant())
    val progress = QuestProgressView.compute(quest, stored, state.counters, period)
    sender.sendText {
        appendInfoPrefix()
        variableValue(quest.id)
        info(" von ")
        variableValue(target.name)
        info(": ")
        variableValue(stored.status.name)
        info(", Schritt ")
        variableValue("${stored.step + 1}/${quest.steps.size}")
        info(", Periode ")
        variableValue(stored.period.toString())
        if (progress != null) {
            val objectives = quest.steps[progress.step].objectives
            info(", Ziele ")
            variableValue(progress.counts.indices.joinToString(" ") { "${progress.counts[it]}/${objectives[it].amount}" })
        } else if (stored.status == QuestStatus.ACTIVE) {
            info(", zählt gerade nicht")
        }
    }
}

/** Accepts the quest named in [args] for the player named in [args] without checking its requirement. */
private fun questGive(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val quest = questOf(sender, args) ?: return
    val period = QuestProgressView.periodOf(quest, Progression.runtime.clock.instant())
    val change = StateChange.QuestAccepted(quest.id, quest.steps.first().version, period)
    runPlan(sender, target, adminPlan(listOf(change)), AdminMessages.questGiven(quest.id, target.name))
}

/**
 * Completes the active quest named in [args] for the player named in [args] at its current step and gives the rewards
 * of that step and the final rewards of the quest. The final rewards are given even when the current step is not the
 * last one; the rewards of the skipped steps are not.
 */
private fun questComplete(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val quest = questOf(sender, args) ?: return
    val runtime = Progression.runtime
    val state = runtime.store[target.uniqueId]?.state
        ?: return refuse(sender, AdminMessages.stateNotLoaded(target.name))
    val stored = state.quests.find { it.questId == quest.id }
    val period = QuestProgressView.periodOf(quest, runtime.clock.instant())
    if (stored == null || !QuestProgressView.isCurrent(quest, stored, period)) {
        return refuse(sender, AdminMessages.questNotActive(target.name, quest.id))
    }
    val step = quest.steps[stored.step]
    val plan = adminPlan(listOf(StateChange.QuestCompleted(quest.id, stored.step)), step.rewards + quest.rewards)
    runPlan(sender, target, plan, AdminMessages.questCompleted(quest.id, target.name))
}

/** Deletes the state and counters of the quest named in [args] for the player named in [args]. */
private fun questReset(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val quest = questOf(sender, args) ?: return
    runChange(sender, target, quest.id, AdminMessages.questReset(quest.id, target.name)) {
        it.resetQuest(target.uniqueId, quest.id)
    }
}

/** Unlocks the recipe named in [args] for the player named in [args]. */
private fun recipeGive(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val id = args["recipe"] as String
    val recipe = Progression.runtime.registry.recipe(id) ?: return refuse(sender, AdminMessages.unknownRecipe(id))
    val plan = adminPlan(rewards = listOf(RecipeReward(recipe)))
    runPlan(sender, target, plan, AdminMessages.recipeGiven(recipe.id, target.name))
}

/** Locks the recipe named in [args] again for the player named in [args]. */
private fun recipeTake(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val id = args["recipe"] as String
    val recipe = Progression.runtime.registry.recipe(id) ?: return refuse(sender, AdminMessages.unknownRecipe(id))
    runChange(sender, target, null, AdminMessages.recipeTaken(recipe.id, target.name)) {
        it.revokeRecipe(target.uniqueId, recipe.id)
    }
}

/** Unlocks the title named in [args] for the player named in [args]. */
private fun titleGive(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val id = args["title"] as String
    val title = Progression.runtime.registry.title(id) ?: return refuse(sender, AdminMessages.unknownTitle(id))
    val plan = adminPlan(rewards = listOf(TitleReward(title)))
    runPlan(sender, target, plan, AdminMessages.titleGiven(title.id, target.name))
}

/** Takes the title named in [args] away from the player named in [args]. */
private fun titleTake(sender: CommandSender, args: CommandArguments) {
    val target = args["player"] as Player
    val id = args["title"] as String
    val title = Progression.runtime.registry.title(id) ?: return refuse(sender, AdminMessages.unknownTitle(id))
    runChange(sender, target, null, AdminMessages.titleTaken(title.id, target.name)) {
        it.revokeTitle(target.uniqueId, title.id)
    }
}

/** Gives the player named in [args] a new item of the tool named in [args], on [level]. */
private fun toolGive(sender: CommandSender, args: CommandArguments, level: Int) {
    val target = args["player"] as Player
    val id = args["tool"] as String
    val tool = Progression.runtime.registry.tool(id) ?: return refuse(sender, AdminMessages.unknownTool(id))
    if (level > tool.maxLevel) return refuse(sender, AdminMessages.maxLevel(tool.id, tool.maxLevel))
    val plan = adminPlan(rewards = listOf(LeveledToolReward(tool, level)))
    runPlan(sender, target, plan, AdminMessages.toolGiven(tool.id, level, target.name))
}

/** Sets the level of the tool in the main hand of [player] to [level]. */
private fun toolSetLevel(player: Player, level: Int) {
    val plugin = ProgressionModule.plugin
    plugin.launch(plugin.entityDispatcher(player)) {
        val item = player.inventory.itemInMainHand
        val state = ToolItems.read(item) ?: return@launch refuse(player, AdminMessages.noToolInHand())
        val tool = ToolItems.definitionOf(state)
            ?: return@launch refuse(player, AdminMessages.toolNotKnown(state.toolId))
        if (level > tool.maxLevel) return@launch refuse(player, AdminMessages.maxLevel(tool.id, tool.maxLevel))
        ToolUsage.writeInto(player.uniqueId, item)
        AdminTools.setLevel(item, tool, level)
        player.inventory.setItemInMainHand(item)
        reply(player, AdminMessages.levelSet(tool.id, level))
    }
}

/** Shows the registration of the tool with the unique ID [tool]. */
private fun toolLookup(sender: CommandSender, tool: UUID) {
    ProgressionModule.plugin.launch {
        val record = try {
            ProgressionBridge.lookupTool(tool)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return@launch refuse(sender, AdminMessages.serviceUnavailable())
        } ?: return@launch refuse(sender, AdminMessages.toolNotRegistered())
        sender.sendText {
            appendInfoPrefix()
            variableValue(record.toolId)
            info(" von ")
            variableValue(record.creator.toString())
            info(", zuletzt bei ")
            variableValue(record.lastHolder?.toString() ?: "niemandem")
            info(" um ")
            variableValue(record.lastSeenAt?.let { Instant.ofEpochMilli(it).toString() } ?: "nie")
        }
    }
}

/** Writes the statement that places the NPC named by [id] at the position of [player]. */
private fun placeNpc(player: Player, id: String) {
    if (Progression.runtime.registry.npc(id) == null) return refuse(player, AdminMessages.unknownNpc(id))
    val at = player.location
    save(player, PlacementWriter.npcFileName(id), PlacementWriter.npc(id, at.x, at.y, at.z, at.yaw, at.pitch))
}

/** Writes the statement that places the station named by [id] on the block [player] looks at. */
private fun placeStation(player: Player, id: String) {
    if (Progression.runtime.registry.station(id) == null) return refuse(player, AdminMessages.unknownStation(id))
    val block = player.getTargetBlockExact(TARGET_RANGE) ?: return refuse(player, AdminMessages.noBlockTargeted())
    save(player, PlacementWriter.stationFileName(id), PlacementWriter.station(id, block.x, block.y, block.z))
}

/** Saves [text] as the placement file [fileName] and tells [player] the path or the reason it failed. */
private fun save(player: Player, fileName: String, text: String) {
    when (val result = PlacementFile.save(ProgressionModule.plugin.dataFolder.toPath(), fileName, text)) {
        is PlacementFile.Result.Saved -> reply(player, AdminMessages.saved(result.file.toAbsolutePath()))
        is PlacementFile.Result.Exists ->
            refuse(player, AdminMessages.fileExists(result.file.toAbsolutePath()))
        is PlacementFile.Result.Failed ->
            refuse(player, AdminMessages.saveFailed(result.file.toAbsolutePath(), result.error.message))
    }
}

/** Creates an operation without items to take, with [changes] and [rewards], for the admin source. */
private fun adminPlan(changes: List<StateChange> = emptyList(), rewards: List<QuestReward> = emptyList()) =
    OperationPlan(take = emptyList(), changes = changes, rewards = rewards, source = ADMIN_SOURCE)
