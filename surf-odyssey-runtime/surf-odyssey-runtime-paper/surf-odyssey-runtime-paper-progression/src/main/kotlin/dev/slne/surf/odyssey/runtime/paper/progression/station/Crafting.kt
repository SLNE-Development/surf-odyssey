package dev.slne.surf.odyssey.runtime.paper.progression.station

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationPlan
import dev.slne.surf.odyssey.runtime.paper.progression.quest.ProgressEvent
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestEngine
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.time.Duration

/**
 * Crafts recipes for players through item operations.
 *
 * Instant recipes take the ingredients and give the results in one operation and count as crafted for quests right
 * away. Timed recipes take one ingredient set and start a station job, which is collected later with
 * [StationJobs.collect].
 */
object Crafting {
    /**
     * Crafts [recipe] for [player], who must have unlocked it. An instant recipe is crafted once, or with
     * [everything] as often as the carried ingredients allow, at most [CraftPlanner.MAX_CRAFTS_PER_CLICK] times. A
     * timed recipe starts one job either way. Nothing happens outside of [ProgressionMode.FULL] or for a locked
     * recipe.
     *
     * Must be called on the player's thread. [afterwards] runs on the player's thread once the operation ended, if
     * the player is still online.
     *
     * @param player the crafting player
     * @param recipe the crafted recipe
     * @param everything whether to craft as often as possible
     * @param afterwards runs after the operation, for example to show the menu again
     */
    fun craft(player: Player, recipe: RecipeDefinition, everything: Boolean, afterwards: (Player) -> Unit = {}) {
        val runtime = Progression.runtimeOrNull ?: return
        if (runtime.mode != ProgressionMode.FULL) return
        val progression = runtime.store[player.uniqueId]
        if (progression == null) {
            ProgressionMessages.unavailable(player)
            return
        }
        if (!recipe.unlockedByDefault && !progression.isRecipeUnlocked(recipe.id)) {
            player.playNoSound()
            return
        }
        if (recipe.duration == Duration.ZERO) {
            val maxCrafts = CraftPlanner.maxCrafts(recipe, RecipeBrowserView.inventoryCounts(player, recipe))
            val times = CraftPlanner.timesFor(maxCrafts, everything)
            run(player, CraftPlanner.craftPlan(recipe, times), afterwards) {
                ProgressionMessages.crafted(player, recipe.resultAmount * times, recipe.result.displayName)
                QuestEngine.onProgress(player, ProgressEvent.Crafted(recipe.id, times))
            }
        } else {
            val plan = CraftPlanner.jobPlan(recipe, UUID.randomUUID(), runtime.clock.instant())
            run(player, plan, afterwards) {
                ProgressionMessages.jobStarted(player, recipe.result.displayName, DurationText.format(recipe.duration))
            }
        }
    }

    /**
     * Runs [plan] for [player] in the background, then on the player's thread reports the outcome, runs [onSuccess]
     * after a successful operation and runs [afterwards], while the player is online.
     */
    internal fun run(player: Player, plan: OperationPlan, afterwards: (Player) -> Unit, onSuccess: () -> Unit) {
        val plugin = ProgressionModule.plugin
        val operations = Progression.runtime.operations
        plugin.launch {
            val outcome = operations.run(player, plan)
            withContext(plugin.entityDispatcher(player)) {
                if (!player.isOnline) return@withContext
                operations.report(player, outcome)
                when (outcome) {
                    OperationOutcome.Success -> {
                        player.playYesSound()
                        onSuccess()
                    }
                    OperationOutcome.Busy -> return@withContext
                    else -> player.playNoSound()
                }
                afterwards(player)
            }
        }
    }
}
