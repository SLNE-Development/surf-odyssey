package dev.slne.surf.odyssey.runtime.paper.progression.trade

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.CustomItemRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.ref
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.ProgressionView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.requirement.QuestStateView
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.ItemReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.reward.RecipeReward
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeOffer
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for the state and the operation of trade offers. */
class TradeOfferStateTest {
    private val sushi = TestPacks.sushiRecipe(rice = 1, fish = 1)
    private val fish = CustomItemRef(TestPacks.item("fish", Material.COD))

    /** Returns a progression view that knows exactly the recipes [known]. */
    private fun viewKnowing(vararg known: String): ProgressionView = object : ProgressionView {
        /** Always null. */
        override fun questState(questId: String): QuestStateView? = null

        /** Returns whether [recipeId] is in [known]. */
        override fun isRecipeUnlocked(recipeId: String): Boolean = recipeId in known

        /** Always false. */
        override fun hasTitle(titleId: String): Boolean = false
    }

    /** Returns a recipe offer for [sushi] paid with [price]. */
    private fun recipeOffer(price: Price) = TradeOffer("sushi_recipe", TradeResult.Recipe(sushi), price)

    /** Returns an item offer paid with 5 fish. */
    private fun itemOffer() =
        TradeOffer("bread", TradeResult.Item(Material.BREAD.ref, 3), Price.Items(listOf(Ingredient(fish, 5))))

    @Test
    fun `an unlocked recipe is known and cannot be bought`() {
        val offer = recipeOffer(Price.Items(listOf(Ingredient(fish, 1))))
        assertEquals(TradeOfferState.KNOWN, TradeOfferState.of(offer, viewKnowing("sushi"), mapOf("fish" to 9)))
    }

    @Test
    fun `a currency price is unavailable`() {
        val offer = recipeOffer(Price.Currency("coins", 10.0))
        assertEquals(TradeOfferState.CURRENCY_UNAVAILABLE, TradeOfferState.of(offer, viewKnowing(), emptyMap()))
    }

    @Test
    fun `enough items make an offer affordable`() =
        assertEquals(TradeOfferState.AFFORDABLE, TradeOfferState.of(itemOffer(), viewKnowing(), mapOf("fish" to 5)))

    @Test
    fun `missing items make an offer too expensive`() =
        assertEquals(TradeOfferState.TOO_EXPENSIVE, TradeOfferState.of(itemOffer(), viewKnowing(), mapOf("fish" to 4)))

    @Test
    fun `prices naming the same item are summed`() {
        val offer = TradeOffer(
            "bread", TradeResult.Item(Material.BREAD.ref, 1),
            Price.Items(listOf(Ingredient(fish, 3), Ingredient(fish, 3)))
        )
        assertEquals(TradeOfferState.TOO_EXPENSIVE, TradeOfferState.of(offer, viewKnowing(), mapOf("fish" to 5)))
    }

    @Test
    fun `an item purchase takes the price and gives the item`() {
        val plan = Trading.planFor(itemOffer())!!
        assertEquals(listOf(Ingredient(fish, 5)), plan.take)
        assertEquals(listOf(ItemReward(Material.BREAD.ref, 3)), plan.rewards)
        assertEquals(emptyList(), plan.changes)
        assertEquals("trade", plan.source)
    }

    @Test
    fun `a recipe purchase takes the price and unlocks the recipe`() {
        val plan = Trading.planFor(recipeOffer(Price.Items(listOf(Ingredient(fish, 1)))))!!
        assertEquals(listOf(RecipeReward(sushi)), plan.rewards)
    }

    @Test
    fun `a currency offer has no plan`() =
        assertEquals(null, Trading.planFor(recipeOffer(Price.Currency("coins", 1.0))))
}
