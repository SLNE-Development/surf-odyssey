package dev.slne.surf.odyssey.runtime.paper.hub.common.protection

import dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape.RegionDsl

/**
 * Which changes a hub world or a region allows. Every setting is `false` by default, so a change
 * is blocked unless a setting allows it.
 *
 * @property blockPlace players may place blocks
 * @property buckets players may empty and fill buckets
 * @property explosions explosions may destroy blocks
 * @property fire blocks may catch fire, burn away and fire may spread
 * @property spread blocks other than fire may spread, e.g. grass, mycelium or vines
 * @property form blocks may form, e.g. snow, ice or concrete
 * @property fade blocks may fade, e.g. melting ice or snow or dying coral
 * @property fluidFlow liquids may flow
 * @property pistons pistons may move blocks
 * @property grow plants may grow and saplings or mushrooms may grow into structures
 * @property fertilize blocks may be fertilized, e.g. with bone meal
 * @property leafDecay leaves may decay
 * @property trample players may trample farmland
 * @property entityBlockChanges entities may change blocks, e.g. endermen, falling blocks or mobs trampling farmland
 * @property hangingEntities item frames and paintings may be placed, broken, damaged and used, including
 *           rotating, inserting or taking their item
 * @property armorStands armor stands may be placed, damaged, used and have their equipment changed
 * @property signs players may open the sign editor, change the text of signs and dye, glow or wax them
 * @property dispensers dispensers and droppers may dispense items
 * @property decoBlocks players may change flower pots, cakes, composters, candles, candle cakes,
 *           chiseled bookshelves and decorated pots by right-clicking them, put books on empty lecterns
 *           and take books from lecterns; reading a book on a lectern is always possible
 * @property doors players may open and close doors, trapdoors and fence gates
 * @property buttons players may press buttons
 * @property levers players may flip levers
 * @property pressurePlates players and other entities may trigger pressure plates by stepping on them
 * @property redstoneComponents players may change note blocks, repeaters, comparators and daylight detectors, trigger tripwires and light redstone ore by touching it; tripwires and redstone ore also cover other entities
 * @property utilityBlocks players may use jukeboxes, bells, campfires, cauldrons, beehives, bee nests, respawn anchors and end portal frames, including shearing beehives and bee nests
 * @property plantHarvest players may pick sweet berries and glow berries, carve pumpkins with shears, tilt big dripleaves and trample turtle eggs; dripleaves and turtle eggs also cover other entities
 * @property vehicles players may place boats, minecarts and end crystals
 * @property containers players may open chests, trapped chests, ender chests, copper chests, barrels, shulker
 *           boxes, furnaces, smokers, blast furnaces, hoppers, droppers, dispensers, brewing stands and crafters
 */
data class ProtectionSettings(
    val blockPlace: Boolean = false,
    val buckets: Boolean = false,
    val explosions: Boolean = false,
    val fire: Boolean = false,
    val spread: Boolean = false,
    val form: Boolean = false,
    val fade: Boolean = false,
    val fluidFlow: Boolean = false,
    val pistons: Boolean = false,
    val grow: Boolean = false,
    val fertilize: Boolean = false,
    val leafDecay: Boolean = false,
    val trample: Boolean = false,
    val entityBlockChanges: Boolean = false,
    val hangingEntities: Boolean = false,
    val armorStands: Boolean = false,
    val signs: Boolean = false,
    val dispensers: Boolean = false,
    val decoBlocks: Boolean = false,
    val doors: Boolean = false,
    val buttons: Boolean = false,
    val levers: Boolean = false,
    val pressurePlates: Boolean = false,
    val redstoneComponents: Boolean = false,
    val utilityBlocks: Boolean = false,
    val plantHarvest: Boolean = false,
    val vehicles: Boolean = false,
    val containers: Boolean = false,
)

/**
 * Collects [ProtectionSettings] in a `settings { }` block; every setting starts as `false`.
 * Each property sets the [ProtectionSettings] property of the same name.
 */
@RegionDsl
class ProtectionSettingsBuilder {
    /** Sets [ProtectionSettings.blockPlace]. */
    var blockPlace = false

    /** Sets [ProtectionSettings.buckets]. */
    var buckets = false

    /** Sets [ProtectionSettings.explosions]. */
    var explosions = false

    /** Sets [ProtectionSettings.fire]. */
    var fire = false

    /** Sets [ProtectionSettings.spread]. */
    var spread = false

    /** Sets [ProtectionSettings.form]. */
    var form = false

    /** Sets [ProtectionSettings.fade]. */
    var fade = false

    /** Sets [ProtectionSettings.fluidFlow]. */
    var fluidFlow = false

    /** Sets [ProtectionSettings.pistons]. */
    var pistons = false

    /** Sets [ProtectionSettings.grow]. */
    var grow = false

    /** Sets [ProtectionSettings.fertilize]. */
    var fertilize = false

    /** Sets [ProtectionSettings.leafDecay]. */
    var leafDecay = false

    /** Sets [ProtectionSettings.trample]. */
    var trample = false

    /** Sets [ProtectionSettings.entityBlockChanges]. */
    var entityBlockChanges = false

    /** Sets [ProtectionSettings.hangingEntities]. */
    var hangingEntities = false

    /** Sets [ProtectionSettings.armorStands]. */
    var armorStands = false

    /** Sets [ProtectionSettings.signs]. */
    var signs = false

    /** Sets [ProtectionSettings.dispensers]. */
    var dispensers = false

    /** Sets [ProtectionSettings.decoBlocks]. */
    var decoBlocks = false

    /** Sets [ProtectionSettings.doors]. */
    var doors = false

    /** Sets [ProtectionSettings.buttons]. */
    var buttons = false

    /** Sets [ProtectionSettings.levers]. */
    var levers = false

    /** Sets [ProtectionSettings.pressurePlates]. */
    var pressurePlates = false

    /** Sets [ProtectionSettings.redstoneComponents]. */
    var redstoneComponents = false

    /** Sets [ProtectionSettings.utilityBlocks]. */
    var utilityBlocks = false

    /** Sets [ProtectionSettings.plantHarvest]. */
    var plantHarvest = false

    /** Sets [ProtectionSettings.vehicles]. */
    var vehicles = false

    /** Sets [ProtectionSettings.containers]. */
    var containers = false

    /** Creates the settings from the collected values. */
    fun build() = ProtectionSettings(
        blockPlace = blockPlace, buckets = buckets, explosions = explosions, fire = fire, spread = spread,
        form = form, fade = fade, fluidFlow = fluidFlow, pistons = pistons, grow = grow, fertilize = fertilize,
        leafDecay = leafDecay, trample = trample, entityBlockChanges = entityBlockChanges,
        hangingEntities = hangingEntities, armorStands = armorStands, signs = signs, dispensers = dispensers,
        decoBlocks = decoBlocks, doors = doors, buttons = buttons, levers = levers, pressurePlates = pressurePlates,
        redstoneComponents = redstoneComponents,
        utilityBlocks = utilityBlocks,
        plantHarvest = plantHarvest,
        vehicles = vehicles,
        containers = containers,
    )
}

/** Creates [ProtectionSettings] from the values set in [block]. */
fun protectionSettings(block: ProtectionSettingsBuilder.() -> Unit): ProtectionSettings =
    ProtectionSettingsBuilder().apply(block).build()

/**
 * A kind of change a [ProtectionSettings] property allows or blocks.
 *
 * @property allowedIn returns the [ProtectionSettings] property that allows this kind of change
 */
enum class ProtectionCategory(val allowedIn: (ProtectionSettings) -> Boolean) {
    /** A player places a block. */
    BLOCK_PLACE({ it.blockPlace }),

    /** A player empties or fills a bucket. */
    BUCKETS({ it.buckets }),

    /** An explosion destroys a block. */
    EXPLOSIONS({ it.explosions }),

    /** A block catches fire, burns away or fire spreads. */
    FIRE({ it.fire }),

    /** A block other than fire spreads, e.g. grass, mycelium or vines. */
    SPREAD({ it.spread }),

    /** A block forms, e.g. snow, ice or concrete, also when formed by an entity. */
    FORM({ it.form }),

    /** A block fades, e.g. melting ice or snow or dying coral. */
    FADE({ it.fade }),

    /** A liquid flows into another block. */
    FLUID_FLOW({ it.fluidFlow }),

    /** A piston moves blocks. */
    PISTONS({ it.pistons }),

    /** A plant grows, or a sapling or mushroom grows into a structure. */
    GROW({ it.grow }),

    /** A block is fertilized, e.g. with bone meal. */
    FERTILIZE({ it.fertilize }),

    /** Leaves decay. */
    LEAF_DECAY({ it.leafDecay }),

    /** A player tramples farmland. */
    TRAMPLE({ it.trample }),

    /** An entity changes a block, e.g. an enderman, a falling block or a mob trampling farmland. */
    ENTITY_BLOCK_CHANGES({ it.entityBlockChanges }),

    /** An item frame or painting is placed, broken, damaged or used. */
    HANGING_ENTITIES({ it.hangingEntities }),

    /** An armor stand is placed, damaged, used or has its equipment changed. */
    ARMOR_STANDS({ it.armorStands }),

    /** A player opens the sign editor, changes the text of a sign or dyes, glows or waxes it. */
    SIGNS({ it.signs }),

    /** A dispenser or dropper dispenses an item. */
    DISPENSERS({ it.dispensers }),

    /** A player changes a decoration block by right-clicking it or takes a book from a lectern. */
    DECO_BLOCKS({ it.decoBlocks }),

    /** A player opens or closes a door, trapdoor or fence gate. */
    DOORS({ it.doors }),

    /** A player presses a button. */
    BUTTONS({ it.buttons }),

    /** A player flips a lever. */
    LEVERS({ it.levers }),

    /** A player or another entity triggers a pressure plate by stepping on it. */
    PRESSURE_PLATES({ it.pressurePlates }),

    /** A player changes a note block, repeater, comparator or daylight detector, triggers a tripwire or lights redstone ore by touching it. */
    REDSTONE_COMPONENTS({ it.redstoneComponents }),

    /** A player uses a jukebox, bell, campfire, cauldron, beehive, bee nest, respawn anchor or end portal frame. */
    UTILITY_BLOCKS({ it.utilityBlocks }),

    /** A player picks sweet berries or glow berries, carves a pumpkin, tilts a big dripleaf or tramples turtle eggs. */
    PLANT_HARVEST({ it.plantHarvest }),

    /** A player places a boat, minecart or end crystal. */
    VEHICLES({ it.vehicles }),

    /** A player opens a container such as a chest, barrel, shulker box, furnace, hopper or brewing stand. */
    CONTAINERS({ it.containers }),
}
