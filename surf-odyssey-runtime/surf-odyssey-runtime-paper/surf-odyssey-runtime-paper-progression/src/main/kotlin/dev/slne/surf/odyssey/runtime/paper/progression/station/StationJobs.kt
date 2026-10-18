package dev.slne.surf.odyssey.runtime.paper.progression.station

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.odyssey.core.common.progression.StationJobDto
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.quest.ProgressEvent
import dev.slne.surf.odyssey.runtime.paper.progression.quest.QuestEngine
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The open station jobs of players: listing them per station kind, showing them as items and collecting finished
 * ones.
 */
object StationJobs {
    /**
     * An open job together with its recipe.
     *
     * @property job the stored job
     * @property recipe the recipe the job crafts
     */
    data class OpenJob(val job: StationJobDto, val recipe: RecipeDefinition)

    /**
     * Returns the open jobs of [player] at stations of the kind [station], the soonest ready first. Jobs whose recipe
     * is not part of the installed content are left out.
     */
    fun jobsAt(player: Player, station: StationDefinition): List<OpenJob> {
        val runtime = Progression.runtimeOrNull ?: return emptyList()
        val progression = runtime.store[player.uniqueId] ?: return emptyList()
        return progression.state.jobs
            .filter { it.stationType == station.id }
            .sortedBy { it.readyAt }
            .mapNotNull { job -> runtime.registry.recipe(job.recipeId)?.let { OpenJob(job, it) } }
    }

    /**
     * Builds the item shown for [open]: the results of the job with a lore line that says it can be collected, or
     * how long it still runs.
     */
    fun itemOf(open: OpenJob): ItemStack {
        val now = Progression.runtime.clock.instant()
        val stack = open.recipe.result.create(1)
        stack.amount = (open.recipe.resultAmount * open.job.amount).coerceIn(1, stack.maxStackSize)
        stack.buildLore {
            emptyLine()
            if (CraftPlanner.isReady(open.job, now)) {
                line { success("Fertig") }
                line { spacer("Klicken zum Abholen".toSmallCaps()) }
            } else {
                line {
                    info("Fertig in: ")
                    variableValue(DurationText.format(CraftPlanner.remaining(open.job, now)))
                }
            }
            emptyLine()
        }
        return stack
    }

    /**
     * Collects the finished job [open] for [player]: the job is marked collected, its results are given and its
     * crafts count for quests. A job that is still running is not collected. Nothing happens outside of
     * [ProgressionMode.FULL].
     *
     * Must be called on the player's thread. [afterwards] runs on the player's thread once the operation ended, if
     * the player is still online.
     *
     * @param player the collecting player
     * @param open the job to collect
     * @param afterwards runs after the operation, for example to show the menu again
     */
    fun collect(player: Player, open: OpenJob, afterwards: (Player) -> Unit = {}) {
        val runtime = Progression.runtimeOrNull ?: return
        if (runtime.mode != ProgressionMode.FULL) return
        if (!CraftPlanner.isReady(open.job, runtime.clock.instant())) {
            player.playNoSound()
            return
        }
        Crafting.run(player, CraftPlanner.collectPlan(open.recipe, open.job), afterwards) {
            ProgressionMessages.jobCollected(player, open.recipe.result.displayName)
            QuestEngine.onProgress(player, ProgressEvent.Crafted(open.recipe.id, open.job.amount))
        }
    }
}
