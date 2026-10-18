package dev.slne.surf.odyssey.runtime.paper.progression.definition.registry

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.dialog.NodeAction
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.Objective
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.quest.QuestType
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.AllOfRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.AnyOfRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.NotRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestCompletedRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStepRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.RecipeUnlockedRequirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.Requirement
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.TitleReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ToolReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult

/**
 * Checks a [ContentPack] for problems that must stop the server from starting.
 *
 * It checks unique and well-formed IDs, references to items, stations, recipes, titles, tools, NPCs and nodes,
 * quest and recipe IDs inside requirements,
 * positive amounts, the neutral NPC portrait, quest structure, recurring pool sizes and tool curves. All problems are
 * collected instead of stopping at the first.
 *
 * @property pack the pack to check
 */
internal class ContentValidator(private val pack: ContentPack) {
    /** Problems found so far. */
    private val problems = mutableListOf<String>()

    /** IDs of the items in the pack. */
    private val itemIds = pack.items.mapTo(HashSet()) { it.id }

    /** IDs of the stations in the pack. */
    private val stationIds = pack.stations.mapTo(HashSet()) { it.id }

    /** IDs of the recipes in the pack. */
    private val recipeIds = pack.recipes.mapTo(HashSet()) { it.id }

    /** IDs of the titles in the pack. */
    private val titleIds = pack.titles.mapTo(HashSet()) { it.id }

    /** IDs of the NPCs in the pack. */
    private val npcIds = pack.npcs.mapTo(HashSet()) { it.id }

    /** IDs of the quests in the pack. */
    private val questIds = pack.quests.mapTo(HashSet()) { it.id }

    /** IDs of the tools in the pack. */
    private val toolIds = pack.tools.mapTo(HashSet()) { it.id }

    /**
     * Runs every check.
     *
     * @return all problems found; empty if the pack is valid
     */
    fun validate(): List<String> {
        checkIds("item", pack.items)
        checkIds("station", pack.stations)
        checkIds("recipe", pack.recipes)
        checkIds("title", pack.titles)
        checkIds("npc", pack.npcs)
        checkIds("quest", pack.quests)
        checkIds("tool", pack.tools)
        checkIds("accessory", pack.accessories)
        pack.recipes.forEach(::checkRecipe)
        pack.npcs.forEach(::checkNpc)
        pack.quests.forEach(::checkQuest)
        checkPools()
        checkTools()
        pack.accessories.forEach { checkItemRef("accessory '${it.id}'", CustomItemRef(it.item)) }
        return problems.toList()
    }

    /** Reports IDs of one kind that do not match the ID pattern or occur more than once. */
    private fun checkIds(kind: String, definitions: List<Definition>) {
        checkIdStrings(kind, definitions.map { it.id })
    }

    /** Reports [ids] of one kind that do not match the ID pattern or occur more than once. */
    private fun checkIdStrings(kind: String, ids: List<String>) {
        ids.filterNot { Definition.ID_PATTERN.matches(it) }.forEach { problems += "invalid $kind id '$it'" }
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach {
            problems += "duplicate $kind id '$it'"
        }
    }

    /** Checks the station, ingredients, result and timing of a recipe. */
    private fun checkRecipe(recipe: RecipeDefinition) {
        val where = "recipe '${recipe.id}'"
        if (recipe.station.id !in stationIds) problems += "$where references unknown station '${recipe.station.id}'"
        checkIngredients(where, recipe.ingredients)
        checkItemRef(where, recipe.result)
        checkAmount(where, "result amount", recipe.resultAmount)
        if (recipe.duration.isNegative()) problems += "$where has a negative duration"
    }

    /** Checks the portraits, station and trades of an NPC. */
    private fun checkNpc(npc: NpcDefinition) {
        val where = "npc '${npc.id}'"
        if (NpcDefinition.NEUTRAL_MOOD !in npc.portraits) {
            problems += "$where has no '${NpcDefinition.NEUTRAL_MOOD}' portrait"
        }
        npc.visibleWhen?.let { checkRequirement("$where visibility", it) }
        npc.station?.let { if (it.id !in stationIds) problems += "$where references unknown station '${it.id}'" }
        checkIdStrings("trade offer of $where", npc.trades.map { it.id })
        npc.trades.forEach { offer ->
            val offerWhere = "trade offer '${offer.id}' of $where"
            when (val result = offer.result) {
                is TradeResult.Item -> {
                    checkItemRef(offerWhere, result.item)
                    checkAmount(offerWhere, "result amount", result.amount)
                }
                is TradeResult.Recipe -> checkRecipeRef(offerWhere, result.recipe)
            }
            when (val price = offer.price) {
                is Price.Items -> checkIngredients(offerWhere, price.ingredients)
                is Price.Currency -> if (price.amount <= 0) problems += "$offerWhere has a non-positive price"
            }
        }
    }

    /** Checks the structure, node references, objectives and rewards of a quest. */
    private fun checkQuest(quest: QuestDefinition) {
        val where = "quest '${quest.id}'"
        checkIdStrings("node of $where", quest.nodes.map { it.id })
        val nodeIds = quest.nodes.mapTo(HashSet()) { it.id }

        /** Reports [nodeId] when the quest has no node with this ID. */
        fun checkNode(context: String, nodeId: String) {
            if (nodeId !in nodeIds) problems += "$where $context: missing node '$nodeId'"
        }

        checkNpcRef(where, "giver", quest.giver)
        checkRequirement("$where requirement", quest.requirement)
        checkNode("offer", quest.offerNode)
        if (quest.steps.isEmpty()) problems += "$where has no steps"
        if (quest.type.recurring && quest.steps.size != 1) {
            problems += "$where is recurring and must have exactly one step, has ${quest.steps.size}"
        }
        quest.steps.forEachIndexed { index, step ->
            val stepWhere = "step $index"
            checkNpcRef(where, "$stepWhere npc", step.npc)
            checkNode("$stepWhere entry", step.entryNode)
            if (step.objectives.isEmpty()) problems += "$where $stepWhere has no objectives"
            step.objectives.forEach { objective ->
                val objectiveWhere = "$where $stepWhere"
                checkAmount(objectiveWhere, "objective amount", objective.amount)
                when (objective) {
                    is Objective.Deliver -> {
                        checkItemRef(objectiveWhere, objective.item)
                        checkNpcRef(where, "$stepWhere deliver target", objective.npc)
                    }
                    is Objective.TalkTo -> {
                        checkNpcRef(where, "$stepWhere talk target", objective.npc)
                        checkNode("$stepWhere talk", objective.nodeId)
                    }
                    is Objective.Fish -> if (objective.fishId == null && objective.material == null) {
                        problems += "$objectiveWhere has a fish objective without fish id or material"
                    }
                    is Objective.Craft -> checkRecipeRef(objectiveWhere, objective.recipe)
                    is Objective.Harvest, is Objective.Kill -> Unit
                }
            }
            step.rewards.forEach { checkReward("$where $stepWhere", it) }
        }
        quest.rewards.forEach { checkReward(where, it) }
        quest.nodes.forEach { node ->
            val nodeWhere = "node '${node.id}'"
            checkNpcRef(where, "$nodeWhere npc", node.npc)
            node.answers.forEach { answer ->
                answer.visibleWhen?.let { checkRequirement("$where $nodeWhere answer visibility", it) }
                when (val action = answer.action) {
                    is NodeAction.Goto -> checkNode("$nodeWhere goto", action.nodeId)
                    is NodeAction.AcceptQuest -> action.then?.let { checkNode("$nodeWhere then", it) }
                    is NodeAction.CompleteTalk -> action.then?.let { checkNode("$nodeWhere then", it) }
                    is NodeAction.DeliverItems -> action.then?.let { checkNode("$nodeWhere then", it) }
                    is NodeAction.TurnIn -> action.then?.let { checkNode("$nodeWhere then", it) }
                    NodeAction.OpenTrade, NodeAction.OpenStation, NodeAction.Close -> Unit
                }
            }
        }
    }

    /** Reports recurring pools that hold fewer quests than are drawn per period. */
    private fun checkPools() {
        QuestType.entries.filter { it.recurring }.forEach { type ->
            val count = pack.pools.countFor(type)
            val size = pack.quests.count { it.type == type }
            if (count < 0) problems += "pool $type has a negative draw count $count"
            if (size < count) problems += "pool $type has $size quests but draws $count per period"
        }
    }

    /** Reports tools whose curve does not start at zero or does not rise strictly up to the maximum level. */
    private fun checkTools() {
        pack.tools.forEach { tool ->
            val where = "tool '${tool.id}'"
            if (tool.maxLevel < 1) problems += "$where has max level ${tool.maxLevel}"
            if (tool.curve.usageFor(1) != 0L) problems += "$where curve needs usage at level 1"
            (2..tool.maxLevel).firstOrNull { tool.curve.usageFor(it) <= tool.curve.usageFor(it - 1) }?.let {
                problems += "$where curve is not strictly increasing at level $it"
            }
        }
    }

    /** Reports quest and recipe IDs in [requirement] and its nested requirements that are not in the pack. */
    private fun checkRequirement(where: String, requirement: Requirement) {
        when (requirement) {
            is QuestCompletedRequirement -> checkQuestId(where, requirement.questId)
            is QuestStepRequirement -> checkQuestId(where, requirement.questId)
            is RecipeUnlockedRequirement -> if (requirement.recipeId !in recipeIds) {
                problems += "$where references unknown recipe '${requirement.recipeId}'"
            }
            is AllOfRequirement -> requirement.requirements.forEach { checkRequirement(where, it) }
            is AnyOfRequirement -> requirement.requirements.forEach { checkRequirement(where, it) }
            is NotRequirement -> checkRequirement(where, requirement.requirement)
        }
    }

    /** Reports [questId] when the pack has no quest with this ID. */
    private fun checkQuestId(where: String, questId: String) {
        if (questId !in questIds) problems += "$where references unknown quest '$questId'"
    }

    /** Checks the target and amount of a reward. */
    private fun checkReward(where: String, reward: QuestReward) {
        when (reward) {
            is ItemReward -> {
                checkItemRef(where, reward.item)
                checkAmount(where, "reward amount", reward.amount)
            }
            is RecipeReward -> checkRecipeRef(where, reward.recipe)
            is TitleReward -> if (reward.title.id !in titleIds) {
                problems += "$where references unknown title '${reward.title.id}'"
            }
            is ToolReward -> if (reward.tool.id !in toolIds) {
                problems += "$where references unknown tool '${reward.tool.id}'"
            }
            is CurrencyReward -> if (reward.amount <= 0) problems += "$where has a non-positive currency reward"
            else -> Unit
        }
    }

    /** Checks the items and amounts of a list of ingredients. */
    private fun checkIngredients(where: String, ingredients: List<Ingredient>) {
        ingredients.forEach {
            checkItemRef(where, it.item)
            checkAmount(where, "ingredient amount", it.amount)
        }
    }

    /** Reports custom item references that are not in the pack. */
    private fun checkItemRef(where: String, ref: ItemRef) {
        when (ref) {
            is CustomItemRef -> if (ref.item.id !in itemIds) problems += "$where references unknown item '${ref.item.id}'"
            is MaterialRef -> Unit
        }
    }

    /** Reports recipe references that are not in the pack. */
    private fun checkRecipeRef(where: String, recipe: RecipeDefinition) {
        if (recipe.id !in recipeIds) problems += "$where references unknown recipe '${recipe.id}'"
    }

    /** Reports NPC references that are not in the pack. */
    private fun checkNpcRef(where: String, role: String, npc: NpcDefinition) {
        if (npc.id !in npcIds) problems += "$where $role references unknown npc '${npc.id}'"
    }

    /** Reports amounts that are not positive. */
    private fun checkAmount(where: String, what: String, amount: Int) {
        if (amount <= 0) problems += "$where has a non-positive $what $amount"
    }
}
