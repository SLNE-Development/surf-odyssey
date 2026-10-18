package dev.slne.surf.odyssey.runtime.paper.content

import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolBonus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.accessory

/** The tool accessories of the Odyssey content. */
object Accessories {
    /** The bait worm in the bait slot of a rod: fish bite 10 percent sooner. */
    val BAIT_WORM: AccessoryDefinition = accessory("bait_worm", Items.BAIT_WORM, Tools.BAIT_SLOT) {
        bonus(ToolBonus.BiteSpeed(10))
    }

    /** The drill bit in the bit slot of a pickaxe: blocks break 15 percent faster. */
    val DRILL_BIT: AccessoryDefinition = accessory("drill_bit", Items.DRILL_BIT, Tools.BIT_SLOT) {
        bonus(Tools.breakSpeed(0.15))
    }

    /** All accessories in the order they are registered. */
    val all: List<AccessoryDefinition> = listOf(BAIT_WORM, DRILL_BIT)
}
