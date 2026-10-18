package dev.slne.surf.odyssey.runtime.paper.hub.common.placeholder

import net.kyori.adventure.text.Component

/** Builds the text that names where a player is. */
object RegionDisplay {
    /** Text placed between the world name and the region name. */
    private val SEPARATOR = Component.text(" - ")

    /**
     * Returns "<world> - <region>" inside a region and "<world>" outside.
     *
     * @param world the display name of the world
     * @param region the display name of the region the player stands in, or `null` outside any region
     */
    fun combined(world: Component, region: Component?): Component =
        if (region == null) world else Component.textOfChildren(world, SEPARATOR, region)
}
