package dev.slne.surf.odyssey.runtime.paper.progression.operation

import dev.slne.surf.odyssey.core.common.progression.StateChange
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.CurrencyReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.QuestReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.TitleReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ToolReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.tool.ToolDefinition
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.logging.Logger

/** Creates the item of a progression tool that a reward gives. */
fun interface ToolRewardFactory {
    /**
     * Creates a new item of [tool].
     *
     * @param tool the rewarded tool
     * @return the tool item
     */
    fun create(tool: ToolDefinition): ItemStack
}

/** Pays currency amounts to players. */
interface CurrencyGateway {
    /**
     * Pays [amount] of the currency [currencyId] to [player].
     *
     * @param player the paid player
     * @param currencyId the ID of the currency
     * @param amount the paid amount
     */
    fun grant(player: UUID, currencyId: String, amount: Double)
}

/**
 * A [CurrencyGateway] that pays nothing and only logs each payment.
 *
 * @param logger the log the payments are written to
 */
class LoggingCurrencyGateway(private val logger: Logger) : CurrencyGateway {
    /** Logs the player, the currency and the amount. */
    override fun grant(player: UUID, currencyId: String, amount: Double) {
        logger.info("Currency reward for $player: $amount $currencyId")
    }
}

/** Turns quest rewards into the state changes and item stacks of an operation. */
object RewardPlanner {
    /** Creates the items of tool rewards; while it is `null`, planning a tool reward fails. */
    @Volatile
    var toolRewards: ToolRewardFactory? = null

    /**
     * Plans [rewards]: recipe and title rewards become unlock changes, item and tool rewards become item stacks.
     * Currency rewards produce neither; [currencies] returns them for payment after the operation is committed.
     *
     * @param rewards the rewards to plan
     * @param stacks creates the stacks of an item reward from the item and the total amount
     * @return the state changes and the item stacks, each in the order of [rewards]
     * @throws IllegalStateException if a tool reward is planned while [toolRewards] is `null`
     * @throws IllegalArgumentException if a reward is of an unknown type
     */
    fun plan(
        rewards: List<QuestReward>,
        stacks: (ItemRef, Int) -> List<ItemStack> = ::stacksOf,
    ): Pair<List<StateChange>, List<ItemStack>> {
        val changes = mutableListOf<StateChange>()
        val items = mutableListOf<ItemStack>()
        for (reward in rewards) {
            when (reward) {
                is RecipeReward -> changes += StateChange.RecipeUnlocked(reward.recipe.id)
                is TitleReward -> changes += StateChange.TitleUnlocked(reward.title.id)
                is ItemReward -> items += stacks(reward.item, reward.amount)
                is ToolReward -> items += checkNotNull(toolRewards) { "tool rewards are not installed" }
                    .create(reward.tool)
                is CurrencyReward -> Unit
                else -> throw IllegalArgumentException("unknown reward type ${reward.typeKey}")
            }
        }
        return changes to items
    }

    /**
     * Returns the currency rewards among [rewards].
     *
     * @param rewards the rewards of an operation
     */
    fun currencies(rewards: List<QuestReward>): List<CurrencyReward> = rewards.filterIsInstance<CurrencyReward>()

    /**
     * Creates [amount] of [ref] as stacks of at most the item's maximum stack size.
     *
     * @param ref the item to create
     * @param amount the total amount
     * @return the stacks, empty when [amount] is not positive
     */
    fun stacksOf(ref: ItemRef, amount: Int): List<ItemStack> {
        if (amount <= 0) return emptyList()
        val sample = ref.create(1)
        val max = sample.maxStackSize.coerceAtLeast(1)
        return (0 until amount step max).map { start -> sample.asQuantity(minOf(max, amount - start)) }
    }
}
