package dev.slne.surf.odyssey.runtime.paper.progression.definition.tool

import dev.slne.surf.odyssey.runtime.paper.progression.definition.Definition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.ProgressionDsl
import dev.slne.surf.odyssey.runtime.paper.progression.definition.curve.Curve
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemDefinition
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier

/** Kind of progression tool. */
enum class ToolType {
    /** Levels by mining region blocks. */
    PICKAXE,

    /** Levels by chopping region blocks. */
    AXE,

    /** Levels by catching fish in fishing regions. */
    ROD,

    /** Levels by killing mobs of mob regions in melee. */
    SWORD,

    /** Levels by killing mobs of mob regions at range. */
    BOW,
}

/** An effect a tool or accessory grants. */
sealed interface ToolBonus {
    /**
     * Adds an attribute modifier to the tool.
     *
     * @property attribute the modified attribute
     * @property amount the modifier amount
     * @property operation how the amount is applied
     */
    data class AttributeBonus(
        val attribute: Attribute,
        val amount: Double,
        val operation: AttributeModifier.Operation,
    ) : ToolBonus

    /**
     * Raises the durability of the tool.
     *
     * @property extra additional maximum damage
     */
    data class MaxDamage(val extra: Int) : ToolBonus

    /**
     * Shortens the time until a fish bites.
     *
     * @property percent reduction of the waiting time in percent
     */
    data class BiteSpeed(val percent: Int) : ToolBonus

    /**
     * Gives a chance to double region loot.
     *
     * @property chance probability between 0 and 1
     */
    data class DoubleDrop(val chance: Double) : ToolBonus
}

/**
 * A slot a tool opens for accessories.
 *
 * @property id slot ID
 * @property name name shown for the slot
 */
data class AccessorySlot(val id: String, val name: Component)

/**
 * A progression tool that levels with use.
 *
 * @property id stable tool ID
 * @property type kind of tool
 * @property name name shown on the tool
 * @property material base material of the tool
 * @property craftEngineId CraftEngine item that provides the model, or `null` for the plain material
 * @property curve usage needed per level
 * @property maxLevel highest level of the tool
 * @property bonuses bonuses gained at each level
 * @property slots accessory slots opened at each level
 */
class ToolDefinition(
    override val id: String,
    val type: ToolType,
    val name: Component,
    val material: Material,
    val craftEngineId: Key?,
    val curve: Curve,
    val maxLevel: Int,
    val bonuses: Map<Int, List<ToolBonus>>,
    val slots: Map<Int, AccessorySlot>,
) : Definition {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "ToolDefinition($id)"
}

/**
 * An accessory that fits into a tool slot.
 *
 * @property id stable accessory ID
 * @property item the item that represents the accessory
 * @property slot the slot the accessory fits into
 * @property bonuses bonuses granted while the accessory is attached
 */
class AccessoryDefinition(
    override val id: String,
    val item: ItemDefinition,
    val slot: AccessorySlot,
    val bonuses: List<ToolBonus>,
) : Definition {
    /** Returns [id] for log and validation messages. */
    override fun toString(): String = "AccessoryDefinition($id)"
}

/**
 * Builds the values of a [ToolDefinition].
 *
 * @property id the tool ID
 * @property type the tool type
 */
@ProgressionDsl
class ToolBuilder(private val id: String, private val type: ToolType) {
    /** Name shown on the tool; required. */
    var name: Component? = null

    /** Base material; required. */
    var material: Material? = null

    /** CraftEngine item that provides the model, or `null`. */
    var craftEngineId: Key? = null

    /** Usage needed per level; required. */
    var curve: Curve? = null

    /** Highest level of the tool. */
    var maxLevel: Int = 10

    /** Bonuses added so far by level. */
    private val bonuses = linkedMapOf<Int, MutableList<ToolBonus>>()

    /** Slots added so far by level. */
    private val slots = linkedMapOf<Int, AccessorySlot>()

    /**
     * Adds bonuses gained at [level].
     *
     * @param level the level
     * @param bonuses the bonuses
     */
    fun bonus(level: Int, vararg bonuses: ToolBonus) {
        this.bonuses.getOrPut(level) { mutableListOf() } += bonuses
    }

    /**
     * Opens an accessory slot at [level].
     *
     * @param level the level
     * @param slot the slot
     */
    fun slot(level: Int, slot: AccessorySlot) {
        slots[level] = slot
    }

    /**
     * Creates the definition.
     *
     * @throws IllegalStateException if [name], [material] or [curve] is missing
     */
    fun build(): ToolDefinition = ToolDefinition(
        id = id,
        type = type,
        name = checkNotNull(name) { "tool $id needs a name" },
        material = checkNotNull(material) { "tool $id needs a material" },
        craftEngineId = craftEngineId,
        curve = checkNotNull(curve) { "tool $id needs a curve" },
        maxLevel = maxLevel,
        bonuses = bonuses.mapValues { it.value.toList() },
        slots = slots.toMap(),
    )
}

/**
 * Builds the values of an [AccessoryDefinition].
 *
 * @property id the accessory ID
 * @property item the item that represents the accessory
 * @property slot the slot the accessory fits into
 */
@ProgressionDsl
class AccessoryBuilder(private val id: String, private val item: ItemDefinition, private val slot: AccessorySlot) {
    /** Bonuses added so far. */
    private val bonuses = mutableListOf<ToolBonus>()

    /**
     * Adds bonuses granted while the accessory is attached.
     *
     * @param bonuses the bonuses
     */
    fun bonus(vararg bonuses: ToolBonus) {
        this.bonuses += bonuses
    }

    /** Creates the definition. */
    fun build(): AccessoryDefinition = AccessoryDefinition(id, item, slot, bonuses.toList())
}

/**
 * Defines a progression tool.
 *
 * @param id stable tool ID
 * @param type kind of tool
 * @param block sets name, material, model, curve, bonuses and slots
 * @return the tool definition
 */
fun tool(id: String, type: ToolType, block: ToolBuilder.() -> Unit): ToolDefinition =
    ToolBuilder(id, type).apply(block).build()

/**
 * Defines an accessory.
 *
 * @param id stable accessory ID
 * @param item the item that represents the accessory
 * @param slot the slot the accessory fits into
 * @param block adds the bonuses of the accessory
 * @return the accessory definition
 */
fun accessory(
    id: String,
    item: ItemDefinition,
    slot: AccessorySlot,
    block: AccessoryBuilder.() -> Unit = {},
): AccessoryDefinition = AccessoryBuilder(id, item, slot).apply(block).build()
