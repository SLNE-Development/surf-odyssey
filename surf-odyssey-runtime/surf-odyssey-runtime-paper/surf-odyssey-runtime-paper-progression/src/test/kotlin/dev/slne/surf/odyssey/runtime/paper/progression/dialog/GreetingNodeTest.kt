package dev.slne.surf.odyssey.runtime.paper.progression.dialog

import dev.slne.surf.odyssey.runtime.paper.progression.definition.TestPacks
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.Ingredient
import dev.slne.surf.odyssey.runtime.paper.progression.definition.item.MaterialRef
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.NpcDefinition
import dev.slne.surf.odyssey.runtime.paper.progression.definition.npc.npc
import dev.slne.surf.odyssey.runtime.paper.progression.definition.station.station
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.Price
import dev.slne.surf.odyssey.runtime.paper.progression.definition.trade.TradeResult
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for the answers of the greeting node that opens every conversation. */
class GreetingNodeTest {
    private val offered = listOf(TestPacks.quest("lost_net"), TestPacks.quest("first_catch"))
    private val active = listOf(TestPacks.quest("harbor_intro"))

    /** Builds an NPC with or without trades and station. */
    private fun npc(trades: Boolean, station: Boolean): NpcDefinition = npc("olek") {
        name = text("Olek")
        skin("texture", "signature")
        portrait("neutral", Key.key("odyssey", "portrait_olek_neutral"))
        greeting = text("Moin!")
        if (trades) {
            trade(
                "bait",
                TradeResult.Item(MaterialRef(Material.COD), 1),
                Price.Items(listOf(Ingredient(MaterialRef(Material.WHEAT), 4))),
            )
        }
        if (station) {
            this.station = station("smokehouse") {
                displayName = text("Räucherei")
                icon = Material.SMOKER
            }
        }
    }

    @Test
    fun `answers list offers, active quests, trade, craft and leave in that order`() {
        val answers = GreetingNode.answers(npc(trades = true, station = true), offered, active)

        assertEquals(
            listOf(
                GreetingAnswer.Offer(offered[0]),
                GreetingAnswer.Offer(offered[1]),
                GreetingAnswer.Continue(active[0]),
                GreetingAnswer.Trade,
                GreetingAnswer.Craft,
                GreetingAnswer.Leave,
            ),
            answers,
        )
    }

    @Test
    fun `trade is missing without trades and craft without station`() {
        val answers = GreetingNode.answers(npc(trades = false, station = false), offered, active)

        assertEquals(
            listOf(
                GreetingAnswer.Offer(offered[0]),
                GreetingAnswer.Offer(offered[1]),
                GreetingAnswer.Continue(active[0]),
                GreetingAnswer.Leave,
            ),
            answers,
        )
    }

    @Test
    fun `a trader without quests offers trade and leave`() {
        val answers = GreetingNode.answers(npc(trades = true, station = false), emptyList(), emptyList())

        assertEquals(listOf(GreetingAnswer.Trade, GreetingAnswer.Leave), answers)
    }
}
