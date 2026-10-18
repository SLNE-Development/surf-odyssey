package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.ContentPack
import dev.slne.surf.odyssey.runtime.paper.progression.definition.registry.RecurringPools
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.StationDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.title.TitleDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition

/**
 * The progression content that the world plugins and the home server install.
 */
object OdysseyContent : ContentPack {
    /** The custom items of the content. */
    override val items: List<ItemDefinition> = emptyList()

    /** The station kinds of the content. */
    override val stations: List<StationDefinition> = emptyList()

    /** The recipes of the content. */
    override val recipes: List<RecipeDefinition> = emptyList()

    /** The titles of the content. */
    override val titles: List<TitleDefinition> = emptyList()

    /** The NPCs of the content. */
    override val npcs: List<NpcDefinition> = emptyList()

    /** The quests of the content. */
    override val quests: List<QuestDefinition> = emptyList()

    /** The progression tools of the content. */
    override val tools: List<ToolDefinition> = emptyList()

    /** The tool accessories of the content. */
    override val accessories: List<AccessoryDefinition> = emptyList()

    /** The default draw counts of the recurring quest pools. */
    override val pools: RecurringPools = RecurringPools()
}
