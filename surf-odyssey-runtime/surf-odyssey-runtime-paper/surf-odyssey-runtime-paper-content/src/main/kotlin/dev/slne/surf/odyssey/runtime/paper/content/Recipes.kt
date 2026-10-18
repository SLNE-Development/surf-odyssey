package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.RecipeDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.recipe.recipe
import org.bukkit.Material
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The recipes of the Odyssey content, crafted at the [Stations]. */
object Recipes {
    /** Workbench: 2 copper ingots make 1 copper sheet, instantly. Known from the start. */
    val COPPER_SHEET: RecipeDefinition = recipe("copper_sheet", Stations.WORKBENCH) {
        ingredient(Material.COPPER_INGOT.ref, 2)
        result(Items.COPPER_SHEET.ref, 1)
        unlockedByDefault = true
    }

    /** Workbench: 1 glass bottle and 2 salmon make 1 lamp oil, instantly. Known from the start. */
    val LANTERN_OIL: RecipeDefinition = recipe("lantern_oil", Stations.WORKBENCH) {
        ingredient(Material.GLASS_BOTTLE.ref, 1)
        ingredient(Material.SALMON.ref, 2)
        result(Items.LANTERN_OIL.ref, 1)
        unlockedByDefault = true
    }

    /** Workbench: 2 wheat and 1 kelp make 4 bait worms, instantly. Unlocked by the lighthouse questline. */
    val BAIT_WORM: RecipeDefinition = recipe("bait_worm", Stations.WORKBENCH) {
        ingredient(Material.WHEAT.ref, 2)
        ingredient(Material.KELP.ref, 1)
        result(Items.BAIT_WORM.ref, 4)
    }

    /** Workbench: 4 copper sheets make 1 drill bit in 2 minutes. Unlocked by completing the lighthouse questline. */
    val DRILL_BIT: RecipeDefinition = recipe("drill_bit", Stations.WORKBENCH) {
        ingredient(Items.COPPER_SHEET.ref, 4)
        result(Items.DRILL_BIT.ref, 1)
        duration = 2.minutes
    }

    /** Cooking station: 1 salmon, 1 kelp and 1 wheat make 2 sushi in 1 minute. Sold by the cook. */
    val SUSHI: RecipeDefinition = recipe("sushi", Stations.COOKING_STATION) {
        ingredient(Material.SALMON.ref, 1)
        ingredient(Material.KELP.ref, 1)
        ingredient(Material.WHEAT.ref, 1)
        result(Items.SUSHI.ref, 2)
        duration = 1.minutes
    }

    /** Cooking station: 2 cod and 1 oak log make 2 smoked fish in 3 minutes. Sold by the cook. */
    val SMOKED_FISH: RecipeDefinition = recipe("smoked_fish", Stations.COOKING_STATION) {
        ingredient(Material.COD.ref, 2)
        ingredient(Material.OAK_LOG.ref, 1)
        result(Items.SMOKED_FISH.ref, 2)
        duration = 3.minutes
    }

    /** Cooking station: 2 cod and 1 kelp make 1 fish soup, instantly. Known from the start. */
    val FISH_SOUP: RecipeDefinition = recipe("fish_soup", Stations.COOKING_STATION) {
        ingredient(Material.COD.ref, 2)
        ingredient(Material.KELP.ref, 1)
        result(Items.FISH_SOUP.ref, 1)
        unlockedByDefault = true
    }

    /** Cooking station: 1 sushi and 1 smoked fish make 1 fish platter in 30 seconds. Unlocked by a weekly quest. */
    val FISH_PLATTER: RecipeDefinition = recipe("fish_platter", Stations.COOKING_STATION) {
        ingredient(Items.SUSHI.ref, 1)
        ingredient(Items.SMOKED_FISH.ref, 1)
        result(Items.FISH_PLATTER.ref, 1)
        duration = 30.seconds
    }

    /** All recipes in the order they are registered. */
    val all: List<RecipeDefinition> = listOf(
        COPPER_SHEET, LANTERN_OIL, BAIT_WORM, DRILL_BIT, SUSHI, SMOKED_FISH, FISH_SOUP, FISH_PLATTER,
    )
}
