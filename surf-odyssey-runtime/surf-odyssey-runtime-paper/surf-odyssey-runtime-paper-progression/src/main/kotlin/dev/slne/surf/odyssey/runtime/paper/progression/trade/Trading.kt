package dev.slne.surf.odyssey.runtime.paper.progression.trade

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playNoSound
import dev.slne.surf.odyssey.runtime.paper.common.menu.utils.view.playYesSound
import dev.slne.surf.odyssey.runtime.paper.progression.Progression
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMessages
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionMode
import dev.slne.surf.odyssey.runtime.paper.progression.ProgressionModule
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.ProgressionView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeOffer
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationOutcome
import dev.slne.surf.odyssey.runtime.paper.progression.operation.OperationPlan
import dev.slne.surf.odyssey.runtime.paper.progression.station.CraftPlanner
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** What a player can do with a trade offer right now. */
enum class TradeOfferState {
    /** The offer sells a recipe the player already knows; it cannot be bought. */
    KNOWN,

    /** The offer is priced in a currency, which cannot be paid yet; it cannot be bought. */
    CURRENCY_UNAVAILABLE,

    /** The player carries every item of the price. */
    AFFORDABLE,

    /** The player lacks items of the price. */
    TOO_EXPENSIVE;

    /** Decides the state of an offer from the player's recipes and carried items. */
    companion object {
        /**
         * Returns the state of [offer] for a player.
         *
         * A recipe the player knows is [KNOWN] whatever its price; otherwise a currency price is
         * [CURRENCY_UNAVAILABLE]. Prices naming the same item are summed before they are compared with the counts.
         *
         * @param offer the offer to judge
         * @param view the player's progression, which tells the recipes the player knows
         * @param counts the carried amount of each item by [CraftPlanner.countKey]; missing keys count as zero
         */
        fun of(offer: TradeOffer, view: ProgressionView, counts: Map<String, Int>): TradeOfferState {
            val result = offer.result
            if (result is TradeResult.Recipe &&
                (result.recipe.unlockedByDefault || view.isRecipeUnlocked(result.recipe.id))
            ) {
                return KNOWN
            }
            return when (val price = offer.price) {
                is Price.Currency -> CURRENCY_UNAVAILABLE
                is Price.Items -> {
                    val enough = needed(price).all { (key, amount) -> (counts[key] ?: 0) >= amount }
                    if (enough) AFFORDABLE else TOO_EXPENSIVE
                }
            }
        }

        /** Returns the amount [price] consumes of each item by [CraftPlanner.countKey]. */
        internal fun needed(price: Price.Items): Map<String, Int> =
            price.ingredients.groupingBy { CraftPlanner.countKey(it.item) }.fold(0) { sum, i -> sum + i.amount }
    }
}

/** Buys trade offers for players through item operations. */
object Trading {
    /** The delivery source of purchases. */
    private const val SOURCE = "trade"

    /**
     * Plans buying [offer]: the item price is taken, and the item is given or the recipe unlocked.
     *
     * @return the plan, or `null` for an offer with a currency price
     */
    fun planFor(offer: TradeOffer): OperationPlan? {
        val price = offer.price as? Price.Items ?: return null
        val reward = when (val result = offer.result) {
            is TradeResult.Item -> ItemReward(result.item, result.amount)
            is TradeResult.Recipe -> RecipeReward(result.recipe)
        }
        return OperationPlan(
            take = price.ingredients.map { Ingredient(it.item, it.amount) },
            changes = emptyList(),
            rewards = listOf(reward),
            source = SOURCE,
        )
    }

    /** Returns the name of what [offer] sells: the item name, or `Rezept: <Name>` for a recipe. */
    fun nameOf(offer: TradeOffer): Component = when (val result = offer.result) {
        is TradeResult.Item -> result.item.displayName
        is TradeResult.Recipe -> Component.text("Rezept: ").append(result.recipe.result.displayName)
    }

    /**
     * Buys [offer] for [player] unless the offer cannot be bought: a recipe the player knows, a currency price or an
     * unaffordable price. Nothing happens outside of [ProgressionMode.FULL]. Must be called on the player's thread.
     * [afterwards] runs on the player's thread once the operation ended, if the player is still online.
     *
     * @param player the buying player
     * @param offer the offer to buy
     * @param afterwards runs after the operation, for example to render the menu again
     */
    fun buy(player: Player, offer: TradeOffer, afterwards: (Player) -> Unit = {}) {
        val runtime = Progression.runtimeOrNull ?: return
        if (runtime.mode != ProgressionMode.FULL) return
        val progression = runtime.store[player.uniqueId]
        if (progression == null) {
            ProgressionMessages.unavailable(player)
            return
        }
        val plan = planFor(offer)
        if (plan == null || TradeOfferState.of(offer, progression, emptyMap()) == TradeOfferState.KNOWN) {
            player.playNoSound()
            return
        }
        val plugin = ProgressionModule.plugin
        val operations = runtime.operations
        plugin.launch {
            val outcome = operations.run(player, plan)
            withContext(plugin.entityDispatcher(player)) {
                if (!player.isOnline) return@withContext
                operations.report(player, outcome)
                when (outcome) {
                    OperationOutcome.Success -> {
                        player.playYesSound()
                        when (val result = offer.result) {
                            is TradeResult.Item -> ProgressionMessages.bought(player, result.item.displayName)
                            is TradeResult.Recipe ->
                                ProgressionMessages.recipeLearned(player, result.recipe.result.displayName)
                        }
                    }
                    OperationOutcome.Busy -> return@withContext
                    else -> player.playNoSound()
                }
                afterwards(player)
            }
        }
    }
}
