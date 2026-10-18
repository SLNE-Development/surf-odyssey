package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.content.quests.Quests
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

/** The progression content that the world plugins and the home server install. */
object OdysseyContent : ContentPack {
    /** The items of [Items]. */
    override val items: List<ItemDefinition>
        get() = Items.all

    /** The stations of [Stations]. */
    override val stations: List<StationDefinition>
        get() = Stations.all

    /** The recipes of [Recipes]. */
    override val recipes: List<RecipeDefinition>
        get() = Recipes.all

    /** The titles of [Titles]. */
    override val titles: List<TitleDefinition>
        get() = Titles.all

    /** The NPCs of [Npcs]. */
    override val npcs: List<NpcDefinition>
        get() = Npcs.all

    /** The quests of [Quests]. */
    override val quests: List<QuestDefinition>
        get() = Quests.all

    /** The tools of [Tools]. */
    override val tools: List<ToolDefinition>
        get() = Tools.all

    /** The accessories of [Accessories]. */
    override val accessories: List<AccessoryDefinition>
        get() = Accessories.all

    /** No recurring quests are drawn yet. */
    override val pools: RecurringPools = RecurringPools(dailyCount = 0, weeklyCount = 0, monthlyCount = 0)
}
