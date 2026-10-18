package dev.slne.surf.odyssey.runtime.paper.progression.definition.registry

import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.DialogNode
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
 * Validated, indexed view of a [ContentPack].
 *
 * Instances are created with [build], which rejects packs with problems.
 */
class ProgressionRegistry private constructor(private val pack: ContentPack) {
    /** Items by ID. */
    private val items = pack.items.associateBy { it.id }

    /** Stations by ID. */
    private val stations = pack.stations.associateBy { it.id }

    /** Recipes by ID. */
    private val recipes = pack.recipes.associateBy { it.id }

    /** Titles by ID. */
    private val titles = pack.titles.associateBy { it.id }

    /** NPCs by ID. */
    private val npcs = pack.npcs.associateBy { it.id }

    /** Quests by ID. */
    private val quests = pack.quests.associateBy { it.id }

    /** Tools by ID. */
    private val tools = pack.tools.associateBy { it.id }

    /** Accessories by ID. */
    private val accessories = pack.accessories.associateBy { it.id }

    /** Recipes by station ID. */
    private val recipesByStation = pack.recipes.groupBy { it.station.id }

    /** Quests by giver NPC ID. */
    private val questsByGiver = pack.quests.groupBy { it.giver.id }

    /** Quests by type. */
    private val questsByType = pack.quests.groupBy { it.type }

    /** Nodes by quest ID and node ID. */
    private val nodes = pack.quests.associate { quest -> quest.id to quest.nodes.associateBy { it.id } }

    /** Draw counts of the recurring quest pools. */
    val pools: RecurringPools
        get() = pack.pools

    /** All items of the pack, in pack order. */
    val allItems: List<ItemDefinition>
        get() = pack.items

    /** All stations of the pack, in pack order. */
    val allStations: List<StationDefinition>
        get() = pack.stations

    /** All recipes of the pack, in pack order. */
    val allRecipes: List<RecipeDefinition>
        get() = pack.recipes

    /** All titles of the pack, in pack order. */
    val allTitles: List<TitleDefinition>
        get() = pack.titles

    /** All NPCs of the pack, in pack order. */
    val allNpcs: List<NpcDefinition>
        get() = pack.npcs

    /** All quests of the pack, in pack order. */
    val allQuests: List<QuestDefinition>
        get() = pack.quests

    /** All tools of the pack, in pack order. */
    val allTools: List<ToolDefinition>
        get() = pack.tools

    /** All accessories of the pack, in pack order. */
    val allAccessories: List<AccessoryDefinition>
        get() = pack.accessories

    /** Returns the item with [id], or `null`. */
    fun item(id: String): ItemDefinition? = items[id]

    /** Returns the station with [id], or `null`. */
    fun station(id: String): StationDefinition? = stations[id]

    /** Returns the recipe with [id], or `null`. */
    fun recipe(id: String): RecipeDefinition? = recipes[id]

    /** Returns the title with [id], or `null`. */
    fun title(id: String): TitleDefinition? = titles[id]

    /** Returns the NPC with [id], or `null`. */
    fun npc(id: String): NpcDefinition? = npcs[id]

    /** Returns the quest with [id], or `null`. */
    fun quest(id: String): QuestDefinition? = quests[id]

    /** Returns the tool with [id], or `null`. */
    fun tool(id: String): ToolDefinition? = tools[id]

    /** Returns the accessory with [id], or `null`. */
    fun accessory(id: String): AccessoryDefinition? = accessories[id]

    /** Returns the recipes crafted at [station], in pack order. */
    fun recipesOf(station: StationDefinition): List<RecipeDefinition> = recipesByStation[station.id].orEmpty()

    /** Returns the quests offered by [npc], in pack order. */
    fun questsGivenBy(npc: NpcDefinition): List<QuestDefinition> = questsByGiver[npc.id].orEmpty()

    /** Returns all quests of [type], in pack order; for recurring types this is the pool drawn from. */
    fun pool(type: QuestType): List<QuestDefinition> = questsByType[type].orEmpty()

    /**
     * Returns a node of [quest].
     *
     * @param quest the quest owning the node
     * @param nodeId the node ID
     * @throws NoSuchElementException if the quest has no such node
     */
    fun node(quest: QuestDefinition, nodeId: String): DialogNode =
        nodes[quest.id]?.get(nodeId) ?: throw NoSuchElementException("quest '${quest.id}' has no node '$nodeId'")

    /** Creates registries. */
    companion object {
        /**
         * Validates [pack] and indexes it.
         *
         * @throws ContentValidationException with every problem found if the pack is invalid
         */
        fun build(pack: ContentPack): ProgressionRegistry {
            val problems = ContentValidator(pack).validate()
            if (problems.isNotEmpty()) throw ContentValidationException(problems)
            return ProgressionRegistry(pack)
        }
    }
}
