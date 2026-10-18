@file:Suppress("UnstableApiUsage")

package dev.slne.surf.odyssey.runtime.paper.progression.tool

import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.AccessoryDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolBonus
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack

/**
 * The bonuses a tool has at its current level together with its accessories.
 *
 * @property attributes the attribute modifiers, level bonuses first in level order, then accessory bonuses
 * @property extraMaxDamage the durability added to the base durability of the item
 * @property biteSpeedPercent how many percent shorter the waiting time of a fishing hook is, between 0 and 100
 * @property doubleDropChance the chance to double region loot, between 0 and 1
 */
data class ResolvedBonuses(
    val attributes: List<ToolBonus.AttributeBonus>,
    val extraMaxDamage: Int,
    val biteSpeedPercent: Int,
    val doubleDropChance: Double,
)

/** Resolves the bonuses of tools and applies them to tool items. */
object ToolBonuses {
    /** The namespace of the attribute modifier keys the tool bonuses use. */
    private const val KEY_NAMESPACE = "odyssey"

    /** The prefix of the attribute modifier keys the tool bonuses use; the index of the bonus follows it. */
    private const val KEY_PREFIX = "tool_bonus_"

    /**
     * Adds up the bonuses of [tool] of every level up to [level] and the bonuses of [accessories]. The bite speed is
     * capped at 100 percent and the double drop chance at 1.
     *
     * @param tool the tool definition
     * @param level the current level of the tool
     * @param accessories the attached accessories
     */
    fun resolve(tool: ToolDefinition, level: Int, accessories: List<AccessoryDefinition>): ResolvedBonuses {
        val levelBonuses = tool.bonuses.entries.filter { it.key <= level }.sortedBy { it.key }.flatMap { it.value }
        val all = levelBonuses + accessories.flatMap { it.bonuses }
        return ResolvedBonuses(
            attributes = all.filterIsInstance<ToolBonus.AttributeBonus>(),
            extraMaxDamage = all.filterIsInstance<ToolBonus.MaxDamage>().sumOf { it.extra },
            biteSpeedPercent = all.filterIsInstance<ToolBonus.BiteSpeed>().sumOf { it.percent }.coerceIn(0, 100),
            doubleDropChance = all.filterIsInstance<ToolBonus.DoubleDrop>().sumOf { it.chance }.coerceIn(0.0, 1.0),
        )
    }

    /**
     * Returns the bonuses of the tool [item] from its definition, its level and its attached accessories, or `null`
     * if [item] is not a tool of the installed content.
     *
     * @param item the item to inspect, may be `null`
     */
    fun of(item: ItemStack?): ResolvedBonuses? {
        val state = ToolItems.read(item) ?: return null
        val tool = ToolItems.definitionOf(state) ?: return null
        return resolve(tool, ToolProgress.levelFor(tool, state.usage), ToolItems.accessoriesOf(state))
    }

    /**
     * Returns how many percent shorter the waiting time of a fishing hook cast with [item] is, or 0 if [item] is not
     * a tool.
     *
     * @param item the item to inspect, may be `null`
     */
    fun biteSpeedPercent(item: ItemStack?): Int = of(item)?.biteSpeedPercent ?: 0

    /**
     * Returns the chance between 0 and 1 that region loot gained with [item] is doubled, or 0 if [item] is not a tool.
     *
     * @param item the item to inspect, may be `null`
     */
    fun doubleDropChance(item: ItemStack?): Double = of(item)?.doubleDropChance ?: 0.0

    /**
     * Writes [bonuses] onto [item]: the attribute modifiers of the bonuses replace every earlier tool bonus modifier
     * and keep all other modifiers of the item, and the maximum damage becomes the default durability of the item
     * type plus the extra durability, kept above the current damage. Items without durability keep their maximum
     * damage.
     *
     * @param item the tool item, changed in place
     * @param bonuses the bonuses to apply
     */
    fun apply(item: ItemStack, bonuses: ResolvedBonuses) {
        val current = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)?.modifiers().orEmpty()
        val builder = ItemAttributeModifiers.itemAttributes()
        current.filterNot { isToolBonusKey(it.modifier().key) }.forEach { entry ->
            builder.addModifier(entry.attribute(), entry.modifier(), entry.group, entry.display())
        }
        bonuses.attributes.forEachIndexed { index, bonus ->
            val key = NamespacedKey(KEY_NAMESPACE, "$KEY_PREFIX$index")
            val modifier = AttributeModifier(key, bonus.amount, bonus.operation, EquipmentSlotGroup.MAINHAND)
            builder.addModifier(bonus.attribute, modifier, EquipmentSlotGroup.MAINHAND)
        }
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build())

        val baseMaxDamage = item.type.asItemType()?.getDefaultData(DataComponentTypes.MAX_DAMAGE)
        if (baseMaxDamage != null && baseMaxDamage > 0) {
            val damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
            item.setData(DataComponentTypes.MAX_DAMAGE, maxDamageFor(baseMaxDamage, bonuses.extraMaxDamage, damage))
        }
    }

    /**
     * Returns the maximum damage of a tool item: [base] plus [extra], but at least one more than [damage], so that the
     * item never breaks because its maximum damage was set.
     *
     * @param base the default maximum damage of the item type
     * @param extra the extra durability of the bonuses
     * @param damage the current damage of the item
     */
    fun maxDamageFor(base: Int, extra: Int, damage: Int): Int = maxOf(base + extra, damage + 1)

    /** Returns whether [key] is the key of a tool bonus attribute modifier. */
    private fun isToolBonusKey(key: NamespacedKey): Boolean =
        key.namespace == KEY_NAMESPACE && key.key.startsWith(KEY_PREFIX)
}
