package dev.slne.surf.odyssey.runtime.paper.progression.quest

import org.bukkit.Material
import org.bukkit.entity.EntityType

/** Something a player did that can advance quest objectives. */
sealed interface ProgressEvent {
    /**
     * The player harvested a block.
     *
     * @property material the material of the harvested block
     * @property regionId the region the block was harvested in, or `null` outside of regions
     */
    data class Harvested(val material: Material, val regionId: String?) : ProgressEvent

    /**
     * The player caught a fish.
     *
     * @property fishId the ID of the caught fish entry, or `null` if the catch has no entry
     * @property material the material of the caught item
     * @property regionId the fishing region, or `null` outside of regions
     */
    data class Fished(val fishId: String?, val material: Material, val regionId: String?) : ProgressEvent

    /**
     * The player killed a mob.
     *
     * @property entity the type of the killed entity
     * @property regionId the mob region, or `null` outside of regions
     */
    data class Killed(val entity: EntityType, val regionId: String?) : ProgressEvent

    /**
     * The player completed crafts of a recipe.
     *
     * @property recipeId the ID of the crafted recipe
     * @property amount how many crafts were completed
     */
    data class Crafted(val recipeId: String, val amount: Int) : ProgressEvent
}
