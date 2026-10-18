package dev.slne.surf.odyssey.runtime.paper.progression.definition.quest

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import org.bukkit.Material
import org.bukkit.entity.EntityType

/** One goal of a quest step. A step is done when all of its objectives are met. */
sealed interface Objective {
    /** How often the objective has to be fulfilled. */
    val amount: Int

    /**
     * Hand items to an NPC.
     *
     * @property item the item to deliver
     * @property amount how many to deliver
     * @property npc the receiving NPC
     */
    data class Deliver(val item: ItemRef, override val amount: Int, val npc: NpcDefinition) : Objective

    /**
     * Talk to an NPC at a node.
     *
     * @property npc the NPC to talk to
     * @property nodeId the node of the quest at which the talk is completed
     */
    data class TalkTo(val npc: NpcDefinition, val nodeId: String) : Objective {
        /** Always one. */
        override val amount: Int = 1
    }

    /**
     * Harvest blocks of a material.
     *
     * @property material the harvested block material
     * @property amount how many blocks to harvest
     * @property regionId the region the blocks must be harvested in, or `null` for any region
     */
    data class Harvest(val material: Material, override val amount: Int, val regionId: String?) : Objective

    /**
     * Catch fish. At least one of [fishId] and [material] is set.
     *
     * @property fishId ID of the fish entry to catch, or `null` for any entry
     * @property material material of the caught item, or `null` for any material
     * @property amount how many fish to catch
     * @property regionId the fishing region, or `null` for any region
     */
    data class Fish(
        val fishId: String?,
        val material: Material?,
        override val amount: Int,
        val regionId: String?,
    ) : Objective

    /**
     * Kill mobs of an entity type.
     *
     * @property entity the entity type to kill
     * @property amount how many to kill
     * @property regionId the mob region, or `null` for any region
     */
    data class Kill(val entity: EntityType, override val amount: Int, val regionId: String?) : Objective

    /**
     * Craft a recipe at its station.
     *
     * @property recipe the recipe to craft
     * @property amount how many crafts to complete
     */
    data class Craft(val recipe: RecipeDefinition, override val amount: Int) : Objective
}
