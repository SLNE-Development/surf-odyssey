package dev.slne.surf.odyssey.runtime.paper.progression.definition.registry

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.TitleDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition

/**
 * How many recurring quests each player draws per period.
 *
 * @property dailyCount daily quests drawn per day
 * @property weeklyCount weekly quests drawn per week
 * @property monthlyCount monthly quests drawn per month
 */
class RecurringPools(val dailyCount: Int = 2, val weeklyCount: Int = 1, val monthlyCount: Int = 1) {
    /**
     * Returns how many quests of [type] are drawn per period.
     *
     * @return the draw count, or zero for types that are not recurring
     */
    fun countFor(type: QuestType): Int = when (type) {
        QuestType.DAILY -> dailyCount
        QuestType.WEEKLY -> weeklyCount
        QuestType.MONTHLY -> monthlyCount
        QuestType.MAIN, QuestType.SIDE -> 0
    }
}

/** All progression content of a server, registered into a [ProgressionRegistry] at startup. */
interface ContentPack {
    /** Custom items. */
    val items: List<ItemDefinition>

    /** Station kinds. */
    val stations: List<StationDefinition>

    /** Recipes. */
    val recipes: List<RecipeDefinition>

    /** Titles. */
    val titles: List<TitleDefinition>

    /** NPCs. */
    val npcs: List<NpcDefinition>

    /** Quests of every type. */
    val quests: List<QuestDefinition>

    /** Progression tools. */
    val tools: List<ToolDefinition>

    /** Tool accessories. */
    val accessories: List<AccessoryDefinition>

    /** Draw counts of the recurring quest pools. */
    val pools: RecurringPools
}

/**
 * Thrown when a [ContentPack] fails validation.
 *
 * @property problems every problem found, each naming the IDs involved
 */
class ContentValidationException(val problems: List<String>) : IllegalStateException(problems.joinToString("\n"))
